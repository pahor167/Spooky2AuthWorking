package com.spooky2.huntkill.ui.hunt

import com.spooky2.huntkill.core.lookup.LookupMatch
import com.spooky2.huntkill.core.lookup.ReverseLookup
import com.spooky2.huntkill.core.lookup.ReverseLookupParameters
import com.spooky2.huntkill.core.model.DropoutSegment
import com.spooky2.huntkill.core.model.ScanOutcome
import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.model.ScanProgress
import com.spooky2.huntkill.core.model.ScanResult
import com.spooky2.huntkill.core.scan.KillControl
import com.spooky2.huntkill.core.scan.PauseGate
import com.spooky2.huntkill.core.scan.RefinementPlanner
import com.spooky2.huntkill.service.RunStatus
import com.spooky2.huntkill.service.ScanRunNotifier
import com.spooky2.huntkill.data.FrequencyDatabaseSource
import com.spooky2.huntkill.data.GeneratorSession
import com.spooky2.huntkill.data.RunHistoryRepository
import com.spooky2.huntkill.data.RunHit
import com.spooky2.huntkill.data.RunRecord
import com.spooky2.huntkill.log.LogBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Owns the per-generator Hunt→Kill run logic extracted verbatim from [HuntViewModel].
 *
 * One controller drives one generator's run lifecycle: sweep, dropout handling, kill,
 * refinement, reverse lookup, pause/jump/cancel, and the foreground-service notifier.
 * It owns its OWN [_state]/[_events] (each connected generator has an independent run
 * snapshot the coordinator switches between by tab) and launches its coroutines on the
 * injected [scope].
 *
 * Session resolution mirrors [HuntViewModel] exactly:
 *  - [liveSession] returns the currently-connected session (SessionHolder.current()),
 *    used by every entry method's null/error guard and by onProgress' engine reads.
 *  - [acquireSession] is the per-run reconnect seam (SessionHolder.acquireForHunt()):
 *    the live USB path reuses the open session; replay tests rebuild a fresh
 *    single-use FakeTransport session each run.
 */
class GeneratorRunController(
    index: Int,
    private val scope: CoroutineScope,
    private val log: LogBus,
    private val liveSession: () -> GeneratorSession?,
    private val acquireSession: suspend () -> GeneratorSession?,
    private val frequencyDatabase: FrequencyDatabaseSource? = null,
    private val runHistory: RunHistoryRepository? = null,
    private val runNotifier: ScanRunNotifier? = null,
    private val onAcquireHuntSlot: suspend () -> Unit = {},
) {

    /** Position in the coordinator's controller list; updated on a list rebuild via [rebindIndex]. */
    var index: Int = index
        private set

    /**
     * The registry (USB port) key this controller is bound to, or null for the single-session
     * default placeholder. Used by the coordinator to reuse a running controller when the
     * session map changes rather than recreating it.
     */
    var portKey: Int? = null
        private set

    /** Bind this controller to a registry port key (coordinator only). */
    fun bindPortKey(key: Int) { portKey = key }

    /** Update this controller's tab position after a list rebuild (coordinator only). */
    fun rebindIndex(newIndex: Int) { index = newIndex }

    /**
     * This controller's own run snapshot — the single source of truth for ONE generator.
     * The coordinator exposes the active controller's [state] to the UI and flips between
     * controllers on tab switch. Seeded with the generator info if a session is already
     * connected at construction.
     */
    private val _state = MutableStateFlow(HuntUiState())
    val state: StateFlow<HuntUiState> = _state.asStateFlow()

    /** One-shot UI messages (snackbars) for THIS controller, e.g. "Generator zeroed". */
    private val _events = Channel<String>(Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    init {
        // Seed the connection chip from the session bound at construction (if any), so a
        // pre-connected generator shows its info immediately without waiting for a refresh.
        refreshGeneratorInfo()
    }

    private var huntJob: Job? = null

    /**
     * Bumped every time a run starts or is cancelled. cancel()'s async safety-stop
     * captures the epoch and applies the terminal Cancelled state ONLY if the epoch is
     * unchanged — so a fast Cancel→Start can't have the stale cancel coroutine stamp
     * Cancelled over the freshly-started run's Hunting state.
     */
    private var runEpoch: Int = 0

    /** Reverse-lookup coroutine; cancelled/replaced when the tolerance is re-selected. */
    private var lookupJob: Job? = null

    /** Parameters of the in-flight/just-finished hunt; reused by the re-scan flow. */
    private var activeParameters: ScanParameters? = null

    /**
     * True while the current Kill is treating frequencies loaded from a saved run
     * (Re-run from History) rather than a fresh hunt. Re-runs are NOT persisted again —
     * they treat already-saved frequencies, so saving would just duplicate the record.
     */
    private var fromReRun: Boolean = false

    /** Guards against double-persisting the same hunt (publishSweepOutcome can re-run). */
    private var savedThisRun: Boolean = false

    /** Last sweep outcome (readings + validity + segments); spliced by a re-scan. */
    private var lastOutcome: ScanOutcome? = null

    /**
     * Incrementally-grown per-step reading history for the LIVE scrollable graph.
     * Indexed by sweep-step (0-based), so provisional marker step indices align to
     * it directly. Published as [HuntUiState.fullHistory] each step; replaced by the
     * authoritative [ScanOutcome.sweepReadings] once the sweep finishes.
     */
    private val liveHistory = ArrayList<Float>()

    /** Parallel to [liveHistory]: per-step display validity (false = read failure or a
     *  post-resume settle step). Lets the live graph exclude the pause/resume spike. */
    private val liveValid = ArrayList<Boolean>()

    /** Drives a 1s tick so [HuntUiState.elapsedSeconds] advances while running, not paused. */
    private var elapsedTicker: Job? = null

    /** Cooperative pause for the running scan; shared between this VM and the engine. */
    val pauseGate = PauseGate()

    /** Cooperative jump control for the kill phase; shared between this VM and the engine. */
    private val killControl = KillControl()

    /**
     * Live repeat flag the engine's `killHits` reads at each frequency-pass boundary.
     * Held separately from [HuntUiState.repeatKill] (which mirrors it for the UI) so a
     * mid-kill toggle is honored on the next pass without rebuilding the kill call. Default
     * true = repeat ON. Kept in sync by [toggleRepeatKill].
     */
    private val repeatKillFlag = MutableStateFlow(true)

    /**
     * Live refinement flag the run loop reads at each kill-pass boundary, mirroring
     * [HuntUiState.refineHits] — same live-toggle pattern as [repeatKillFlag]. While
     * true the kill does NOT repeat (one pass per generation); after each pass a
     * refinement generation re-scans around the hits at a halved step. Default ON,
     * matching the canonical GX Hunt and Kill preset (BFB_Continue_Refining_Hits=1).
     */
    private val refineFlag = MutableStateFlow(true)

    /**
     * True while a refinement generation's sweep is running. Gates [onProgress] so the
     * narrow-window readings do NOT append to the gen-1 live graph (different grid).
     */
    @Volatile
    private var refinementSweepActive = false

    /**
     * Flip kill-phase repeat on/off. Updates both the UI state and the live
     * [repeatKillFlag] the running engine reads, so a mid-kill toggle takes effect at the
     * next pass-end: turning repeat OFF lets the current pass finish and the run complete;
     * turning it ON keeps looping.
     */
    fun toggleRepeatKill() {
        val next = !repeatKillFlag.value
        repeatKillFlag.value = next
        _state.update { it.copy(repeatKill = next) }
        log.i(TAG, "Repeat kill ${if (next) "enabled" else "disabled"}")
    }

    /**
     * Switch the hit-list view between compact (frequency only) and details (deviation +
     * matches). Shared across the Hits and Kill screens; screens reset their per-row
     * "show all" expansion when this changes so every frequency renders uniformly.
     */
    fun setHitsCompactView(compact: Boolean) {
        _state.update { it.copy(hitsCompactView = compact) }
    }

    /** Toggle "review hits before treatment" (clean live sweep stops at the Hits review). */
    fun setReviewBeforeKill(review: Boolean) {
        _state.update { it.copy(reviewBeforeKill = review) }
    }

    /** Flip refinement mode. Honored live at the next kill-pass boundary. */
    fun toggleRefineHits() {
        val next = !refineFlag.value
        refineFlag.value = next
        _state.update { it.copy(refineHits = next) }
        log.i(TAG, "Refine hits ${if (next) "enabled" else "disabled"}")
    }

    fun updateStartFrequency(v: String) = updateParams { it.copy(startFrequencyText = v) }
    fun updateEndFrequency(v: String) = updateParams { it.copy(endFrequencyText = v) }
    fun updateDwellSeconds(v: String) = updateParams { it.copy(dwellSecondsText = v) }
    fun updateTargetAmplitude(v: String) = updateParams { it.copy(targetAmplitudeCvText = v) }

    private fun updateParams(transform: (HuntParamsUi) -> HuntParamsUi) {
        _state.update { it.copy(params = transform(it.params)) }
    }

    /** Launch the full Hunt→Kill flow off the main thread; collect progress into state. */
    fun startHunt() {
        if (huntJob?.isActive == true) return
        if (liveSession() == null) {
            log.e(TAG, "startHunt blocked: not connected")
            _state.update {
                it.copy(phase = HuntPhase.Error, errorMessage = "Not connected. Connect first.")
            }
            return
        }
        // New run: invalidate any in-flight cancel coroutine (see [runEpoch]).
        runEpoch++

        pauseGate.resume()
        liveHistory.clear()
        liveValid.clear()
        // A fresh hunt: its hits ARE persisted (not a History re-run), once.
        fromReRun = false
        savedThisRun = false
        _state.update {
            it.copy(
                phase = HuntPhase.Hunting,
                statusText = "Starting hunt...",
                angleHistory = emptyList(),
                fullHistory = FloatArray(0),
                historyValid = BooleanArray(0),
                graphMarkers = emptyList(),
                hits = emptyList(),
                killIndex = 0,
                killTotal = 0,
                killDwellRemainingSeconds = 0,
                isPaused = false,
                rescanInProgress = false,
                elapsedSeconds = 0,
                estimatedRemainingSeconds = 0,
                errorMessage = null,
                busyAction = "Starting…",
            )
        }
        startElapsedTicker()

        huntJob = scope.launch(Dispatchers.Default) {
            // The live USB path reuses the open session across hunts (no reconnector).
            // Test replay sessions set a reconnector so a fresh single-use FakeTransport
            // is rebuilt each run. Both go through SessionHolder.acquireForHunt().
            val session = runCatching { acquireSession() }.getOrElse { error ->
                log.e(TAG, "acquireForHunt failed: ${error.message}")
                _state.update {
                    it.copy(
                        phase = HuntPhase.Error,
                        errorMessage = error.message ?: "Reconnect failed",
                        busyAction = null,
                    )
                }
                return@launch
            }
            if (session == null) {
                log.e(TAG, "acquireForHunt returned no session")
                _state.update {
                    it.copy(
                        phase = HuntPhase.Error,
                        errorMessage = "Not connected. Connect first.",
                        busyAction = null,
                    )
                }
                return@launch
            }

            // Timing depends on the session kind: live hardware uses the original
            // Spooky2 settle delay + amplitude ramp; the demo replay runs fast.
            // continueRefining mirrors the UI switch (the run loop reads refineFlag live;
            // the param keeps the persisted/logged parameters honest).
            val parameters = _state.value.params
                .toScanParameters(isDemo = session.isDemo)
                .copy(continueRefining = refineFlag.value)
            _state.update { it.copy(refineGeneration = 1) }
            log.i(
                TAG,
                "startHunt(${if (session.isDemo) "demo" else "live"}): " +
                    "start=${parameters.startFrequency} end=${parameters.endFrequency} " +
                    "dwell=${parameters.dwellSeconds}s ampCv=${parameters.targetAmplitudeCv} " +
                    "readDelay=${parameters.minReadDelaySeconds}s ramp=${parameters.enableAmplitudeRampUp}",
            )

            activeParameters = parameters

            runCatching {
                // Mutual-exclusion hook (no-op for now); a later step makes this acquire
                // a shared hunt slot before the sweep enters live treatment.
                onAcquireHuntSlot()
                // Run the SWEEP only. The kill decision is made afterwards so a
                // dropout can be surfaced before any frequencies are treated.
                val outcome = session.engine.runBiofeedbackScanDetailed(
                    parameters,
                    { progress -> onProgress(progress, parameters) },
                    pauseGate,
                )
                lastOutcome = outcome
                publishSweepOutcome(outcome)

                if (outcome.segments.isNotEmpty()) {
                    // Dropouts detected: STOP before the kill, surface the warning.
                    surfaceDropouts(outcome, parameters)
                } else if (!session.isDemo && _state.value.reviewBeforeKill) {
                    // Live + "review before treatment" on: STOP at the Hits review so the
                    // user can inspect / graph-rescan candidates, then Start treatment.
                    surfaceHitsReady(outcome)
                } else {
                    // Default (and all demo replay): go straight to the kill.
                    proceedToKill(session, parameters, outcome.hits, cycle = 1)
                }
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                log.e(TAG, "Scan failed: ${error.message}")
                stopElapsedTicker()
                safetyStopInternal(session)
                _state.update {
                    it.copy(
                        phase = HuntPhase.Error,
                        errorMessage = error.message ?: "Scan failed",
                        isPaused = false,
                        busyAction = null,
                    )
                }
            }
        }
    }

    /** Store the full reading history + dropout diagnostics into UI state, and kick off
     *  reverse lookup immediately so matches are available as soon as hits are known. */
    private fun publishSweepOutcome(outcome: ScanOutcome) {
        val markers = finalMarkers(outcome.hits)
        // Overlay the display lead-in onto the validity mask for the FINAL graph too, so
        // the unsettled lead-in (which would re-introduce the leading vertical spike) stays
        // excluded after the sweep completes — matching the live view. This is a DISPLAY
        // copy; detection ran on outcome.sweepValid untouched, and lastOutcome (used by the
        // re-scan splice) still holds the original engine mask. Real dropouts are preserved.
        val displayValid = displayValidWithLeadIn(outcome.sweepValid)
        _state.update {
            it.copy(
                fullHistory = outcome.sweepReadings,
                historyValid = displayValid,
                dropoutSegments = outcome.segments,
                totalSweepSteps = outcome.sweepReadings.size,
                graphMarkers = markers,
                hits = outcome.hits,
            )
        }
        // Start reverse lookup as soon as hits are known (sweep complete), so the Kill
        // screen and dropout-warning screen can already show matches during treatment.
        if (outcome.hits.isNotEmpty()) {
            runReverseLookup(outcome.hits, _state.value.lookupTolerancePercent)
        }
        // Persist the run the MOMENT final hits are known — before the kill starts — so a
        // later cancel never loses the found frequencies. Re-runs and empty-hit sweeps are
        // not saved; the savedThisRun guard makes this idempotent across re-scan merges.
        persistRunIfNeeded(outcome.hits)
    }

    /**
     * Save the completed hunt's final frequencies as a [RunRecord]. No-op when there are
     * no hits, when the frequencies came from a History re-run, when no repository is
     * wired (JVM tests), or when this run was already saved.
     */
    private fun persistRunIfNeeded(hits: List<ScanResult>) {
        if (hits.isEmpty() || fromReRun || savedThisRun) return
        val repository = runHistory ?: return
        savedThisRun = true
        val params = activeParameters
        val generatorLabel = _state.value.generator?.activeLabel()
        val record = RunRecord(
            id = "",
            timestampMs = System.currentTimeMillis(),
            generatorLabel = generatorLabel,
            startFrequency = params?.startFrequency ?: 0.0,
            endFrequency = params?.endFrequency ?: 0.0,
            dwellSeconds = params?.dwellSeconds ?: 0.0,
            targetAmplitudeCv = params?.targetAmplitudeCv ?: 0,
            hits = hits.map { RunHit(frequency = it.frequency, deviation = it.deviation) },
        )
        scope.launch {
            runCatching { repository.save(record) }
                .onSuccess { saved -> log.i(TAG, "Saved run ${saved.id}: ${saved.hits.size} frequencies") }
                .onFailure { error -> log.e(TAG, "Failed to save run: ${error.message}") }
        }
    }

    /**
     * Map each detected hit to its sweep-step index so the graph can draw a FINAL
     * (solid red) marker aligned to [HuntUiState.fullHistory]. Uses the same step
     * frequencies the sweep produced; ties resolve to the first matching step.
     */
    private fun finalMarkers(hits: List<ScanResult>): List<GraphMarker> {
        if (hits.isEmpty()) return emptyList()
        val parameters = activeParameters ?: return emptyList()
        val frequencies = com.spooky2.huntkill.core.scan.ScanEngine.calculateFrequencySteps(parameters)
        // Build a freq -> first step-index map once (O(N)); hits is tiny.
        val freqToStep = HashMap<Double, Int>(frequencies.size)
        for (i in frequencies.indices) freqToStep.putIfAbsent(frequencies[i], i)
        return hits.mapNotNull { hit ->
            val step = freqToStep[hit.frequency] ?: return@mapNotNull null
            GraphMarker(step, hit.frequency, hit.deviation, isFinal = true)
        }
    }

    /**
     * Surface the [HuntPhase.HitsReadyWithDropouts] warning state and log the
     * detected segments with their frequency ranges. No kill happens until the
     * user chooses "Re-scan affected segments" or "Continue anyway".
     */
    private fun surfaceDropouts(outcome: ScanOutcome, parameters: ScanParameters) {
        stopElapsedTicker()
        val pct = (outcome.invalidFraction * 100)
        log.w(
            TAG,
            "Dropout detected: ${outcome.segments.size} segment(s), " +
                "~${"%.1f".format(pct)}% of sweep flagged",
        )
        outcome.segments.forEachIndexed { i, seg ->
            log.w(
                TAG,
                "  dropout[$i] steps ${seg.startStep}..${seg.endStep} " +
                    "(${"%.2f".format(seg.startFrequency)}–${"%.2f".format(seg.endFrequency)} Hz)",
            )
        }
        _state.update {
            it.copy(
                phase = HuntPhase.HitsReadyWithDropouts,
                hits = outcome.hits,
                statusText = "Connection dropped during ${outcome.segments.size} segment(s)",
                isPaused = false,
                busyAction = null,
            )
        }
    }

    /** Run the kill phase for [hits], then publish Done + reverse lookup. */
    private suspend fun proceedToKill(
        session: GeneratorSession,
        parameters: ScanParameters,
        hits: List<ScanResult>,
        cycle: Int,
    ) {
        var currentHits = hits
        var generation = cycle
        // Params of the generation that found [currentHits]; refinement halves its step.
        var huntParams = parameters

        while (currentHits.isNotEmpty()) {
            session.engine.killHits(
                currentHits,
                huntParams,
                { progress -> onProgress(progress, huntParams) },
                pauseGate,
                generation,
                killControl,
                // Read live so a mid-kill repeat toggle is honored at the next pass-end.
                // With repeat ON this call does not return until the user turns repeat off
                // (current pass finishes) or cancels — so HuntPhase.Done is reached then.
                // Refinement supersedes repeat: one pass per generation, then refine.
                repeatEnabled = { repeatKillFlag.value && !refineFlag.value },
            )

            // ── Refinement generations (original "Continue Refining Hits") ──
            // Checked at each pass boundary so a mid-kill toggle is honored live.
            if (!refineFlag.value) break
            kotlin.coroutines.coroutineContext.ensureActive()

            generation++
            // Per binary: window is sized to the halved step (±10 sweep-steps per hit)
            // and zooms in as the step halves each generation.
            val plan = RefinementPlanner.planNextGeneration(currentHits, huntParams)
            val freqs = RefinementPlanner.frequencyStepsFor(plan, huntParams)
            if (freqs.isEmpty()) {
                log.i(TAG, "Refinement gen $generation: no scannable window — stopping")
                break
            }
            val scanParams = huntParams.copy(
                stepSizeHz = plan.nextStepSizeHz,
                stepSizePercent = plan.nextStepSizePercent,
            )
            log.i(
                TAG,
                "Refinement gen $generation: ${plan.windows.size} window(s), " +
                    "${freqs.size} steps at ${"%.5f".format(plan.nextStepSizePercent)}%",
            )
            _state.update {
                it.copy(
                    refineGeneration = generation,
                    statusText = "Refining (cycle $generation) — scanning…",
                )
            }

            // Narrow-window sweep. The UI stays on the Kill screen (phase unchanged);
            // [refinementSweepActive] keeps these readings off the gen-1 live graph.
            // Dropout segments are not surfaced mid-refinement (the gen-1 dropout flow
            // already gated the run); a dropped refinement read just weakens that hit.
            refinementSweepActive = true
            // Mutual-exclusion hook (no-op for now): a later step re-acquires the hunt
            // slot before re-entering a refinement sweep.
            onAcquireHuntSlot()
            val outcome = try {
                session.engine.runBiofeedbackScanDetailed(
                    scanParams,
                    { progress -> onProgress(progress, scanParams) },
                    pauseGate,
                    freqs,
                )
            } finally {
                refinementSweepActive = false
            }

            currentHits = outcome.hits
            huntParams = scanParams
            if (currentHits.isEmpty()) {
                log.i(TAG, "Refinement gen $generation: no hits — refinement complete")
                break
            }
            log.i(TAG, "Refinement gen $generation: ${currentHits.size} refined hits")
            // Show the refined hits on the Kill screen (gen-1 graph/markers are kept —
            // refined frequencies don't map onto the full-range step grid).
            _state.update { it.copy(hits = currentHits) }
            runReverseLookup(currentHits, _state.value.lookupTolerancePercent)
        }
        session.engine.finishHuntAndKill(parameters)

        log.i(TAG, "Hunt complete — ${currentHits.size} hits (generation $generation)")
        currentHits.forEachIndexed { index, hit ->
            log.i(TAG, "  hit[$index] freq=${"%.2f".format(hit.frequency)} deviation=${hit.deviation}")
        }
        stopElapsedTicker()
        val finalHits = currentHits.ifEmpty { _state.value.hits.ifEmpty { hits } }
        _state.update {
            it.copy(
                phase = HuntPhase.Done,
                hits = finalHits,
                statusText = "Hunt & Kill complete — ${finalHits.size} hits",
                killDwellRemainingSeconds = 0,
                isPaused = false,
                busyAction = null,
            )
        }
        // Lookup was already started in publishSweepOutcome when hits were first found.
        // Only re-run here if the hit set changed (e.g. after a re-scan merge changed hits)
        // or if results are still absent for some reason (e.g. DB load failed earlier).
        val currentState = _state.value
        val hitsChanged = currentState.lookupResults.keys != finalHits.map { it.frequency }.toSet()
        if (currentState.lookupResults.isEmpty() || hitsChanged) {
            runReverseLookup(finalHits, currentState.lookupTolerancePercent)
        }
    }

    /**
     * "Continue anyway": skip the re-scan and kill using the hits computed with
     * the invalid steps already excluded. No-op outside the dropout-warning state.
     */
    /** Clean-sweep review state: hits ready, user decides when to treat. */
    private fun surfaceHitsReady(outcome: ScanOutcome) {
        stopElapsedTicker()
        log.i(TAG, "Sweep complete — ${outcome.hits.size} hits ready for review")
        _state.update {
            it.copy(
                phase = HuntPhase.HitsReady,
                hits = outcome.hits,
                statusText = "Sweep complete — ${outcome.hits.size} hits",
                isPaused = false,
                busyAction = null,
            )
        }
    }

    /** "Continue anyway" on the dropout review = start treatment ignoring the dropouts. */
    fun continueAnyway() = startTreatment()

    /**
     * Begin the kill phase on the reviewed hits. Valid from either review state
     * (HitsReady clean, or HitsReadyWithDropouts). Drives the Kill screen.
     */
    fun startTreatment() {
        val phase = _state.value.phase
        if (phase != HuntPhase.HitsReady && phase != HuntPhase.HitsReadyWithDropouts) return
        val session = liveSession() ?: return
        val parameters = activeParameters ?: return
        val hits = _state.value.hits

        log.i(TAG, "Start treatment — killing ${hits.size} reviewed hits")
        runEpoch++
        pauseGate.resume()
        _state.update { it.copy(phase = HuntPhase.Killing, busyAction = null, isPaused = false) }
        startElapsedTicker()
        huntJob = scope.launch(Dispatchers.Default) {
            runCatching { proceedToKill(session, parameters, hits, cycle = 1) }
                .onFailure { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    log.e(TAG, "Kill failed: ${error.message}")
                    stopElapsedTicker()
                    safetyStopInternal(session)
                    _state.update {
                        it.copy(phase = HuntPhase.Error, errorMessage = error.message ?: "Kill failed", busyAction = null)
                    }
                }
        }
    }

    /**
     * Re-run a treatment from a saved run's frequencies (History → "Re-run treatment").
     * Treats [freqs] again via the KILL phase only — no new hunt — on the currently
     * connected generator, driving the same Kill screen (progress, jump, reverse lookup,
     * zero-on-cancel) as a fresh hunt. The synthesized hits carry the saved [deviations]
     * (aligned by index, 0.0 when absent) and a zero reading; only their frequency is
     * treated. Requires a connected session — returns false (no state change) otherwise.
     *
     * Re-runs are intentionally NOT persisted again (they treat already-saved
     * frequencies), so [fromReRun] is set to suppress the save trigger.
     */
    fun startKillFromFrequencies(
        freqs: List<Double>,
        dwellSeconds: Double,
        amplitudeCv: Int,
        deviations: List<Double> = emptyList(),
    ): Boolean {
        if (freqs.isEmpty()) return false
        val session = liveSession() ?: run {
            log.e(TAG, "Re-run blocked: not connected")
            return false
        }
        // A hunt/kill may be running (or paused) on this session. Stop it and reuse the
        // session for the re-run — the caller confirms this with the user first. The new
        // kill re-programs the generator immediately, so the old frequency is overwritten.
        if (huntJob?.isActive == true) {
            log.i(TAG, "Re-run over an active run — cancelling it first")
            huntJob?.cancel()
            huntJob = null
            stopElapsedTicker()
        }

        val parameters = ScanParameters(
            startFrequency = freqs.min(),
            endFrequency = freqs.max(),
            dwellSeconds = dwellSeconds,
            targetAmplitudeCv = amplitudeCv,
            continueRefining = false,
        )
        val hits = freqs.mapIndexed { index, freq ->
            ScanResult(
                frequency = freq,
                reading = 0.0,
                runningAverage = 0.0,
                deviation = deviations.getOrElse(index) { 0.0 },
                hitCount = 1,
            )
        }

        log.i(TAG, "Re-run: treating ${hits.size} saved frequencies (dwell=${dwellSeconds}s, ampCv=$amplitudeCv)")
        runEpoch++
        activeParameters = parameters
        lastOutcome = null
        // Re-run: do NOT persist these frequencies again.
        fromReRun = true
        savedThisRun = true
        pauseGate.resume()
        // Re-runs start with refine OFF by default: the typical intent is "treat the
        // saved list again", not a new biofeedback session. The Kill-screen chip can
        // still turn refinement on mid-run (honored at the pass boundary). The
        // pre-re-run setting is restored when this run ends so the next normal hunt
        // keeps its default.
        val refineBeforeReRun = refineFlag.value
        refineFlag.value = false
        _state.update {
            it.copy(
                phase = HuntPhase.Killing,
                refineHits = false,
                statusText = "Re-running ${hits.size} frequencies…",
                hits = hits,
                graphMarkers = emptyList(),
                fullHistory = FloatArray(0),
                historyValid = BooleanArray(0),
                dropoutSegments = emptyList(),
                killIndex = 0,
                killTotal = hits.size,
                killDwellRemainingSeconds = 0,
                isPaused = false,
                elapsedSeconds = 0,
                estimatedRemainingSeconds = 0,
                errorMessage = null,
                lookupResults = emptyMap(),
                busyAction = null,
            )
        }
        // Surface reverse-lookup matches for the re-run frequencies too.
        runReverseLookup(hits, _state.value.lookupTolerancePercent)
        startElapsedTicker()
        huntJob = scope.launch(Dispatchers.Default) {
            // Re-runs honor the refine flag like any kill (the chip can enable it
            // mid-run); refinement plans windows from the saved frequencies alone.
            // Restore the pre-re-run refine setting once this run is over.
            runCatching { proceedToKill(session, parameters, hits, cycle = 1) }
                .also {
                    refineFlag.value = refineBeforeReRun
                    _state.update { s -> s.copy(refineHits = refineBeforeReRun) }
                }
                .onFailure { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    log.e(TAG, "Re-run kill failed: ${error.message}")
                    stopElapsedTicker()
                    safetyStopInternal(session)
                    _state.update {
                        it.copy(phase = HuntPhase.Error, errorMessage = error.message ?: "Re-run failed", busyAction = null)
                    }
                }
        }
        return true
    }

    /**
     * "Re-scan affected segments": re-sweep the flagged segments (buffered), splice
     * the fresh readings over the old, recompute hits, then proceed to kill with the
     * merged hits. If the re-scan itself fails (cable still bad), surface the error
     * and re-offer the dropout choice so the user can retry.
     */
    fun rescanAffectedSegments() {
        if (_state.value.phase != HuntPhase.HitsReadyWithDropouts) return
        val session = liveSession() ?: return
        val parameters = activeParameters ?: return
        val outcome = lastOutcome ?: return

        log.i(TAG, "User chose Re-scan — re-sweeping ${outcome.segments.size} segment(s)")
        runEpoch++
        pauseGate.resume()
        _state.update { it.copy(phase = HuntPhase.Hunting, rescanInProgress = true, isPaused = false, errorMessage = null) }
        startElapsedTicker()
        huntJob = scope.launch(Dispatchers.Default) {
            runCatching {
                // Mutual-exclusion hook (no-op for now): a later step acquires the hunt
                // slot before re-entering a sweep.
                onAcquireHuntSlot()
                val merged = session.engine.rescanSegments(
                    parameters,
                    outcome.segments,
                    outcome,
                    onProgress = { progress -> onProgress(progress, parameters) },
                    pauseGate = pauseGate,
                )
                lastOutcome = merged
                log.i(
                    TAG,
                    "Re-scan merged: ${merged.segments.size} dropout(s) remain, ${merged.hits.size} hits",
                )
                publishSweepOutcome(merged)
                _state.update { it.copy(rescanInProgress = false, hits = merged.hits) }
                proceedToKill(session, parameters, merged.hits, cycle = 1)
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                log.e(TAG, "Re-scan failed: ${error.message}")
                stopElapsedTicker()
                // Re-offer the dropout choice so the user can retry the re-scan.
                _state.update {
                    it.copy(
                        phase = HuntPhase.HitsReadyWithDropouts,
                        rescanInProgress = false,
                        isPaused = false,
                        errorMessage = error.message ?: "Re-scan failed — cable may still be unstable",
                        busyAction = null,
                    )
                }
            }
        }
    }

    /**
     * MANUAL graph-selection re-scan: re-sweep the user-selected sweep-step range
     * [[startStep], [endStep]] (long-press start, long-press end on the review graph),
     * splice the fresh readings over the old history, re-detect hits, and land back in
     * the SAME review state with updated hits/graph/markers/lookup — it does NOT proceed
     * to kill. Reuses [ScanEngine.rescanSegments], which already splices the re-read
     * steps over [lastOutcome] and re-runs [ScanEngine.detectHits].
     *
     * Availability: only when a completed sweep history exists ([lastOutcome] != null) —
     * i.e. the Hits / dropout-review screen or the Done graph. It is NOT safe to invoke
     * while a fresh sweep is actively running on the same transport: there is a single
     * serial line, so re-entering a sweep over a live one would interleave commands on
     * the same wire. The host only surfaces the selection affordance off an active sweep,
     * and the [lastOutcome]/parameters guard below no-ops in any unexpected state.
     */
    fun rescanManualRange(startStep: Int, endStep: Int) {
        val session = liveSession() ?: run {
            log.e(TAG, "Manual re-scan blocked: not connected")
            return
        }
        val parameters = activeParameters ?: run {
            log.e(TAG, "Manual re-scan blocked: no active parameters")
            return
        }
        val outcome = lastOutcome ?: run {
            log.e(TAG, "Manual re-scan blocked: no completed sweep to splice")
            return
        }

        val lastIndex = outcome.sweepReadings.lastIndex
        if (lastIndex < 0) {
            log.e(TAG, "Manual re-scan blocked: empty sweep history")
            return
        }
        val lo = minOf(startStep, endStep).coerceIn(0, lastIndex)
        val hi = maxOf(startStep, endStep).coerceIn(0, lastIndex)

        // The review phase to restore on failure (or success): dropouts present ⇒
        // the warning state, else the clean Hits state.
        val reviewPhase = if (outcome.segments.isEmpty()) {
            HuntPhase.HitsReady
        } else {
            HuntPhase.HitsReadyWithDropouts
        }

        val freqs = com.spooky2.huntkill.core.scan.ScanEngine.calculateFrequencySteps(parameters)
        val seg = DropoutSegment(
            startStep = lo,
            endStep = hi,
            startFrequency = freqs.getOrElse(lo) { parameters.startFrequency },
            endFrequency = freqs.getOrElse(hi) { parameters.endFrequency },
        )

        log.i(TAG, "Manual re-scan of steps $lo..$hi (${seg.stepCount} steps)")
        runEpoch++
        pauseGate.resume()
        _state.update {
            it.copy(
                phase = HuntPhase.Hunting,
                rescanInProgress = true,
                isPaused = false,
                errorMessage = null,
                busyAction = "Re-scanning selection…",
            )
        }
        startElapsedTicker()
        huntJob = scope.launch(Dispatchers.Default) {
            runCatching {
                // Mutual-exclusion: this re-enters a Hunting sweep on the shared hunt
                // slot, so pause any OTHER running hunt first (consistent with
                // rescanAffectedSegments).
                onAcquireHuntSlot()
                val merged = session.engine.rescanSegments(
                    parameters,
                    listOf(seg),
                    outcome,
                    onProgress = { progress -> onProgress(progress, parameters) },
                    pauseGate = pauseGate,
                )
                lastOutcome = merged
                log.i(
                    TAG,
                    "Manual re-scan merged: ${merged.segments.size} dropout(s), ${merged.hits.size} hits",
                )
                publishSweepOutcome(merged)
                stopElapsedTicker()
                // Land back in the SAME review state — do NOT proceed to kill.
                _state.update {
                    it.copy(
                        phase = if (merged.segments.isEmpty()) HuntPhase.HitsReady else HuntPhase.HitsReadyWithDropouts,
                        rescanInProgress = false,
                        busyAction = null,
                        hits = merged.hits,
                    )
                }
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                log.e(TAG, "Manual re-scan failed: ${error.message}")
                stopElapsedTicker()
                _state.update {
                    it.copy(
                        phase = reviewPhase,
                        rescanInProgress = false,
                        isPaused = false,
                        errorMessage = error.message ?: "Re-scan failed — cable may still be unstable",
                        busyAction = null,
                    )
                }
            }
        }
    }

    private fun onProgress(progress: ScanProgress, parameters: ScanParameters) {
        val isKill = progress.statusText.startsWith("Killing")
        if (isKill && _state.value.phase != HuntPhase.Killing) {
            val count = liveSession()?.engine?.lastResults()?.size ?: 0
            log.i(TAG, "Entering kill phase — $count hits to treat")
        }
        if (isKill) {
            log.d(
                TAG,
                "Kill ${progress.stepNumber}/${progress.totalSteps} " +
                    "freq=${"%.2f".format(progress.currentFrequency)}",
            )
        }
        // Grow the live per-step history during the MAIN sweep only (not kill, not the
        // segment re-scan, which publishes its own merged outcome). The main sweep is
        // the only path that produces provisional hits, so gate on that.
        // Refinement-generation sweeps run on a narrow per-hit grid that does not align
        // with the gen-1 full-range graph — keep their readings off the live history.
        val isMainSweep = !isKill && !progress.statusText.startsWith("Re-scanning") &&
            !refinementSweepActive
        // Lead-in length: the first `raWindow` sweep steps are the unsettled lead-in
        // (the amplitude is still physically settling from the ramp and the detection
        // SMA window has not warmed up). Their low/rising readings are real on the wire
        // but would stretch the live graph's Y-scale (a leading vertical spike at index 0)
        // and trigger false "rising" provisional peaks at the far left. We mark them
        // INVALID for DISPLAY only — detection is unchanged (the engine's ScanOutcome
        // drives the final published history/markers). The mask keeps indices aligned so
        // later markers still map to the correct sweep-step X.
        val leadIn = leadInSteps(parameters)
        val liveSnapshot: FloatArray? = if (isMainSweep && progress.currentReading != 0.0) {
            // stepNumber is 1-based; append in order. Defensive against a missed step.
            val idx = (progress.stepNumber - 1).coerceAtLeast(liveHistory.size)
            while (liveHistory.size <= idx) { liveHistory.add(progress.currentReading.toFloat()); liveValid.add(true) }
            liveHistory[idx] = progress.currentReading.toFloat()
            liveValid[idx] = progress.currentStepValid
            liveHistory.toFloatArray()
        } else {
            null
        }
        val provisionalMarkers: List<GraphMarker>? = if (isMainSweep) {
            // Drop provisional hits that land in the lead-in region so there are no
            // false red dots bunched at the far left while the signal is still settling.
            progress.provisionalHits
                .filter { it.stepIndex >= leadIn }
                .map { GraphMarker(it.stepIndex, it.frequency, it.deviation, isFinal = false) }
        } else {
            null
        }

        _state.update { current ->
            val newHistory = if (progress.currentReading != 0.0 && !isKill) {
                (current.angleHistory + progress.currentReading).takeLast(MAX_HISTORY)
            } else {
                current.angleHistory
            }
            current.copy(
                phase = if (isKill) HuntPhase.Killing else current.phase.coerceHunting(),
                busyAction = null,
                statusText = progress.statusText,
                currentFrequency = progress.currentFrequency,
                amplitudeCv = if (progress.amplitudeCv != 0) progress.amplitudeCv else current.amplitudeCv,
                currentReading = if (progress.currentReading != 0.0) progress.currentReading else current.currentReading,
                runningAverage = if (progress.currentRunningAverage != 0.0) {
                    progress.currentRunningAverage
                } else {
                    current.runningAverage
                },
                percentComplete = progress.percentComplete,
                angleHistory = newHistory,
                fullHistory = liveSnapshot ?: current.fullHistory,
                historyValid = if (liveSnapshot != null) {
                    // A step is shown only when it's past the lead-in AND its read was
                    // valid (engine flag: false for a read failure OR a post-resume settle
                    // step). ScanGraph excludes invalid steps from the Y-scale and the
                    // trace, so the pause/resume spike never appears nor zooms the graph.
                    BooleanArray(liveSnapshot.size) { i -> i >= leadIn && liveValid.getOrElse(i) { true } }
                } else {
                    current.historyValid
                },
                totalSweepSteps = if (isMainSweep && progress.totalSteps > 0) {
                    progress.totalSteps
                } else {
                    current.totalSweepSteps
                },
                graphMarkers = provisionalMarkers ?: current.graphMarkers,
                hits = if (progress.hitsFound > 0 && current.hits.isEmpty()) {
                    // Hits become known once a kill cycle starts; pull them from the engine.
                    liveSession()?.engine?.lastResults() ?: current.hits
                } else {
                    current.hits
                },
                killIndex = if (isKill) progress.stepNumber else current.killIndex,
                killTotal = if (isKill) progress.totalSteps else current.killTotal,
                killDwellRemainingSeconds = if (isKill) {
                    progress.killDwellRemainingSeconds
                } else {
                    current.killDwellRemainingSeconds
                },
                estimatedRemainingSeconds = if (!isKill) {
                    estimateRemaining(current.elapsedSeconds, progress.percentComplete)
                } else {
                    current.estimatedRemainingSeconds
                },
            )
        }
    }

    /**
     * Estimate sweep seconds remaining from elapsed time and percent complete, assuming
     * a roughly constant step rate. Returns 0 until there is enough progress to be
     * meaningful (avoids a wild estimate in the first instants).
     */
    private fun estimateRemaining(elapsedSeconds: Int, percentComplete: Double): Int {
        if (elapsedSeconds <= 0 || percentComplete < 1.0 || percentComplete >= 100.0) return 0
        val total = elapsedSeconds / (percentComplete / 100.0)
        return (total - elapsedSeconds).coerceAtLeast(0.0).toInt()
    }

    private fun HuntPhase.coerceHunting(): HuntPhase =
        if (this == HuntPhase.Killing || this == HuntPhase.Done) this else HuntPhase.Hunting

    /**
     * Number of leading SWEEP steps excluded from the LIVE graph display. These are
     * the unsettled lead-in readings: the amplitude is still physically settling after
     * the ramp and the detection SMA window has not warmed up, so they read low/rising
     * before the curve flattens. Equal to one RA window ([ScanParameters.raWindow]),
     * clamped to >= 0. Display-only — does not affect detection or the final published
     * [ScanOutcome] history/markers.
     */
    private fun leadInSteps(parameters: ScanParameters): Int =
        parameters.raWindow.coerceAtLeast(0)

    /**
     * Return a DISPLAY copy of [sweepValid] with the leading lead-in steps additionally
     * marked invalid, so the final graph excludes the unsettled lead-in just like the live
     * view. The input array is never mutated (the engine [ScanOutcome] keeps the true
     * detection mask). Falls through unchanged when no active parameters are known.
     */
    private fun displayValidWithLeadIn(sweepValid: BooleanArray): BooleanArray {
        val leadIn = activeParameters?.let { leadInSteps(it) } ?: 0
        if (leadIn <= 0) return sweepValid
        val copy = sweepValid.copyOf()
        val end = leadIn.coerceAtMost(copy.size)
        for (i in 0 until end) copy[i] = false
        return copy
    }

    /**
     * Toggle pause/resume on the running hunt. While paused the engine holds at the
     * current frequency (no new commands), the sweep progress and kill countdown
     * freeze, and the elapsed clock stops advancing.
     */
    fun togglePause() {
        val phase = _state.value.phase
        if (phase != HuntPhase.Hunting && phase != HuntPhase.Killing) return

        val nowPaused = !_state.value.isPaused
        if (nowPaused) {
            pauseGate.pause()
            log.i(TAG, "Run paused (hold)")
            _state.update { it.copy(isPaused = true) }
            publishRunStatus()
        } else if (phase == HuntPhase.Hunting) {
            // Resuming into a HUNT: acquire the shared hunt slot (pause any OTHER live
            // hunt) BEFORE opening the gate, so the engine never sweeps concurrently with
            // another hunt. Done inside the coroutine so the gate stays closed until the
            // slot is held — the old code opened the gate before the async pause ran.
            scope.launch {
                onAcquireHuntSlot()
                pauseGate.resume()
                log.i(TAG, "Hunt resumed")
                _state.update { it.copy(isPaused = false) }
                publishRunStatus()
            }
        } else {
            // Resuming a KILL needs no slot (kills overlap freely).
            pauseGate.resume()
            log.i(TAG, "Kill resumed")
            _state.update { it.copy(isPaused = false) }
            publishRunStatus()
        }
    }

    /**
     * Pause THIS controller's hunt because ANOTHER generator is acquiring the hunt slot
     * (hunt mutual-exclusion). Only acts when this controller is actively Hunting and not
     * already paused; kills are unaffected. Holds the engine at the current frequency via
     * [pauseGate] and mirrors the pause into the UI snapshot. Called by the coordinator's
     * [HuntViewModel.pauseOtherHunts] from the acquiring controller's onAcquireHuntSlot hook.
     */
    fun pauseForExclusion() {
        if (_state.value.phase != HuntPhase.Hunting || _state.value.isPaused) return
        log.i(TAG, "Pausing hunt for mutual-exclusion (another generator acquired the slot)")
        pauseGate.pause()
        _state.update { it.copy(isPaused = true) }
        publishRunStatus()
    }

    /**
     * "Treat this now": ask the running kill to immediately jump to hit [index]
     * (0-based) and continue treating from there onward. No-op outside the kill
     * phase. The engine applies the jump on its next dwell slice (after resuming
     * if paused); the kill index/countdown then update via engine progress.
     */
    fun jumpToHit(index: Int) {
        if (_state.value.phase != HuntPhase.Killing) return
        if (index !in _state.value.hits.indices) return
        log.i(TAG, "Jump to hit $index requested (treat now)")
        killControl.requestJump(index)
    }

    /**
     * 1s ticker: advances elapsedSeconds only while running and not paused. Ticker
     * start/stop maps 1:1 to run start/end, so it also drives the foreground-service
     * run notification: started here, fed a status snapshot every tick, ended in
     * [stopElapsedTicker] (which every terminal transition already calls).
     */
    private fun startElapsedTicker() {
        elapsedTicker?.cancel()
        runNotifier?.runStarted()
        publishRunStatus()
        elapsedTicker = scope.launch {
            while (true) {
                delay(1000)
                val s = _state.value
                val running = s.phase == HuntPhase.Hunting || s.phase == HuntPhase.Killing
                if (running && !s.isPaused) {
                    _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
                publishRunStatus()
            }
        }
    }

    private fun stopElapsedTicker() {
        // Idempotent: runEnded() (which ref-counts the shared foreground service) fires
        // ONLY when a ticker was actually running. Without this guard a second call —
        // e.g. dispose() after the run already ended — would decrement the shared count
        // again and stop the service while ANOTHER generator is still running.
        val wasRunning = elapsedTicker != null
        elapsedTicker?.cancel()
        elapsedTicker = null
        if (wasRunning) runNotifier?.runEnded()
    }

    /** Push the current run snapshot to the foreground-service notification. */
    private fun publishRunStatus() {
        val notifier = runNotifier ?: return
        val s = _state.value
        if (!s.isRunning) return
        notifier.publish(
            RunStatus(
                killing = s.phase == HuntPhase.Killing,
                paused = s.isPaused,
                percentComplete = s.percentComplete.toInt().coerceIn(0, 100),
                currentFrequencyHz = s.currentFrequency,
                killIndex = s.killIndex,
                killTotal = s.killTotal,
                dwellRemainingSeconds = s.killDwellRemainingSeconds,
                elapsedSeconds = s.elapsedSeconds,
                remainingSeconds = s.estimatedRemainingSeconds,
                generation = s.refineGeneration,
            ),
        )
    }

    /** Cancel the running scan coroutine and run the safety-stop path. */
    fun cancel() {
        log.w(TAG, "Cancel requested — running safety stop")
        val epoch = ++runEpoch
        pauseGate.resume()
        stopElapsedTicker()
        huntJob?.cancel()
        huntJob = null
        _state.update { it.copy(busyAction = "Cancelling…") }
        scope.launch {
            // Capture the session INSIDE the coroutine so a concurrent generator switch
            // cannot race us: we zero the session that is current at cancellation time,
            // not the one that was current when cancel() was first called.
            val session = liveSession()
            session?.let { safetyStopInternal(it) }
            // A startHunt() between cancel() and here bumped runEpoch — don't stamp
            // Cancelled over the new run. The safety-stop (zeroing) above still ran,
            // and the new run re-programs the generator immediately.
            if (runEpoch != epoch) return@launch
            _state.update {
                it.copy(
                    phase = HuntPhase.Cancelled,
                    statusText = "Cancelled — generator zeroed",
                    killDwellRemainingSeconds = 0,
                    isPaused = false,
                    rescanInProgress = false,
                    busyAction = null,
                )
            }
            _events.trySend("Generator zeroed")
            // If the cancel happened after hits were already detected, still surface
            // their reverse-lookup matches (results not yet computed for this run).
            val hits = _state.value.hits
            if (hits.isNotEmpty() && _state.value.lookupResults.isEmpty()) {
                runReverseLookup(hits, _state.value.lookupTolerancePercent)
            }
        }
    }

    /** Explicit safety-stop button: zero the generator output immediately. */
    fun safetyStop() = cancel()

    /**
     * Reset a terminal phase (Done/Cancelled/Error) back to Idle when the user returns
     * to the Hunt config screen, so the config UI isn't stuck showing a finished run.
     * Keeps params + generator info. No-op while a run is active.
     */
    fun prepareForConfig() {
        refreshGeneratorInfo()
        if (_state.value.isRunning) return
        _state.update {
            it.copy(
                phase = HuntPhase.Idle,
                statusText = "",
                errorMessage = null,
                killDwellRemainingSeconds = 0,
            )
        }
    }

    /** Pull connection info from the current session into [HuntUiState.generator]. */
    fun refreshGeneratorInfo() {
        val session = liveSession()
        _state.update {
            it.copy(
                generator = session?.let { s ->
                    GeneratorInfo(
                        generatorType = s.generatorType,
                        baudRate = s.baudRate,
                        portIndex = s.usbPort?.index,
                        portCount = s.usbPort?.count,
                        serialNumber = s.serialNumber,
                        firmwareVersion = s.firmwareVersion,
                        hardwareType = s.hardwareType,
                    )
                },
            )
        }
    }

    /**
     * Zero the generator: clear both frequency channels, set amplitude CV to 0 on both
     * outputs, then stop outputs. Delegates to [GeneratorSession.client] zeroOutput,
     * which is itself fully guarded so it never throws even on a half-open transport.
     */
    private suspend fun safetyStopInternal(session: GeneratorSession) {
        runCatching { session.client.zeroOutput() }
    }

    /**
     * Re-run reverse lookup at a different [tolerancePercent] from the Hits screen.
     * Re-uses the already-loaded database, so this is fast; cancels any in-flight lookup.
     * No-op when there are no hits.
     */
    fun setLookupTolerance(tolerancePercent: Double) {
        if (_state.value.lookupTolerancePercent == tolerancePercent && _state.value.lookupResults.isNotEmpty()) {
            return
        }
        val hits = _state.value.hits
        _state.update { it.copy(lookupTolerancePercent = tolerancePercent) }
        if (hits.isEmpty()) return
        runReverseLookup(hits, tolerancePercent)
    }

    /**
     * Match each [hits] frequency against the bundled frequency database and store the
     * results in [HuntUiState.lookupResults]. Runs on [Dispatchers.Default] off the UI
     * thread; the database load itself happens on IO inside the repository. Cancellation
     * of [lookupJob] (re-run with a new tolerance, or VM cleared) is honoured between hits.
     */
    private fun runReverseLookup(hits: List<ScanResult>, tolerancePercent: Double) {
        val repository = frequencyDatabase ?: return
        if (hits.isEmpty()) return

        lookupJob?.cancel()
        _state.update { it.copy(lookupBusy = true) }
        lookupJob = scope.launch(Dispatchers.Default) {
            val database = runCatching { repository.database() }.getOrElse { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                log.e(TAG, "Reverse lookup DB load failed: ${error.message}")
                _state.update { it.copy(lookupBusy = false) }
                return@launch
            }

            val params = ReverseLookupParameters(tolerancePercent = tolerancePercent)
            val results = LinkedHashMap<Double, List<LookupMatch>>(hits.size)
            for (hit in hits) {
                coroutineContext.ensureActive()
                results[hit.frequency] = ReverseLookup.lookup(hit.frequency, database.entries, params)
            }

            logLookupSummary(tolerancePercent, hits, results)
            _state.update { it.copy(lookupResults = results, lookupBusy = false) }
        }
    }

    /**
     * On-demand reverse lookup for a SINGLE frequency — used by the graph marker
     * popup, where a provisional (live) hit may not yet be in [HuntUiState.lookupResults].
     * Returns the matches (empty list = no matches) or null when the database source is
     * unavailable (JVM tests) or the load failed. Runs off the UI thread; cancellation-safe.
     */
    suspend fun lookupForFrequency(
        frequency: Double,
        tolerancePercent: Double = _state.value.lookupTolerancePercent,
    ): List<LookupMatch>? {
        _state.value.lookupResults[frequency]?.let { return it }
        val repository = frequencyDatabase ?: return null
        return withContext(Dispatchers.Default) {
            val database = runCatching { repository.database() }.getOrElse { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                log.e(TAG, "Marker lookup DB load failed: ${error.message}")
                return@withContext null
            }
            val params = ReverseLookupParameters(tolerancePercent = tolerancePercent)
            ReverseLookup.lookup(frequency, database.entries, params)
        }
    }

    /** Emit a per-hit summary so the matches land in the daily log files / ZIP export. */
    private fun logLookupSummary(
        tolerancePercent: Double,
        hits: List<ScanResult>,
        results: Map<Double, List<LookupMatch>>,
    ) {
        log.i(TAG, "Reverse lookup @ ${tolerancePercent}% tolerance — ${hits.size} hits:")
        hits.forEach { hit ->
            val matches = results[hit.frequency].orEmpty()
            val top = matches.take(LOOKUP_SUMMARY_TOP)
                .joinToString("; ") { it.toReportLine() }
                .ifEmpty { "No matches" }
            val more = if (matches.size > LOOKUP_SUMMARY_TOP) " (+${matches.size - LOOKUP_SUMMARY_TOP} more)" else ""
            log.i(TAG, "  ${"%.2f".format(hit.frequency)} Hz → $top$more")
        }
    }

    /**
     * Close the active session and reset to a disconnected state. Used by the
     * "Disconnect" action on the post-run summary before navigating to Connect.
     *
     * Session teardown is delegated to the coordinator via [onDisconnect] (the
     * controller does not own the SessionHolder); the post-clear state reset is done here.
     */
    fun disconnect(onDisconnect: suspend () -> Unit) {
        log.i(TAG, "Disconnect requested")
        pauseGate.resume()
        stopElapsedTicker()
        huntJob?.cancel()
        huntJob = null
        _state.update { it.copy(busyAction = "Disconnecting…") }
        scope.launch {
            runCatching { onDisconnect() }
            _state.update { HuntUiState(params = it.params) }
        }
    }

    /**
     * USB device-detach handler. Cancels the running hunt job, freezes the clock, and
     * transitions to Error. Session teardown (marking the session dead) is delegated to
     * the coordinator via [onSessionLost] since the controller does not own the holder.
     */
    fun onDeviceDetached(onSessionLost: suspend () -> Unit) {
        scope.launch {
            pauseGate.resume()
            stopElapsedTicker()
            huntJob?.cancel()
            huntJob = null
            // Mark the session dead so subsequent startHunt blocks on "not connected".
            onSessionLost()
            _state.update {
                it.copy(
                    phase = HuntPhase.Error,
                    statusText = "Generator unplugged",
                    errorMessage = "Generator unplugged",
                    isPaused = false,
                    rescanInProgress = false,
                    busyAction = null,
                )
            }
        }
    }

    /** Cancel jobs/scope children. Called by the coordinator from ViewModel.onCleared(). */
    fun dispose() {
        stopElapsedTicker()
        huntJob?.cancel()
        lookupJob?.cancel()
    }

    companion object {
        private const val MAX_HISTORY = 200
        private const val TAG = "Hunt"

        /** How many matches per hit are written to the log summary. */
        private const val LOOKUP_SUMMARY_TOP = 5
    }
}

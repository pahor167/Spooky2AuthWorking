package com.spooky2.huntkill.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped registry of active [GeneratorSession]s, keyed by USB port index.
 *
 * The GeneratorX box exposes two serial ports (two generators) on one USB device. This
 * holder stores one session per port index (0-based) so both can be open simultaneously.
 * Single-generator callers that use the back-compat shim API ([set], [current],
 * [acquireForHunt], [replace]) see an unchanged behaviour: their session lives at key 0.
 *
 * Connect/Auth establishes sessions here; Hunt/Live/Kill read them back. A [Singleton]
 * holder is the simplest correct way to share open transports across navigation-scoped
 * ViewModels without re-running the handshake on every screen.
 */
@Singleton
class SessionHolder @Inject constructor() {

    // ── Multi-session registry ────────────────────────────────────────────────

    private val _sessions = MutableStateFlow<Map<Int, GeneratorSession>>(emptyMap())

    /** Snapshot of all currently-open sessions, keyed by port index. */
    val sessions: StateFlow<Map<Int, GeneratorSession>> = _sessions.asStateFlow()

    /** Store [session] under [portIndex], replacing any previous entry at that key. */
    fun put(portIndex: Int, session: GeneratorSession) {
        _sessions.value = _sessions.value + (portIndex to session)
    }

    /** Return the session at [portIndex], or null if no session is open on that port. */
    fun get(portIndex: Int): GeneratorSession? = _sessions.value[portIndex]

    /**
     * All open sessions ordered by ascending port index. The first entry (index 0) is
     * the "primary" session used by the single-session back-compat API.
     */
    fun all(): List<GeneratorSession> =
        _sessions.value.entries.sortedBy { it.key }.map { it.value }

    /**
     * Close and remove the session at [portIndex]. No-op if no session is registered
     * there. Transport-close errors are swallowed (the session is already being removed);
     * [CancellationException] is always rethrown.
     */
    suspend fun remove(portIndex: Int) {
        val session = _sessions.value[portIndex] ?: return
        _sessions.value = _sessions.value - portIndex
        try {
            session.close()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // swallow transport errors on close
        }
    }

    /** Close every open session and clear the registry. [CancellationException] rethrown. */
    suspend fun clear() {
        val snapshot = _sessions.value
        _sessions.value = emptyMap()
        for (session in snapshot.values) {
            try {
                session.close()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // swallow transport errors on close
            }
        }
    }

    // ── Back-compat shims (single-session callers) ────────────────────────────
    //
    // All existing callers (HuntViewModel, tests) use set/current/setReconnect/
    // acquireForHunt/replace.  These shims map the single-session contract onto the
    // registry at key 0 (or the session's own port index when available).
    //
    // IMPORTANT: Do not remove these until HuntViewModel and all tests are migrated.

    /**
     * Optional per-hunt reconnector. The live USB path leaves this null so the open
     * session is reused across hunts. The no-hardware replay tests set it to rebuild a
     * fresh single-use FakeTransport session each run.
     */
    private var reconnect: (suspend () -> GeneratorSession)? = null

    /**
     * Store [session] in the registry.
     *
     * Key = [GeneratorSession.usbPort].index when available, 0 otherwise. This matches
     * the behaviour expected by single-session callers (test/HuntViewModel) which always
     * call `set(session)` with the session they just obtained and then `current()` to
     * retrieve it.
     */
    fun set(session: GeneratorSession) {
        val key = session.usbPort?.index ?: 0
        put(key, session)
    }

    fun setReconnect(block: (suspend () -> GeneratorSession)?) {
        reconnect = block
    }

    /**
     * Resolve the session to run a hunt on. If a reconnector is set, build a fresh
     * session and swap it in (test replay reset); otherwise reuse the current one.
     *
     * "Current" for this shim is the session at the lowest registered key (key 0 in
     * the typical single-session setup).
     */
    suspend fun acquireForHunt(): GeneratorSession? = acquireForHunt(portKey = null)

    /**
     * Port-aware hunt acquire. A reconnector (test replay) always wins — it rebuilds a
     * fresh session and swaps it in. Otherwise, a keyed controller resolves EXACTLY its
     * own [portKey] (so a port-1 hunt never runs on the port-0 session); a null key
     * falls back to the lowest-key "current" session (single-session/back-compat path).
     */
    suspend fun acquireForHunt(portKey: Int?): GeneratorSession? {
        val block = reconnect
        return when {
            block != null -> block().also { replace(it) }
            portKey != null -> get(portKey)
            else -> current()
        }
    }

    /**
     * Swap in a new session at the same key as [session], closing the previous one first.
     *
     * Key is derived the same way as [set]: [GeneratorSession.usbPort].index ?: 0.
     */
    suspend fun replace(session: GeneratorSession) {
        val key = session.usbPort?.index ?: 0
        val previous = _sessions.value[key]
        put(key, session)
        if (previous != null && previous !== session) {
            try {
                previous.close()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // swallow transport errors on close
            }
        }
    }

    /**
     * Return the "current" session for single-session callers: the session at the lowest
     * registered key, or null if the registry is empty.
     */
    fun current(): GeneratorSession? = _sessions.value.entries.minByOrNull { it.key }?.value

    // ── Legacy single-session StateFlow (kept for any future observers) ───────
    //
    // The old _session / session StateFlow has been replaced by _sessions / sessions.
    // No production code was observed to observe sessionHolder.session, so the property
    // is not re-exposed here. Add it back as a derived flow if needed:
    //
    //   val session: StateFlow<GeneratorSession?> = sessions
    //       .map { it.values.firstOrNull() }
    //       .stateIn(scope, SharingStarted.Eagerly, null)
}

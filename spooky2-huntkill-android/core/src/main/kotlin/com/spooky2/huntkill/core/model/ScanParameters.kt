package com.spooky2.huntkill.core.model

/**
 * Hunt & Kill scan configuration.
 *
 * Verbatim port of the C# reference `Spooky2.Core.Models.ScanParameters`.
 * Default values are copied exactly from the C# record and reflect the
 * GX Hunt and Kill preset cross-checked against the recorded scan dumps.
 */
data class ScanParameters(
    val startFrequency: Double = 41000.0,
    val endFrequency: Double = 1800000.0,
    val usePercentageStep: Boolean = true,
    val stepSizeHz: Double = 100.0,
    val stepSizePercent: Double = 0.025,
    val maxHits: Int = 10,
    /** Primary RA window size (BFB_RA_Window_1). */
    val raWindow: Int = 20,
    /** Secondary RA window size (BFB_RA_Window_2). 0 = same as [raWindow]. */
    val raWindow2: Int = 0,
    /** Use the secondary (retentive) RA window for detection. */
    val useRetentiveWindow: Boolean = false,
    /** Use peak detection instead of running average. */
    val calculateUsingPeak: Boolean = false,
    val samplesPerStep: Int = 1,
    val startDelayMs: Int = 200,
    /**
     * Minimum sweep-step PERIOD (write-to-write), in seconds. Default 0.07 = 70 ms.
     *
     * Derived from the original software's timestamped serial dump, which sweeps
     * at 14-15 steps/s (≈70 ms/step). This is the minimum time between successive
     * `:w24` frequency writes — NOT an additive sleep. The per-step serial I/O
     * (frequency write + sensor reads) counts toward this period; the engine only
     * sleeps the remainder. 0 = unpaced fast path (test/replay).
     */
    val minReadDelaySeconds: Double = 0.07,
    val detectMax: Boolean = true,
    val detectMin: Boolean = false,
    /**
     * Detection settle warm-up tolerance (fraction of the window mean). Detection does
     * not score any sweep step until the SMA window has "settled": the window's
     * (max − min) ≤ [settleToleranceFraction] × windowMean. The first step (≥ [raWindow],
     * so the window is full) that satisfies this is `warmupStart`; scoring begins there.
     *
     * This is a one-time LEADING warm-up: it suppresses a generator/sensor startup
     * transient (the first few sweep readings sitting at the baseline level before they
     * JUMP to the settled level) from being selected as a false top hit. It does NOT
     * re-gate later steps — a real peak legitimately widens the window range. If no
     * settled window is found within a cap (5 × [raWindow]) the warm-up falls back to
     * [raWindow] so a genuinely noisy scan is never fully discarded. Default 0.01 (1%).
     */
    val settleToleranceFraction: Double = 0.01,
    /**
     * Use current (mA) sensor for hit detection.
     * Despite the GX Hunt and Kill preset saying BFB_Detect_mA=True,
     * empirical testing against real scan data proves the original uses
     * angle/phase for detection — angle finds all 10 expected hits across
     * all parameter combinations, while current produces spurious hits at
     * 980K/1150K Hz that mask weaker real hits.
     */
    val useCurrent: Boolean = false,
    val useAngle: Boolean = true,
    val loops: Int = 1,
    val threshold: Double = 0.0,
    val continueRefining: Boolean = true,
    /**
     * Generator ID for kill phase output. 0 = same generator as scan.
     * Maps to preset BFB_After_Scan_Run_On_Gen=0.
     */
    val runOnGeneratorId: Int = 0,
    /**
     * Kill phase dwell time per frequency. Default 180s (3 minutes) from dump analysis.
     * No preset field — derived from observed behavior.
     */
    val dwellSeconds: Double = 180.0,
    val logName: String = "",

    // Amplitude ramp-up (from preset: Enable_Amplitude_RampUp, Ramp_Amplitude_Up_Rate)
    /** Enable gradual amplitude ramp from near-zero to target before scanning. */
    val enableAmplitudeRampUp: Boolean = true,
    /**
     * Number of amplitude ramp steps. Default 330 from dump analysis.
     * Preset has Ramp_Amplitude_Up_Rate=4 but the mapping formula is unknown.
     * Verified from dump: 330 steps, formula round((i+1)*target/330).
     */
    val rampSteps: Int = 330,
    /**
     * Target amplitude in centivolt. Default 2000 = 20V.
     * Maps to preset Out1_Amplitude=20.
     */
    val targetAmplitudeCv: Int = 2000,
    /** Enable amplitude ramp-down after scan. */
    val enableAmplitudeRampDown: Boolean = true,
    /**
     * Number of baseline :r11/:r12 read pairs before sweeping.
     * Dump shows 203 pairs (+ 1 initial standalone :r11 = 407 total reads).
     */
    val baselineReadCount: Int = 203,

    // ── Dropout detection (cable disconnect / half-broken connection) ──
    /**
     * Minimum contiguous run length (steps) for the deviation heuristic to flag
     * a dropout. Short excursions (1-2 steps) are normal biofeedback variation;
     * a sustained plateau of >= this many steps is cable garbage.
     */
    val dropoutMinRunLength: Int = 3,
    /**
     * Fractional deviation from the surrounding rolling median above which a step
     * is considered an outlier candidate (0.10 = 10%). Tuned so the observed
     * failure (collapse to a far-away plateau) is caught but normal variation
     * (a few percent) is not.
     */
    val dropoutDeviationFraction: Double = 0.10,
    /**
     * Window radius (steps on each side) for the rolling median used by the
     * deviation heuristic. The median is taken over valid readings in this window.
     */
    val dropoutMedianWindow: Int = 25,
    /**
     * Consecutive failed reads after which the live "connection unstable" status
     * is surfaced (the sweep keeps going, values flagged).
     */
    val dropoutUnstableReadThreshold: Int = 5,
) {
    init {
        require(samplesPerStep > 0) { "samplesPerStep must be > 0, got $samplesPerStep" }
    }
}

package com.spooky2.huntkill.core.scan

import java.util.ArrayDeque

/**
 * Sliding window used for the Running Average (RA) in hit detection.
 *
 * Verbatim port of the C# reference `ScanService.SlidingWindow`.
 * Production detection uses [simpleAverage] (SMA), as decoded from VB6
 * `Proc_0_331`. [weightedAverage] (LWMA) is kept for compatibility.
 */
internal class SlidingWindow(size: Int) {

    private val size: Int = maxOf(1, size)
    private val buffer: ArrayDeque<Double> = ArrayDeque(this.size)

    fun add(value: Double) {
        buffer.addLast(value)
        if (buffer.size > size) buffer.removeFirst()
    }

    val isFull: Boolean
        get() = buffer.size >= size

    val count: Int
        get() = buffer.size

    /** Simple Moving Average (SMA). Matches C# `SimpleAverage()`. */
    fun simpleAverage(): Double =
        if (buffer.isEmpty()) 0.0 else buffer.sum() / buffer.size

    /**
     * Linearly Weighted Moving Average.
     * LWMA = (N×newest + (N-1)×next + ... + 1×oldest) / (N×(N+1)/2).
     * Kept for compatibility; production scan detection uses [simpleAverage].
     */
    fun weightedAverage(): Double {
        if (buffer.isEmpty()) return 0.0
        val n = buffer.size
        var weightedSum = 0.0
        var weight = 1
        for (value in buffer) {
            weightedSum += value * weight
            weight++
        }
        return weightedSum / (n * (n + 1) / 2.0)
    }

    fun peak(): Double = if (buffer.isEmpty()) 0.0 else buffer.max()

    /**
     * True when the window's spread is within [toleranceFraction] of its mean, i.e.
     * `(max − min) ≤ toleranceFraction × mean`. Used by the detection settle warm-up
     * to detect that the signal has stopped jumping (generator/sensor has settled).
     * An empty or non-positive-mean window is treated as not settled.
     */
    fun isSettled(toleranceFraction: Double): Boolean =
        isSettled(toleranceFraction, reading = null)

    /**
     * Settle test for the detection warm-up. The window is "settled at the signal
     * level" when BOTH:
     *   1. its internal spread is small: `(max − min) ≤ toleranceFraction × mean`, AND
     *   2. the incoming [reading] is consistent with the window — no level
     *      discontinuity: `abs(reading − mean) ≤ toleranceFraction × mean`.
     *
     * Clause 2 is what rejects the baseline↔sweep boundary: when the SMA window is
     * pre-seeded with the (internally homogeneous) baseline level but the incoming
     * sweep reading has JUMPED to a different settled level, clause 1 alone returns
     * true (the window is flat) yet the reading is far from the window mean → clause 2
     * is false, so warm-up correctly extends past the discontinuity.
     *
     * Passing [reading] = null skips clause 2 (range-only settle, kept for callers
     * that only care about the window's own homogeneity). An empty or non-positive-mean
     * window is treated as not settled.
     */
    fun isSettled(toleranceFraction: Double, reading: Double?): Boolean {
        if (buffer.isEmpty()) return false
        var min = Double.MAX_VALUE
        var max = -Double.MAX_VALUE
        var sum = 0.0
        for (value in buffer) {
            if (value < min) min = value
            if (value > max) max = value
            sum += value
        }
        val mean = sum / buffer.size
        if (mean <= 0.0) return false
        val tol = toleranceFraction * mean
        if ((max - min) > tol) return false
        if (reading != null && kotlin.math.abs(reading - mean) > tol) return false
        return true
    }
}

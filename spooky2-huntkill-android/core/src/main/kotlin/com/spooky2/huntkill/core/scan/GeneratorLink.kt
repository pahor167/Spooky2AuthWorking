package com.spooky2.huntkill.core.scan

/**
 * Minimal, pure-Kotlin abstraction the [ScanEngine] talks to.
 *
 * This mirrors only the methods the C# `ScanService` uses from
 * `Spooky2.Core.Interfaces.IGeneratorService` — keeping the `core` module
 * Android-free. A real implementation lives in the Android `transport` module;
 * tests provide a fake that replays a recorded serial dump.
 *
 * All operations are `suspend` (the Kotlin idiom replacing C# `Task`).
 */
interface GeneratorLink {

    /**
     * Send a single command and wait for the device response string.
     * Mirrors `IGeneratorService.SendCommandWithResponse`.
     * Returns the raw response (e.g. `":r11=52458."`, `":ok"`) or null.
     */
    suspend fun sendCommandWithResponse(command: String): String?

    /**
     * Send multiple commands rapidly without waiting for individual responses.
     * Used for amplitude ramp-up and waveform-table upload.
     * Mirrors `IGeneratorService.SendCommandsBatch`.
     */
    suspend fun sendCommandsBatch(commands: List<String>)

    /**
     * Write one or more output frequencies (kill phase).
     * Mirrors `IGeneratorService.WriteFrequencies`.
     */
    suspend fun writeFrequencies(frequencies: List<Double>)

    /** Start generator output. Mirrors `IGeneratorService.Start`. */
    suspend fun start()

    /** Stop generator output. Mirrors `IGeneratorService.Stop`. */
    suspend fun stop()
}

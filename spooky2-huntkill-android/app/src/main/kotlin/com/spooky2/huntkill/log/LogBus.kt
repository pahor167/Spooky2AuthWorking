package com.spooky2.huntkill.log

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** One captured log line. [level] is a single char: 'd', 'i', 'w', or 'e'. */
data class LogEntry(
    val timestampMs: Long,
    val level: Char,
    val tag: String,
    val message: String,
)

/**
 * App-scoped in-memory log sink. A thread-safe ring buffer (capacity [CAPACITY])
 * that also forwards to [android.util.Log]. The UI observes [entries]; the
 * background IO threads of the transport call [d]/[i]/[w]/[e] freely.
 *
 * All mutations are synchronized so it is safe to call from any thread.
 */
@Singleton
class LogBus @Inject constructor(
    /**
     * Optional persistent sink. When present (Hilt runtime) every entry is mirrored to a
     * daily log file off the I/O path. Tests use the no-arg secondary constructor, which
     * passes null so [LogBus] stays constructible without Android.
     */
    private val fileWriter: FileLogWriter?,
) {

    /** No-Android constructor for unit tests: in-memory only, no file persistence. */
    constructor() : this(null)

    private val lock = Any()
    private val buffer = ArrayDeque<LogEntry>(CAPACITY)

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())

    /** Snapshot list, oldest → newest, emitted on every change. */
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun d(tag: String, msg: String) = add('d', tag, msg)
    fun i(tag: String, msg: String) = add('i', tag, msg)
    fun w(tag: String, msg: String) = add('w', tag, msg)
    fun e(tag: String, msg: String) = add('e', tag, msg)

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            _entries.value = emptyList()
        }
    }

    /** Formatted `HH:mm:ss.SSS L/tag: msg` lines, oldest → newest. */
    fun dump(): String {
        val snapshot = synchronized(lock) { buffer.toList() }
        return snapshot.joinToString("\n") { format(it) }
    }

    /** Force any buffered file-log lines to disk (no-op when persistence is off). */
    fun flush() {
        fileWriter?.flush()
    }

    private fun add(level: Char, tag: String, msg: String) {
        forwardToAndroidLog(level, tag, msg)
        val timestampMs = System.currentTimeMillis()
        // Persist the full history to file off the I/O path; the ring is UI-only.
        fileWriter?.append(timestampMs, level, tag, msg)
        val entry = LogEntry(timestampMs, level, tag, msg)
        synchronized(lock) {
            if (buffer.size >= CAPACITY) buffer.removeFirst()
            buffer.addLast(entry)
            _entries.value = buffer.toList()
        }
    }

    private fun forwardToAndroidLog(level: Char, tag: String, msg: String) {
        when (level) {
            'd' -> Log.d(tag, msg)
            'i' -> Log.i(tag, msg)
            'w' -> Log.w(tag, msg)
            'e' -> Log.e(tag, msg)
            else -> Log.i(tag, msg)
        }
    }

    private fun format(entry: LogEntry): String =
        "${timeFormat.format(Instant.ofEpochMilli(entry.timestampMs))} ${entry.level}/${entry.tag}: ${entry.message}"

    companion object {
        const val CAPACITY = 2000

        // DateTimeFormatter is immutable and thread-safe (unlike SimpleDateFormat).
        // Device-local zone so in-app log lines match the wall clock during a run.
        private val timeFormat: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
    }
}

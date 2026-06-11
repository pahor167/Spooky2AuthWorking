package com.spooky2.huntkill.log

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Persistent, append-only log file sink. Mirrors the in-memory [LogBus] buffer to a
 * daily file `filesDir/logs/huntkill-YYYY-MM-DD.log` using the same
 * `HH:mm:ss.SSS L/tag: msg` line format — the FILES are the full history, the in-memory
 * ring is only for the UI.
 *
 * All disk I/O runs on a dedicated single-thread executor so the serial I/O path (which
 * calls [append] from its IO threads) never blocks on a flush. The buffered writer is
 * flushed every [FLUSH_EVERY_LINES] lines and, defensively, on day rollover and
 * [close]. [flush] forces an immediate flush.
 *
 * Retention: files older than [RETENTION_DAYS] days are pruned on init and on each day
 * rollover (today + the previous two days are always kept).
 */
@Singleton
class FileLogWriter @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val logsDir: File = File(context.filesDir, "logs").apply { mkdirs() }
    private val cacheDir: File = context.cacheDir

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "file-log-writer").apply { isDaemon = true }
    }

    // Only ever touched on the executor thread.
    private var currentDay: String? = null
    private var writer: BufferedWriter? = null
    private var sinceFlush = 0

    init {
        executor.execute { pruneOldFiles() }
    }

    /** Format identical to [LogBus]; the file history reuses the on-screen line shape. */
    fun append(timestampMs: Long, level: Char, tag: String, message: String) {
        executor.execute {
            runCatching {
                val line = "${timeFormat.format(Instant.ofEpochMilli(timestampMs))} $level/$tag: $message"
                rolloverIfNeeded(timestampMs)
                writer?.apply {
                    write(line)
                    newLine()
                    // FIX 3: flush immediately after error or warn lines so those are never lost.
                    if (level == 'e' || level == 'w') {
                        flush()
                        sinceFlush = 0
                    }
                }
                if (level != 'e' && level != 'w' && ++sinceFlush >= FLUSH_EVERY_LINES) {
                    writer?.flush()
                    sinceFlush = 0
                }
            }
        }
    }

    /** Force any buffered lines to disk (e.g. before sharing/exporting). */
    fun flush() {
        executor.execute {
            runCatching {
                writer?.flush()
                sinceFlush = 0
            }
        }
    }

    /**
     * Zip every file in the logs dir into `cacheDir/logs-export-<timestamp>.zip` and
     * return it. Flushes pending lines first so the export is current. Runs on the
     * executor thread and blocks the caller until the zip is written.
     */
    fun exportZip(): File {
        val target = File(cacheDir, "logs-export-${fileStampFormat.format(Instant.now())}.zip")
        val task = executor.submit<File> {
            writer?.flush()
            sinceFlush = 0
            ZipOutputStream(target.outputStream().buffered()).use { zip ->
                logsDir.listFiles()?.sortedBy { it.name }?.forEach { file ->
                    if (file.isFile) {
                        zip.putNextEntry(ZipEntry(file.name))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
            target
        }
        return task.get()
    }

    private fun rolloverIfNeeded(timestampMs: Long) {
        val day = dayFormat.format(Instant.ofEpochMilli(timestampMs))
        if (day == currentDay && writer != null) return

        runCatching { writer?.flush() }
        runCatching { writer?.close() }
        currentDay = day
        sinceFlush = 0
        val file = File(logsDir, "huntkill-$day.log")
        writer = BufferedWriter(FileWriter(file, /* append = */ true))
        pruneOldFiles()
    }

    /** Delete log files whose day is older than [RETENTION_DAYS] days from today. */
    private fun pruneOldFiles() {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(RETENTION_DAYS.toLong())
        logsDir.listFiles()?.forEach { file ->
            val day = file.name.removePrefix(FILE_PREFIX).removeSuffix(FILE_SUFFIX)
            val dayMs = runCatching {
                dayFormat.parse(day, java.time.temporal.TemporalQueries.localDate())
                    ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            }.getOrNull()
            // Compare by parsed day so a stale system clock doesn't delete by mtime alone.
            if (dayMs != null && dayMs < cutoff) {
                runCatching { file.delete() }
            }
        }
    }

    /**
     * Flush pending lines and close the underlying writer. Shuts down the executor so
     * all queued tasks complete first (up to 5 s). Safe to call multiple times.
     *
     * NOTE: Do NOT wire this to `Application.onTerminate` — Android does not guarantee
     * that callback is ever invoked. Prefer calling it from an explicit lifecycle boundary
     * (e.g. a WorkManager finish callback or a test tear-down) where deterministic flushing
     * is needed.
     */
    fun close() {
        executor.execute {
            runCatching { writer?.flush() }
            runCatching { writer?.close() }
            writer = null
        }
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    companion object {
        /** Today + the two previous days are retained; strictly older files are pruned. */
        const val RETENTION_DAYS = 3

        private const val FLUSH_EVERY_LINES = 20
        private const val FILE_PREFIX = "huntkill-"
        private const val FILE_SUFFIX = ".log"

        // DateTimeFormatter is immutable and thread-safe (unlike SimpleDateFormat).
        // Device-local zone so log lines match the wall clock the user sees during a run
        // and daily files roll at local midnight (same behavior as the old SimpleDateFormat).
        private val timeFormat: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
        private val dayFormat: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
        private val fileStampFormat: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss").withZone(ZoneId.systemDefault())
    }
}

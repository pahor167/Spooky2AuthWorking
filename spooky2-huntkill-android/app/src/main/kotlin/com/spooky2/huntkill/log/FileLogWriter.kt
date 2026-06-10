package com.spooky2.huntkill.log

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
        val line = "${timeFormat.format(Date(timestampMs))} $level/$tag: $message"
        executor.execute {
            runCatching {
                rolloverIfNeeded(timestampMs)
                writer?.apply {
                    write(line)
                    newLine()
                }
                if (++sinceFlush >= FLUSH_EVERY_LINES) {
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
        val target = File(cacheDir, "logs-export-${fileStampFormat.format(Date())}.zip")
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
        val day = dayFormat.format(Date(timestampMs))
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
            val dayMs = runCatching { dayFormat.parse(day)?.time }.getOrNull()
            // Compare by parsed day so a stale system clock doesn't delete by mtime alone.
            if (dayMs != null && dayMs < cutoff) {
                runCatching { file.delete() }
            }
        }
    }

    companion object {
        /** Today + the two previous days are retained; strictly older files are pruned. */
        const val RETENTION_DAYS = 3

        private const val FLUSH_EVERY_LINES = 20
        private const val FILE_PREFIX = "huntkill-"
        private const val FILE_SUFFIX = ".log"

        private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val fileStampFormat = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US)
    }
}

package com.spooky2.huntkill.log

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

/**
 * JVM tests for [FileLogWriter]: lines land in a daily file, retention prunes files
 * older than 3 days, and [exportZip] zips the whole logs dir. Context.filesDir /
 * cacheDir are backed by real temp folders so the real disk path is exercised.
 */
class FileLogWriterTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // Match the device-local zone used by FileLogWriter's dayFormat so expected filenames align.
    private val dayFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

    private fun context(filesDir: File, cacheDir: File): Context = mockk<Context>().also {
        every { it.filesDir } returns filesDir
        every { it.cacheDir } returns cacheDir
    }

    @Test
    fun `append writes a line into today's daily file`() {
        val files = tmp.newFolder("files")
        val cache = tmp.newFolder("cache")
        val writer = FileLogWriter(context(files, cache))

        writer.append(System.currentTimeMillis(), 'i', "Test", "hello world")
        writer.flush()

        val today = dayFormat.format(Instant.now())
        val logFile = File(File(files, "logs"), "huntkill-$today.log")
        waitUntil { logFile.exists() && logFile.readText().contains("hello world") }

        assertTrue(logFile.exists())
        val text = logFile.readText()
        assertTrue("line keeps the L/tag: msg shape", text.contains("i/Test: hello world"))
    }

    @Test
    fun `retention prunes files older than three days on init`() {
        val files = tmp.newFolder("files")
        val cache = tmp.newFolder("cache")
        val logsDir = File(files, "logs").apply { mkdirs() }

        // Old file: 5 days ago -> should be pruned. Recent: 1 day ago -> kept.
        val old = dayFormat.format(Instant.ofEpochMilli(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)))
        val recent = dayFormat.format(Instant.ofEpochMilli(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)))
        val oldFile = File(logsDir, "huntkill-$old.log").apply { writeText("old\n") }
        val recentFile = File(logsDir, "huntkill-$recent.log").apply { writeText("recent\n") }

        FileLogWriter(context(files, cache)) // init prunes on the executor thread

        waitUntil { !oldFile.exists() }
        assertFalse("5-day-old file pruned", oldFile.exists())
        assertTrue("1-day-old file kept", recentFile.exists())
    }

    @Test
    fun `exportZip zips every log file`() {
        val files = tmp.newFolder("files")
        val cache = tmp.newFolder("cache")
        val logsDir = File(files, "logs").apply { mkdirs() }
        // Dates relative to NOW so retention (prune > 3 days old) keeps both —
        // hardcoded calendar dates break once the wall clock passes them.
        val today = dayFormat.format(Instant.now())
        val yesterday = dayFormat.format(Instant.ofEpochMilli(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)))
        File(logsDir, "huntkill-$today.log").writeText("a\n")
        File(logsDir, "huntkill-$yesterday.log").writeText("b\n")

        val writer = FileLogWriter(context(files, cache))
        val zip = writer.exportZip()

        assertTrue(zip.exists())
        assertTrue(zip.name.endsWith(".zip"))
        val names = ZipFile(zip).use { z -> z.entries().toList().map { it.name }.sorted() }
        assertEquals(listOf("huntkill-$yesterday.log", "huntkill-$today.log").sorted(), names)
    }

    private fun waitUntil(timeoutMs: Long = 3_000, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(10)
        }
    }
}

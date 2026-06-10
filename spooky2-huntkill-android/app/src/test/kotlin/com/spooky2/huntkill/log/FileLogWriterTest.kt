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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

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

        val today = dayFormat.format(Date())
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
        val old = dayFormat.format(Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)))
        val recent = dayFormat.format(Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)))
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
        File(logsDir, "huntkill-2026-06-08.log").writeText("a\n")
        File(logsDir, "huntkill-2026-06-09.log").writeText("b\n")

        val writer = FileLogWriter(context(files, cache))
        val zip = writer.exportZip()

        assertTrue(zip.exists())
        assertTrue(zip.name.endsWith(".zip"))
        val names = ZipFile(zip).use { z -> z.entries().toList().map { it.name }.sorted() }
        assertEquals(listOf("huntkill-2026-06-08.log", "huntkill-2026-06-09.log"), names)
    }

    private fun waitUntil(timeoutMs: Long = 3_000, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(10)
        }
    }
}

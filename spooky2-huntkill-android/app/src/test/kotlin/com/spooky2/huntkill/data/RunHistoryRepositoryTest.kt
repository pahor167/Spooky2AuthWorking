package com.spooky2.huntkill.data

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/**
 * JVM round-trip tests for [RunHistoryRepository]: save/all/get/delete against a real
 * temp filesDir (Context.filesDir mocked like [com.spooky2.huntkill.log.FileLogWriter]),
 * newest-first ordering, and tolerance of a missing/corrupt file.
 */
class RunHistoryRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun context(filesDir: File): Context = mockk<Context>().also {
        every { it.filesDir } returns filesDir
    }

    private fun record(id: String, timestampMs: Long) = RunRecord(
        id = id,
        timestampMs = timestampMs,
        generatorLabel = "S/N r91",
        startFrequency = 41000.0,
        endFrequency = 1_800_000.0,
        dwellSeconds = 180.0,
        targetAmplitudeCv = 2000,
        hits = listOf(
            RunHit(frequency = 123456.0, deviation = 42.5),
            RunHit(frequency = 654321.0, deviation = 17.25),
        ),
    )

    @Test
    fun `save two records and all returns them newest first`() = runBlocking {
        val files = tmp.newFolder("files")
        val repo = RunHistoryRepository(context(files))

        val older = repo.save(record("a", timestampMs = 1_000))
        val newer = repo.save(record("b", timestampMs = 2_000))

        val all = repo.all()
        assertEquals(2, all.size)
        assertEquals(newer.id, all[0].id)
        assertEquals(older.id, all[1].id)
        // Field round-trip survives serialization.
        assertEquals("S/N r91", all[0].generatorLabel)
        assertEquals(2, all[0].hits.size)
        assertEquals(123456.0, all[1].hits[0].frequency, 1e-9)
        assertEquals(42.5, all[1].hits[0].deviation, 1e-9)
        assertEquals(2000, all[0].targetAmplitudeCv)
    }

    @Test
    fun `blank id is replaced with a generated id`() = runBlocking {
        val files = tmp.newFolder("files")
        val repo = RunHistoryRepository(context(files))

        val saved = repo.save(record("", timestampMs = 1_000))
        assertTrue("id was generated", saved.id.isNotBlank())
        assertEquals(saved.id, repo.all().single().id)
    }

    @Test
    fun `get and delete operate by id`() = runBlocking {
        val files = tmp.newFolder("files")
        val repo = RunHistoryRepository(context(files))
        repo.save(record("a", timestampMs = 1_000))
        repo.save(record("b", timestampMs = 2_000))

        assertEquals("b", repo.get("b")?.id)
        assertNull(repo.get("missing"))

        repo.delete("a")
        assertNull(repo.get("a"))
        assertEquals(listOf("b"), repo.all().map { it.id })
    }

    @Test
    fun `missing file returns empty`() = runBlocking {
        val files = tmp.newFolder("files")
        val repo = RunHistoryRepository(context(files))
        assertTrue(repo.all().isEmpty())
        assertNull(repo.get("any"))
    }

    @Test
    fun `corrupt file returns empty`() = runBlocking {
        val files = tmp.newFolder("files")
        File(files, "history").apply { mkdirs() }
        File(File(files, "history"), "runs.json").writeText("{ this is not valid json ][")

        val repo = RunHistoryRepository(context(files))
        assertTrue(repo.all().isEmpty())
    }

    /**
     * When the history directory is replaced by a plain file, mkdirs() fails and the
     * write cannot proceed. [save] must throw [IOException] rather than silently swallow
     * the failure and return the record as if it were persisted.
     */
    @Test
    fun `save throws IOException when write is impossible`() = runBlocking {
        val files = tmp.newFolder("files")
        // Place a *file* where the history directory would go so mkdirs() fails.
        File(files, "history").createNewFile()
        val repo = RunHistoryRepository(context(files))

        try {
            repo.save(record("x", timestampMs = 1_000))
            fail("Expected IOException but save() returned normally")
        } catch (e: IOException) {
            // expected — write could not proceed
        }
        Unit
    }

    /**
     * When a write succeeds the old content must survive process-death mid-write.
     * After one successful save, the runs.json must contain the record; no orphaned
     * .tmp file should remain once the call returns.
     */
    @Test
    fun `atomic write leaves no temp file on success`() = runBlocking {
        val files = tmp.newFolder("files")
        val repo = RunHistoryRepository(context(files))
        repo.save(record("a", timestampMs = 1_000))

        val historyDir = File(files, "history")
        val tmpFile = File(historyDir, "runs.json.tmp")
        assertTrue("runs.json exists after save", File(historyDir, "runs.json").exists())
        assertTrue("no .tmp file left behind", !tmpFile.exists())
    }
}

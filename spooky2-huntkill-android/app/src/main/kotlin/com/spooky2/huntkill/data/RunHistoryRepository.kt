package com.spooky2.huntkill.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** One frequency found in a completed hunt, persisted in a [RunRecord]. */
data class RunHit(
    val frequency: Double,
    val deviation: Double,
)

/**
 * A persisted record of one completed hunt: its final frequencies plus the
 * parameters/generator that produced them. Saved as soon as the final hits are
 * known so found frequencies survive even if the user cancels the kill.
 */
data class RunRecord(
    val id: String,
    val timestampMs: Long,
    val generatorLabel: String?,
    val startFrequency: Double,
    val endFrequency: Double,
    val dwellSeconds: Double,
    val targetAmplitudeCv: Int,
    val hits: List<RunHit>,
)

/**
 * Persists completed-hunt results to `filesDir/history/runs.json` using Android's
 * built-in `org.json` (no extra Gradle plugins). The whole list is read/written as a
 * single JSON array — runs are small (a handful of frequencies each) and the file is
 * only touched at save/list/delete time, so a single-file store keeps it simple.
 *
 * All disk I/O runs on [Dispatchers.IO]. Reads tolerate a missing or corrupt file by
 * returning an empty list. Writes are serialized through a [Mutex] so concurrent
 * save/delete calls don't clobber each other's read-modify-write.
 */
@Singleton
class RunHistoryRepository @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val historyDir: File = File(context.filesDir, "history")
    private val runsFile: File = File(historyDir, "runs.json")

    /** Serializes read-modify-write so save/delete never lose a concurrent change. */
    private val writeMutex = Mutex()

    /** All saved runs, newest first. Missing/corrupt file → empty list. */
    suspend fun all(): List<RunRecord> = withContext(Dispatchers.IO) {
        readAll().sortedByDescending { it.timestampMs }
    }

    /** The run with [id], or null if not found (or the file is missing/corrupt). */
    suspend fun get(id: String): RunRecord? = withContext(Dispatchers.IO) {
        readAll().firstOrNull { it.id == id }
    }

    /**
     * Persist [record], appending it to the store. A blank [RunRecord.id] is replaced
     * with a fresh UUID. Returns the saved record (with its final id).
     */
    suspend fun save(record: RunRecord): RunRecord = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val withId = if (record.id.isBlank()) record.copy(id = UUID.randomUUID().toString()) else record
            val current = readAll().filterNot { it.id == withId.id }
            writeAll(current + withId)
            withId
        }
    }

    /** Remove the run with [id]. No-op when it isn't present. */
    suspend fun delete(id: String): Unit = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val remaining = readAll().filterNot { it.id == id }
            writeAll(remaining)
        }
    }

    private fun readAll(): List<RunRecord> {
        if (!runsFile.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(runsFile.readText())
            (0 until array.length()).mapNotNull { i ->
                runCatching { recordFromJson(array.getJSONObject(i)) }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    private fun writeAll(records: List<RunRecord>) {
        runCatching {
            historyDir.mkdirs()
            val array = JSONArray()
            records.forEach { array.put(recordToJson(it)) }
            runsFile.writeText(array.toString())
        }
    }

    private fun recordToJson(record: RunRecord): JSONObject {
        val hits = JSONArray()
        record.hits.forEach { hit ->
            hits.put(
                JSONObject()
                    .put(KEY_FREQUENCY, hit.frequency)
                    .put(KEY_DEVIATION, hit.deviation),
            )
        }
        return JSONObject()
            .put(KEY_ID, record.id)
            .put(KEY_TIMESTAMP, record.timestampMs)
            // org.json treats a Kotlin null as JSONObject.NULL via put(String, Any?)
            // only when wrapped; use putOpt so a null label is simply omitted.
            .putOpt(KEY_GENERATOR_LABEL, record.generatorLabel)
            .put(KEY_START_FREQUENCY, record.startFrequency)
            .put(KEY_END_FREQUENCY, record.endFrequency)
            .put(KEY_DWELL_SECONDS, record.dwellSeconds)
            .put(KEY_TARGET_AMPLITUDE_CV, record.targetAmplitudeCv)
            .put(KEY_HITS, hits)
    }

    private fun recordFromJson(json: JSONObject): RunRecord {
        val hitsArray = json.optJSONArray(KEY_HITS) ?: JSONArray()
        val hits = (0 until hitsArray.length()).map { i ->
            val hit = hitsArray.getJSONObject(i)
            RunHit(
                frequency = hit.optDouble(KEY_FREQUENCY, 0.0),
                deviation = hit.optDouble(KEY_DEVIATION, 0.0),
            )
        }
        return RunRecord(
            id = json.getString(KEY_ID),
            timestampMs = json.getLong(KEY_TIMESTAMP),
            generatorLabel = if (json.has(KEY_GENERATOR_LABEL) && !json.isNull(KEY_GENERATOR_LABEL)) {
                json.optString(KEY_GENERATOR_LABEL, "")
            } else {
                null
            },
            startFrequency = json.optDouble(KEY_START_FREQUENCY, 0.0),
            endFrequency = json.optDouble(KEY_END_FREQUENCY, 0.0),
            dwellSeconds = json.optDouble(KEY_DWELL_SECONDS, 0.0),
            targetAmplitudeCv = json.optInt(KEY_TARGET_AMPLITUDE_CV, 0),
            hits = hits,
        )
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_TIMESTAMP = "timestampMs"
        private const val KEY_GENERATOR_LABEL = "generatorLabel"
        private const val KEY_START_FREQUENCY = "startFrequency"
        private const val KEY_END_FREQUENCY = "endFrequency"
        private const val KEY_DWELL_SECONDS = "dwellSeconds"
        private const val KEY_TARGET_AMPLITUDE_CV = "targetAmplitudeCv"
        private const val KEY_HITS = "hits"
        private const val KEY_FREQUENCY = "frequency"
        private const val KEY_DEVIATION = "deviation"
    }
}

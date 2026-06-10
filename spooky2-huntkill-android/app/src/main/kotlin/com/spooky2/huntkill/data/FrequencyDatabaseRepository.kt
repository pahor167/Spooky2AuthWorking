package com.spooky2.huntkill.data

import android.content.Context
import com.spooky2.huntkill.core.lookup.FrequencyDatabase
import com.spooky2.huntkill.core.lookup.FrequencyDatabaseParser
import com.spooky2.huntkill.log.LogBus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source of the parsed reverse-lookup frequency database. An interface so the
 * ViewModel can be unit-tested with an in-memory fake (the production impl reads an
 * Android asset).
 */
interface FrequencyDatabaseSource {
    suspend fun database(): FrequencyDatabase
}

/**
 * App-wide, lazily-loaded reverse-lookup frequency database.
 *
 * The bundled database ships gzipped as `assets/frequencies.csv.gz` (~575 KB). On first
 * use it is gunzipped and parsed once, off the main thread, then cached for the process
 * lifetime. Load time and entry count are logged to [LogBus] so they land in the daily
 * log files / ZIP export.
 *
 * Loading is guarded by a [Mutex] so concurrent callers (e.g. several hits looking up at
 * once) share a single parse instead of racing.
 */
@Singleton
class FrequencyDatabaseRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val log: LogBus,
) : FrequencyDatabaseSource {

    private val loadMutex = Mutex()

    @Volatile
    private var cached: FrequencyDatabase? = null

    /**
     * Return the parsed database, loading it on [Dispatchers.IO] on first call. Safe to
     * call from any dispatcher and from multiple coroutines concurrently.
     */
    override suspend fun database(): FrequencyDatabase {
        cached?.let { return it }
        return loadMutex.withLock {
            cached ?: load().also { cached = it }
        }
    }

    private suspend fun load(): FrequencyDatabase = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        val (assetName, gzipped) = resolveAsset()
        val db = context.assets.open(assetName).use { raw ->
            val stream = if (gzipped) GZIPInputStream(raw) else raw
            stream.use { input ->
                InputStreamReader(input, Charsets.UTF_8).use { reader ->
                    FrequencyDatabaseParser.parse(reader)
                }
            }
        }
        val elapsedMs = System.currentTimeMillis() - startMs
        log.i(
            TAG,
            "Loaded frequency database from '$assetName': ${db.size} entries " +
                "(${db.skippedLines} skipped, ${db.declaredCount} declared) in ${elapsedMs}ms",
        )
        db
    }

    /**
     * Pick the asset to read. The database is committed gzipped as [GZ_ASSET], but the
     * Android asset packager transparently gunzips `*.gz` assets at build time, serving
     * them under the stripped name. So prefer the still-gzipped file when present and
     * fall back to the build-time-decompressed plain CSV otherwise.
     */
    private fun resolveAsset(): Pair<String, Boolean> {
        val assets = context.assets.list("")?.toSet().orEmpty()
        return when {
            GZ_ASSET in assets -> GZ_ASSET to true
            PLAIN_ASSET in assets -> PLAIN_ASSET to false
            // Last resort: attempt the gzipped name (open() will surface a clear error).
            else -> GZ_ASSET to true
        }
    }

    companion object {
        private const val GZ_ASSET = "frequencies.csv.gz"
        private const val PLAIN_ASSET = "frequencies.csv"
        private const val TAG = "FreqDb"
    }
}

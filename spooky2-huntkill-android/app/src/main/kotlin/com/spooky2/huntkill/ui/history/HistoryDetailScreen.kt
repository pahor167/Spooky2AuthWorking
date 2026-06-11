package com.spooky2.huntkill.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.core.lookup.LookupMatch
import com.spooky2.huntkill.data.RunHit
import com.spooky2.huntkill.data.RunRecord
import com.spooky2.huntkill.ui.common.asHz

/** Per-frequency matches shown before the list is truncated. */
private const val DETAIL_MATCHES_SHOWN = 5

/**
 * Detail view for one saved run: its metadata (date, generator, params), the list of
 * saved frequencies (formatted, with deviation and reverse-lookup matches), and a
 * prominent "Re-run treatment" button.
 *
 * [onReRun] is given the run's frequencies + dwell + amplitude; the host decides whether
 * to drive the Kill screen (connected) or route to Connect (not connected).
 */
@Composable
fun HistoryDetailScreen(
    runId: String,
    viewModel: HistoryViewModel,
    onReRun: (RunRecord) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(runId) { viewModel.loadDetail(runId) }

    val run = state.selected
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (run == null || run.id != runId) {
            Text("Run not found.", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    formatRunTimestamp(run.timestampMs),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            item { MetadataCard(run) }
            item {
                Text(
                    "Frequencies (${run.hits.size})",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            itemsIndexed(run.hits) { index, hit ->
                FrequencyRow(
                    index = index,
                    hit = hit,
                    matches = state.lookupResults[hit.frequency],
                    busy = state.lookupBusy,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onReRun(run) },
            modifier = Modifier.fillMaxWidth(),
            enabled = run.hits.isNotEmpty(),
        ) {
            Text("Re-run treatment", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MetadataCard(run: RunRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Generator: ${run.generatorLabel ?: "Unknown"}")
            Text("Sweep: ${run.startFrequency.asHz()} – ${run.endFrequency.asHz()}")
            Text("Dwell: ${"%.0f".format(run.dwellSeconds)} s per frequency")
            Text("Amplitude: ${run.targetAmplitudeCv} CV")
        }
    }
}

@Composable
private fun FrequencyRow(
    index: Int,
    hit: RunHit,
    matches: List<LookupMatch>?,
    busy: Boolean,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "${index + 1}. ${hit.frequency.asHz()}",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text("Deviation: ${"%.2f".format(hit.deviation)}")
            Spacer(Modifier.height(6.dp))
            DetailMatches(matches, busy)
        }
    }
}

/** Reverse-lookup matches for a saved frequency: loading / no-matches / top-N list. */
@Composable
private fun DetailMatches(matches: List<LookupMatch>?, busy: Boolean) {
    when {
        matches == null -> Text(
            if (busy) "Looking up matches…" else "Matches pending…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        matches.isEmpty() -> Text(
            "No matches",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            matches.take(DETAIL_MATCHES_SHOWN).forEach { match ->
                Text(match.toReportLine(), style = MaterialTheme.typography.bodySmall)
            }
            if (matches.size > DETAIL_MATCHES_SHOWN) {
                Text(
                    "+${matches.size - DETAIL_MATCHES_SHOWN} more",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

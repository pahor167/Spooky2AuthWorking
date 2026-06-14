package com.spooky2.huntkill.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.core.lookup.LookupMatch
import com.spooky2.huntkill.ui.common.ConditionTags
import com.spooky2.huntkill.data.RunHit
import com.spooky2.huntkill.data.RunRecord
import com.spooky2.huntkill.ui.common.asHz
import com.spooky2.huntkill.ui.theme.MonoNumberSmall
import com.spooky2.huntkill.ui.theme.SLOutline
import com.spooky2.huntkill.ui.theme.SLPrimary
import com.spooky2.huntkill.ui.theme.SectionLabel

private const val DETAIL_MATCHES_SHOWN = 5

/**
 * Detail view for one saved run: metadata, frequencies, Re-run button.
 */
@Composable
fun HistoryDetailScreen(
    runId: String,
    viewModel: HistoryViewModel,
    onReRun: (RunRecord) -> Unit,
    onBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(runId) { viewModel.loadDetail(runId) }

    val run = state.selected
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (run == null || run.id != runId) {
            Text("Run not found.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick  = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(8.dp),
            ) {
                Text("Back to history", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            return@Column
        }

        LazyColumn(
            modifier            = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    formatRunTimestamp(run.timestampMs),
                    style = MonoNumberSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { MetadataCard(run) }
            item {
                Text(
                    "FREQUENCIES (${run.hits.size})",
                    style = SectionLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            itemsIndexed(run.hits) { index, hit ->
                FrequencyRow(
                    index   = index,
                    hit     = hit,
                    matches = state.lookupResults[hit.frequency],
                    busy    = state.lookupBusy,
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick  = { onReRun(run) },
            modifier = Modifier.fillMaxWidth(),
            enabled  = run.hits.isNotEmpty(),
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = SLPrimary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("Re-run treatment", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MetadataCard(run: RunRecord) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border    = BorderStroke(1.dp, SLOutline),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GENERATOR", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(run.generatorLabel ?: "Unknown", style = MonoNumberSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SWEEP", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${run.startFrequency.asHz()} – ${run.endFrequency.asHz()}",
                    style = MonoNumberSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DWELL", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${"%.0f".format(run.dwellSeconds)} s", style = MonoNumberSmall, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AMP", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${run.targetAmplitudeCv} CV", style = MonoNumberSmall, color = MaterialTheme.colorScheme.onSurface)
            }
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
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border    = BorderStroke(1.dp, SLOutline),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    "${index + 1}. ${hit.frequency.asHz()}",
                    style    = MonoNumberSmall,
                    color    = SLPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(6.dp))
                ConditionTags(matches = matches)
            }
            Text(
                "dev ${"%.2f".format(hit.deviation)}",
                style = MonoNumberSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            DetailMatches(matches, busy)
        }
    }
}

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
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

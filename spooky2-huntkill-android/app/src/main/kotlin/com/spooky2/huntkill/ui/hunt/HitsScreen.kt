package com.spooky2.huntkill.ui.hunt

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.core.lookup.LookupMatch
import com.spooky2.huntkill.ui.common.DisclaimerBanner
import com.spooky2.huntkill.ui.common.asHz
import kotlin.math.roundToInt

/** Matches shown per hit before the "show all" expander appears. */
private const val COLLAPSED_MATCHES = 5

@Composable
fun HitsScreen(
    viewModel: HuntViewModel,
    onRunAgain: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val dwellSeconds = state.params.dwellSecondsText.toDoubleOrNull() ?: 0.0
    val totalMinutes = (state.hits.size * dwellSeconds / 60.0).roundToInt()

    // Per-hit "show all matches" toggle, keyed by hit frequency. Local UI state only.
    val expanded = remember { mutableStateMapOf<Double, Boolean>() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Hunt complete", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${state.hits.size} hits · treated for ~$totalMinutes min total",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (state.hits.isEmpty()) {
            Text("No resonant frequencies were detected this run.")
        } else {
            ToleranceSelector(
                selected = state.lookupTolerancePercent,
                busy = state.lookupBusy,
                onSelect = viewModel::setLookupTolerance,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.hits) { index, hit ->
                val matches = state.lookupResults[hit.frequency]
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
                        Text("Reading: ${"%.1f".format(hit.reading)}")

                        Spacer(Modifier.height(6.dp))
                        MatchList(
                            matches = matches,
                            busy = state.lookupBusy,
                            isExpanded = expanded[hit.frequency] == true,
                            onToggleExpanded = {
                                expanded[hit.frequency] = !(expanded[hit.frequency] ?: false)
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        val isBusy = state.busyAction != null
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onRunAgain,
                enabled = !isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text("Run again", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = {
                    viewModel.disconnect()
                    onDisconnect()
                },
                enabled = !isBusy,
                modifier = Modifier.weight(1f),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.size(8.dp))
                }
                Text("Disconnect", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.height(8.dp))
        DisclaimerBanner()
    }
}

/** Tolerance preset chips (0.1 / 0.25 / 0.5 / 1.0 %) that re-run the lookup on tap. */
@Composable
private fun ToleranceSelector(
    selected: Double,
    busy: Boolean,
    onSelect: (Double) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Match tolerance",
                style = MaterialTheme.typography.labelLarge,
            )
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LOOKUP_TOLERANCE_OPTIONS.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    enabled = !busy,
                    label = { Text(formatPercent(option)) },
                )
            }
        }
    }
}

/** Per-hit reverse-lookup matches with a collapse/"show all" expander. */
@Composable
private fun MatchList(
    matches: List<LookupMatch>?,
    busy: Boolean,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    when {
        matches == null -> {
            Text(
                if (busy) "Looking up matches…" else "Matches pending…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        matches.isEmpty() -> {
            Text(
                "No matches",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> {
            val shown = if (isExpanded) matches else matches.take(COLLAPSED_MATCHES)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                shown.forEach { match ->
                    Text(
                        match.toReportLine(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (matches.size > COLLAPSED_MATCHES) {
                    TextButton(
                        onClick = onToggleExpanded,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Text(
                            if (isExpanded) "show less" else "show all (${matches.size})",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

/** Format a tolerance percent for a chip label, dropping a trailing `.0`. */
private fun formatPercent(value: Double): String {
    val text = if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    return "$text%"
}

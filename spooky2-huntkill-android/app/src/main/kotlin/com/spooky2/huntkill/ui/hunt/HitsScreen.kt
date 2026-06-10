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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    onKilling: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    // Re-scan / Continue-anyway transition the run into the kill phase; hand off to
    // the Kill screen the moment that happens.
    LaunchedEffect(state.phase) {
        if (state.phase == HuntPhase.Killing) onKilling()
    }
    val dwellSeconds = state.params.dwellSecondsText.toDoubleOrNull() ?: 0.0
    val totalMinutes = (state.hits.size * dwellSeconds / 60.0).roundToInt()
    val hasDropouts = state.phase == HuntPhase.HitsReadyWithDropouts

    // Per-hit "show all matches" toggle, keyed by hit frequency. Local UI state only.
    val expanded = remember { mutableStateMapOf<Double, Boolean>() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (hasDropouts) "Review needed" else "Hunt complete",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            "${state.hits.size} hits · treated for ~$totalMinutes min total",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (hasDropouts) {
            DropoutWarningCard(
                state = state,
                onRescan = viewModel::rescanAffectedSegments,
                onContinueAnyway = viewModel::continueAnyway,
            )
        }

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
        // While dropouts are pending the user must resolve them via the warning card
        // (Re-scan / Continue anyway); the run-again / disconnect controls return
        // once the run has truly completed.
        if (!hasDropouts) {
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
        }

        Spacer(Modifier.height(8.dp))
        DisclaimerBanner()
    }
}

/**
 * Warning card shown when the sweep detected cable dropouts. Summarizes how many
 * segments were affected (and the rough share of the sweep), surfaces any prior
 * re-scan error, and offers "Re-scan affected segments" vs "Continue anyway".
 */
@Composable
private fun DropoutWarningCard(
    state: HuntUiState,
    onRescan: () -> Unit,
    onContinueAnyway: () -> Unit,
) {
    val totalSteps = state.totalSweepSteps.takeIf { it > 0 } ?: state.historyValid.size
    val flagged = state.historyValid.count { !it }
    val pct = if (totalSteps > 0) flagged * 100.0 / totalSteps else 0.0
    val busy = state.rescanInProgress

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Connection dropped during ${state.dropoutSegments.size} segment(s) " +
                    "(~${"%.0f".format(pct)}% of sweep)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                "Those readings were excluded from detection. Re-scan the affected " +
                    "bands to recover anything hidden in the dropout, or continue with " +
                    "the hits found so far.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            state.errorMessage?.let { msg ->
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onRescan,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(8.dp))
                    }
                    Text("Re-scan affected segments", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            OutlinedButton(
                onClick = onContinueAnyway,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue anyway", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
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

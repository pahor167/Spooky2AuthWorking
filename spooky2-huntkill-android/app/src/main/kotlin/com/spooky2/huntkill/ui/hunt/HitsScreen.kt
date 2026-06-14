package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.core.lookup.LookupMatch
import com.spooky2.huntkill.ui.common.ConditionTags
import com.spooky2.huntkill.ui.common.DisclaimerBanner
import com.spooky2.huntkill.ui.common.asHz
import com.spooky2.huntkill.ui.theme.MonoNumberSmall
import com.spooky2.huntkill.ui.theme.SLActive
import com.spooky2.huntkill.ui.theme.SLActiveContainer
import com.spooky2.huntkill.ui.theme.SLError
import com.spooky2.huntkill.ui.theme.SLOnActiveContainer
import com.spooky2.huntkill.ui.theme.SLPrimary
import com.spooky2.huntkill.ui.theme.SectionLabel
import kotlin.math.roundToInt

/** Matches shown per hit before the "show all" expander appears. */
internal const val COLLAPSED_MATCHES = 5

@Composable
fun HitsScreen(
    viewModel: HuntViewModel,
    onRunAgain: () -> Unit,
    onDisconnect: () -> Unit,
    onKilling: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val activeIndex by viewModel.activeIndex.collectAsState()

    LaunchedEffect(state.phase) {
        if (state.phase == HuntPhase.Killing) onKilling()
    }
    val dwellSeconds  = state.params.dwellSecondsText.toDoubleOrNull() ?: 0.0
    val totalMinutes  = (state.hits.size * dwellSeconds / 60.0).roundToInt()
    val hasDropouts   = state.phase == HuntPhase.HitsReadyWithDropouts
    val expanded      = remember { mutableStateMapOf<Double, Boolean>() }
    val compactView   = state.hitsCompactView
    // Flipping the view mode standardizes every row: clear per-row "show all" state.
    LaunchedEffect(compactView) { expanded.clear() }
    var selectedMarker by remember { mutableStateOf<GraphMarker?>(null) }
    selectedMarker?.let { marker ->
        MarkerDetailSheet(marker = marker, viewModel = viewModel, onDismiss = { selectedMarker = null })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LazyColumn(
            modifier            = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                GeneratorTabs(tabs = tabs, activeIndex = activeIndex, onSelect = viewModel::setActiveGenerator)
            }
            item {
                Text(
                    if (hasDropouts) "Review needed" else "Hunt complete",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            item {
                Text(
                    "${state.hits.size} hits · ~$totalMinutes min total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (hasDropouts) {
                item {
                    DropoutWarningCard(
                        state          = state,
                        onRescan       = viewModel::rescanAffectedSegments,
                        onContinueAnyway = viewModel::continueAnyway,
                    )
                }
            }

            // Scan graph compact view
            if (state.fullHistory.isNotEmpty()) {
                item {
                    Text("SCAN GRAPH", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    ScrollableReadingGraph(
                        readings    = state.fullHistory,
                        valid       = state.historyValid,
                        markers     = state.graphMarkers,
                        onMarkerTap = { selectedMarker = it },
                        modifier    = Modifier.fillMaxWidth().height(110.dp),
                    )
                }
            }

            if (state.hits.isEmpty()) {
                item { Text("No resonant frequencies were detected this run.") }
            } else if (!compactView) {
                item {
                    ToleranceSelector(
                        selected = state.lookupTolerancePercent,
                        busy     = state.lookupBusy,
                        onSelect = viewModel::setLookupTolerance,
                    )
                }
            }

            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("CANDIDATES (${state.hits.size})", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HitViewModeSwitch(compact = compactView, onChange = viewModel::setHitsCompactView)
                }
            }

            itemsIndexed(state.hits) { index, hit ->
                val matches = state.lookupResults[hit.frequency]
                Card(
                    modifier  = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedMarker = GraphMarker(
                                stepIndex = Int.MAX_VALUE,
                                frequency = hit.frequency,
                                deviation = hit.deviation,
                                isFinal   = true,
                            )
                        },
                    shape  = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        if (!compactView) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "dev ${"%.2f".format(hit.deviation)}",
                                    style = MonoNumberSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "rdg ${"%.1f".format(hit.reading)}",
                                    style = MonoNumberSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            MatchList(
                                matches          = matches,
                                busy             = state.lookupBusy,
                                isExpanded       = expanded[hit.frequency] == true,
                                onToggleExpanded = {
                                    expanded[hit.frequency] = !(expanded[hit.frequency] ?: false)
                                },
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        if (!hasDropouts) {
            val isBusy = state.busyAction != null
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick  = onRunAgain,
                    enabled  = !isBusy,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = SLPrimary,
                        contentColor   = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text("Run again", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick  = { viewModel.disconnect(); onDisconnect() },
                    enabled  = !isBusy,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.size(6.dp))
                    }
                    Text("Disconnect", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Spacer(Modifier.height(2.dp))
        DisclaimerBanner()
    }
}

/**
 * Warning card shown when the sweep detected cable dropouts.
 */
@Composable
private fun DropoutWarningCard(
    state: HuntUiState,
    onRescan: () -> Unit,
    onContinueAnyway: () -> Unit,
) {
    val totalSteps = state.totalSweepSteps.takeIf { it > 0 } ?: state.historyValid.size
    val flagged    = state.historyValid.count { !it }
    val pct        = if (totalSteps > 0) flagged * 100.0 / totalSteps else 0.0
    val busy       = state.rescanInProgress

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        border    = BorderStroke(1.dp, SLError.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Connection dropped — ${state.dropoutSegments.size} segment(s) " +
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
                Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick  = onRescan,
                    enabled  = !busy,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = SLError,
                        contentColor   = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.onError,
                        )
                        Spacer(Modifier.size(6.dp))
                    }
                    Text("Re-scan affected", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            OutlinedButton(
                onClick  = onContinueAnyway,
                enabled  = !busy,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.4f)),
            ) {
                Text("Continue anyway", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Tolerance preset chips: 0.1 / 0.25 / 0.5 / 1.0 % */
@Composable
internal fun ToleranceSelector(
    selected: Double,
    busy: Boolean,
    onSelect: (Double) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("TOLERANCE", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = SLPrimary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LOOKUP_TOLERANCE_OPTIONS.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick  = { onSelect(option) },
                    enabled  = !busy,
                    label    = { Text(formatPercent(option), style = MaterialTheme.typography.labelSmall) },
                    shape    = RoundedCornerShape(6.dp),
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SLActiveContainer,
                        selectedLabelColor     = SLOnActiveContainer,
                    ),
                    border   = FilterChipDefaults.filterChipBorder(
                        enabled             = !busy,
                        selected            = option == selected,
                        borderColor         = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = SLActive.copy(alpha = 0.5f),
                    ),
                )
            }
        }
    }
}

/**
 * Compact/Details switch for the hit list, shared by the Hits and Kill screens.
 * Compact = frequency only; Details = deviation + reverse-lookup matches. Selecting
 * either mode standardizes the view: callers reset per-row expansion on change.
 */
@Composable
internal fun HitViewModeSwitch(
    compact: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(true to "Compact", false to "Details").forEach { (mode, label) ->
            val selected = compact == mode
            FilterChip(
                selected = selected,
                onClick  = { onChange(mode) },
                label    = {
                    Text(
                        label,
                        style    = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                },
                shape  = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SLActiveContainer,
                    selectedLabelColor     = SLOnActiveContainer,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled             = true,
                    selected            = selected,
                    borderColor         = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = SLActive.copy(alpha = 0.5f),
                ),
            )
        }
    }
}

/** Per-hit reverse-lookup matches with collapse/"show all" expander. */
@Composable
internal fun MatchList(
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
                    Text(match.toReportLine(), style = MaterialTheme.typography.bodySmall)
                }
                if (matches.size > COLLAPSED_MATCHES) {
                    TextButton(
                        onClick        = onToggleExpanded,
                        contentPadding = PaddingValues(0.dp),
                        colors         = ButtonDefaults.textButtonColors(contentColor = SLPrimary),
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
internal fun formatPercent(value: Double): String {
    val text = if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    return "$text%"
}

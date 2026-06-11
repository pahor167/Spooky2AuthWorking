package com.spooky2.huntkill.ui.hunt

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.ui.common.DisclaimerBanner
import com.spooky2.huntkill.ui.common.asHz
import com.spooky2.huntkill.ui.common.formatElapsed
import com.spooky2.huntkill.ui.theme.MonoNumberLarge
import com.spooky2.huntkill.ui.theme.MonoNumberSmall
import com.spooky2.huntkill.ui.theme.SLActive
import com.spooky2.huntkill.ui.theme.SLActiveContainer
import com.spooky2.huntkill.ui.theme.SLError
import com.spooky2.huntkill.ui.theme.SLOnActiveContainer
import com.spooky2.huntkill.ui.theme.SLPrimary
import com.spooky2.huntkill.ui.theme.SLSurface
import com.spooky2.huntkill.ui.theme.SectionLabel

@Composable
fun KillScreen(
    viewModel: HuntViewModel,
    onDone: () -> Unit,
    onStopped: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    var showStopConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = state.isRunning) { showStopConfirm = true }
    if (showStopConfirm) {
        ConfirmStopDialog(
            title        = "Stop the treatment?",
            text         = "The kill phase is still running. Stopping zeroes the generator and ends the treatment.",
            confirmLabel = "Stop & zero",
            onConfirm    = { showStopConfirm = false; viewModel.safetyStop() },
            onDismiss    = { showStopConfirm = false },
        )
    }

    LaunchedEffect(state.phase) {
        when (state.phase) {
            HuntPhase.Done                        -> onDone()
            HuntPhase.Cancelled, HuntPhase.Error  -> onStopped()
            else                                  -> Unit
        }
    }

    val remaining  = state.killDwellRemainingSeconds
    val expanded   = remember { mutableStateMapOf<Double, Boolean>() }
    var selectedMarker by remember { mutableStateOf<GraphMarker?>(null) }
    selectedMarker?.let { marker ->
        MarkerDetailSheet(
            marker    = marker,
            viewModel = viewModel,
            onDismiss = { selectedMarker = null },
        )
    }

    val isKilling = state.phase == HuntPhase.Killing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LazyColumn(
            modifier              = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement   = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Kill Phase", style = MaterialTheme.typography.headlineSmall)
            }
            item { PhaseChip(state.phase, state.isPaused) }

            if (state.busyAction != null) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = SLPrimary) }
            }

            item {
                Text(
                    state.statusText,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("ELAPSED", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatElapsed(state.elapsedSeconds), style = MonoNumberSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Current frequency card with dwell countdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "TREATING",
                                style = SectionLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${state.killIndex} / ${state.killTotal}",
                                style = MonoNumberSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            state.currentFrequency.asHz(),
                            style    = MonoNumberLarge,
                            color    = SLPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("DWELL", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${remaining}s", style = MonoNumberSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            item {
                LinearProgressIndicator(
                    progress   = { (state.percentComplete / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier   = Modifier.fillMaxWidth(),
                    color      = SLPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // Hits list
            item {
                Text("HITS (${state.hits.size})", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.hits.isNotEmpty()) {
                item {
                    ToleranceSelector(
                        selected = state.lookupTolerancePercent,
                        busy     = state.lookupBusy,
                        onSelect = viewModel::setLookupTolerance,
                    )
                }
            }

            itemsIndexed(state.hits) { index, hit ->
                val isCurrent = index == state.killIndex - 1
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedMarker = GraphMarker(
                                stepIndex  = Int.MAX_VALUE,
                                frequency  = hit.frequency,
                                deviation  = hit.deviation,
                                isFinal    = true,
                            )
                        },
                    shape  = RoundedCornerShape(10.dp),
                    colors = if (isCurrent) {
                        CardDefaults.cardColors(containerColor = SLActiveContainer)
                    } else {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    },
                    border = if (isCurrent) {
                        BorderStroke(1.dp, SLActive.copy(alpha = 0.6f))
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    },
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "${index + 1}. ${hit.frequency.asHz()}",
                                style      = MonoNumberSmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color      = if (isCurrent) SLOnActiveContainer else MaterialTheme.colorScheme.onSurface,
                                maxLines   = 1,
                                softWrap   = false,
                                overflow   = TextOverflow.Ellipsis,
                                modifier   = Modifier.weight(1f),
                            )
                            if (isKilling && !isCurrent) {
                                TextButton(
                                    onClick = { viewModel.jumpToHit(index) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 6.dp,
                                        vertical   = 0.dp,
                                    ),
                                    colors = ButtonDefaults.textButtonColors(contentColor = SLPrimary),
                                ) {
                                    Text(
                                        "Treat now",
                                        style   = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Text(
                            "dev ${"%.2f".format(hit.deviation)}",
                            style = MonoNumberSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        MatchList(
                            matches          = state.lookupResults[hit.frequency],
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

        val isBusy = state.busyAction != null

        // Repeat toggle
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.repeatKill,
                onClick  = viewModel::toggleRepeatKill,
                label    = {
                    Text(
                        if (state.repeatKill) "Repeat: On" else "Repeat: Off",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style    = MaterialTheme.typography.labelMedium,
                    )
                },
                shape  = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SLActiveContainer,
                    selectedLabelColor     = SLOnActiveContainer,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled              = true,
                    selected             = state.repeatKill,
                    borderColor          = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor  = SLActive.copy(alpha = 0.5f),
                ),
            )
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick  = viewModel::togglePause,
                enabled  = !isBusy,
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = SLPrimary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    if (state.isPaused) "Resume" else "Pause",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(
                onClick  = { showStopConfirm = true },
                enabled  = !isBusy,
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = SLError),
                border   = BorderStroke(1.dp, SLError.copy(alpha = 0.6f)),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = SLError)
                    Spacer(Modifier.size(6.dp))
                }
                Text("Stop & Zero", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.height(2.dp))
        DisclaimerBanner()
    }
}

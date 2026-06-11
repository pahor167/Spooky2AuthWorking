package com.spooky2.huntkill.ui.hunt

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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

@Composable
fun KillScreen(
    viewModel: HuntViewModel,
    onDone: () -> Unit,
    onStopped: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // Leaving a running kill must be confirmed: back (or Stop & Zero) first opens a
    // dialog; only confirming zeroes the generator and returns to config.
    var showStopConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = state.isRunning) { showStopConfirm = true }
    if (showStopConfirm) {
        ConfirmStopDialog(
            title = "Stop the treatment?",
            text = "The kill phase is still running. Stopping zeroes the generator and ends the treatment.",
            confirmLabel = "Stop & zero",
            onConfirm = {
                showStopConfirm = false
                viewModel.safetyStop()
            },
            onDismiss = { showStopConfirm = false },
        )
    }

    LaunchedEffect(state.phase) {
        when (state.phase) {
            HuntPhase.Done -> onDone()
            HuntPhase.Cancelled, HuntPhase.Error -> onStopped()
            else -> Unit
        }
    }

    // Countdown is driven straight from engine progress (1s slices), so pausing
    // freezes it automatically — no local timer that keeps ticking while paused.
    val remaining = state.killDwellRemainingSeconds

    val expanded = remember { mutableStateMapOf<Double, Boolean>() }

    // Tapping a hit row opens the reverse-lookup sheet for that frequency, mirroring
    // the graph marker behaviour on the results screen.
    var selectedMarker by remember { mutableStateOf<GraphMarker?>(null) }
    selectedMarker?.let { marker ->
        MarkerDetailSheet(
            marker = marker,
            viewModel = viewModel,
            onDismiss = { selectedMarker = null },
        )
    }

    // The kill phase is active while the run is Killing; the per-hit "Treat now"
    // button is only meaningful then.
    val isKilling = state.phase == HuntPhase.Killing

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // The header block + hits list live in ONE scrollable LazyColumn so the list
        // gets real, usable height (instead of being crushed between fixed elements).
        // The Pause / Stop buttons + disclaimer stay PINNED below, outside the scroll.
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Kill Phase", style = MaterialTheme.typography.headlineSmall)
            }
            item { PhaseChip(state.phase, state.isPaused) }
            if (state.busyAction != null) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            item {
                Text(
                    state.statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            item {
                Text(
                    "Elapsed: ${formatElapsed(state.elapsedSeconds)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Treating ${state.killIndex} of ${state.killTotal}",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            state.currentFrequency.asHz(),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Dwell remaining: ${remaining}s", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            item {
                LinearProgressIndicator(
                    progress = { (state.percentComplete / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Detected hits being treated. The frequency currently killing (1-based
            // killIndex) is highlighted so the user sees the chosen frequencies.
            // Reverse-lookup matches (computed at sweep end) are shown under each hit.
            item {
                Text("Hits (${state.hits.size})", style = MaterialTheme.typography.titleSmall)
            }
            if (state.hits.isNotEmpty()) {
                item {
                    ToleranceSelector(
                        selected = state.lookupTolerancePercent,
                        busy = state.lookupBusy,
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
                                stepIndex = Int.MAX_VALUE,
                                frequency = hit.frequency,
                                deviation = hit.deviation,
                                isFinal = true,
                            )
                        },
                    colors = if (isCurrent) {
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    } else {
                        CardDefaults.cardColors()
                    },
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "${index + 1}. ${hit.frequency.asHz()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            // "Treat now": jump the kill straight to this frequency and
                            // continue from it. Only during the Kill phase, and hidden for
                            // the row currently being treated (killIndex is 1-based).
                            if (isKilling && !isCurrent) {
                                TextButton(
                                    onClick = { viewModel.jumpToHit(index) },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 8.dp,
                                        vertical = 0.dp,
                                    ),
                                ) {
                                    Text(
                                        "Treat now",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Text(
                            "Deviation: ${"%.2f".format(hit.deviation)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(4.dp))
                        MatchList(
                            matches = state.lookupResults[hit.frequency],
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

        val isBusy = state.busyAction != null
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = viewModel::togglePause,
                enabled = !isBusy,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (state.isPaused) "Resume" else "Pause", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = { showStopConfirm = true },
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
                Text("Stop & Zero", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.height(8.dp))
        DisclaimerBanner()
    }
}

package com.spooky2.huntkill.ui.hunt

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.ui.common.DisclaimerBanner
import com.spooky2.huntkill.ui.common.asHz
import com.spooky2.huntkill.ui.common.formatElapsed

@Composable
fun LiveScanScreen(
    viewModel: HuntViewModel,
    onKilling: () -> Unit,
    onDone: () -> Unit,
    onCancelled: () -> Unit,
    onDropouts: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // Leaving a running scan must be confirmed: back (or Cancel) first opens a
    // dialog; only confirming zeroes the generator and returns to config.
    var showStopConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = state.isRunning) { showStopConfirm = true }
    if (showStopConfirm) {
        ConfirmStopDialog(
            title = "Stop the scan?",
            text = "The hunt is still running. Stopping zeroes the generator and discards this sweep.",
            confirmLabel = "Stop & zero",
            onConfirm = {
                showStopConfirm = false
                viewModel.cancel()
            },
            onDismiss = { showStopConfirm = false },
        )
    }

    LaunchedEffect(state.phase) {
        when (state.phase) {
            // The engine auto-chains kill after the sweep, so the moment hits are found
            // and the kill begins we jump straight to the Kill screen — no extra tap.
            HuntPhase.HitsReady, HuntPhase.Killing -> onKilling()
            // Dropouts detected: stop on the Hits screen so the user can choose to
            // re-scan the affected segments or continue anyway.
            HuntPhase.HitsReadyWithDropouts -> onDropouts()
            // Done with no kill (e.g. zero hits) -> show the post-run summary.
            HuntPhase.Done -> onDone()
            HuntPhase.Cancelled -> onCancelled()
            else -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Live Scan", style = MaterialTheme.typography.headlineSmall)
        PhaseChip(state.phase, state.isPaused)
        if (state.busyAction != null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Text(
            state.statusText,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )

        // Bigger frequency readout — the headline number the user watches.
        Text(state.currentFrequency.asHz(), style = MaterialTheme.typography.headlineMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "${"%.0f".format(state.percentComplete)}% complete",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                if (state.estimatedRemainingSeconds > 0) {
                    "~${formatElapsed(state.estimatedRemainingSeconds)} left"
                } else {
                    "estimating…"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text("Elapsed: ${formatElapsed(state.elapsedSeconds)}", style = MaterialTheme.typography.bodyMedium)

        LinearProgressIndicator(
            progress = { (state.percentComplete / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Amplitude CV: ${state.amplitudeCv}")
        Text("Latest reading (angle): ${"%.1f".format(state.currentReading)}")
        Text("Running average: ${"%.1f".format(state.runningAverage)}")

        Text("Angle readings", style = MaterialTheme.typography.titleSmall)
        // Marker tapped on the graph -> show its detail sheet (frequency + matches).
        var selectedMarker by remember { mutableStateOf<GraphMarker?>(null) }
        if (state.fullHistory.isNotEmpty()) {
            // Live + post-sweep: horizontally scrollable history with dropout tints and
            // clickable hit-frequency markers (provisional during the sweep, final after).
            ScrollableReadingGraph(
                readings = state.fullHistory,
                valid = state.historyValid,
                markers = state.graphMarkers,
                onMarkerTap = { selectedMarker = it },
                modifier = Modifier.fillMaxWidth().height(160.dp),
            )
        } else {
            // Early tail before any step readings arrive.
            AngleGraph(
                readings = state.angleHistory,
                modifier = Modifier.fillMaxWidth().height(160.dp),
            )
        }
        selectedMarker?.let { marker ->
            MarkerDetailSheet(
                marker = marker,
                viewModel = viewModel,
                onDismiss = { selectedMarker = null },
            )
        }

        Spacer(Modifier.height(8.dp))
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
                Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.height(8.dp))
        DisclaimerBanner()
    }
}

/** Colored status chip: Hunting=primary, Killing=error, Paused=tertiary. */
@Composable
internal fun PhaseChip(phase: HuntPhase, isPaused: Boolean) {
    val (label, color) = when {
        isPaused -> "Paused" to MaterialTheme.colorScheme.tertiaryContainer
        phase == HuntPhase.Killing -> "Killing" to MaterialTheme.colorScheme.errorContainer
        phase == HuntPhase.Hunting -> "Hunting" to MaterialTheme.colorScheme.primaryContainer
        else -> phase.name to MaterialTheme.colorScheme.surfaceVariant
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = color,
            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun AngleGraph(readings: List<Double>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (readings.size < 2) return@Canvas
        val minV = readings.min()
        val maxV = readings.max()
        val range = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
        val stepX = size.width / (readings.size - 1)

        var prev = Offset(
            x = 0f,
            y = (size.height * (1.0 - (readings[0] - minV) / range)).toFloat(),
        )
        for (i in 1 until readings.size) {
            val next = Offset(
                x = stepX * i,
                y = (size.height * (1.0 - (readings[i] - minV) / range)).toFloat(),
            )
            drawLine(color = lineColor, start = prev, end = next, strokeWidth = 2f)
            prev = next
        }
    }
}

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
) {
    val state by viewModel.state.collectAsState()

    // System back while running = Cancel (zero + back to config), never a silent run.
    BackHandler(enabled = state.isRunning) { viewModel.cancel() }

    LaunchedEffect(state.phase) {
        when (state.phase) {
            // The engine auto-chains kill after the sweep, so the moment hits are found
            // and the kill begins we jump straight to the Kill screen — no extra tap.
            HuntPhase.HitsReady, HuntPhase.Killing -> onKilling()
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
        AngleGraph(
            readings = state.angleHistory,
            modifier = Modifier.fillMaxWidth().height(160.dp),
        )

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::togglePause, modifier = Modifier.weight(1f)) {
                Text(if (state.isPaused) "Resume" else "Pause")
            }
            OutlinedButton(onClick = viewModel::cancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
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

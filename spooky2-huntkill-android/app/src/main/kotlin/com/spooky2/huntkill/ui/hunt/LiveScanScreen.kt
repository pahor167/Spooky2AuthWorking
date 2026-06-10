package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.ui.common.DisclaimerBanner

@Composable
fun LiveScanScreen(
    viewModel: HuntViewModel,
    onHitsReady: () -> Unit,
    onCancelled: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.phase) {
        when (state.phase) {
            HuntPhase.HitsReady, HuntPhase.Killing, HuntPhase.Done -> onHitsReady()
            HuntPhase.Cancelled -> onCancelled()
            else -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Live Scan", style = MaterialTheme.typography.headlineSmall)
        Text(state.statusText, style = MaterialTheme.typography.bodyMedium)

        LinearProgressIndicator(
            progress = { (state.percentComplete / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Phase: ${state.phase}")
        Text("Frequency: ${"%.2f".format(state.currentFrequency)} Hz")
        Text("Amplitude CV: ${state.amplitudeCv}")
        Text("Latest reading (angle): ${"%.1f".format(state.currentReading)}")
        Text("Running average: ${"%.1f".format(state.runningAverage)}")

        Text("Angle readings", style = MaterialTheme.typography.titleSmall)
        AngleGraph(
            readings = state.angleHistory,
            modifier = Modifier.fillMaxWidth().height(160.dp),
        )

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = viewModel::cancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }

        Spacer(Modifier.height(8.dp))
        DisclaimerBanner()
    }
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

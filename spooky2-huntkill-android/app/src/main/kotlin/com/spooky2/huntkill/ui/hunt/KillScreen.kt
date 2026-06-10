package com.spooky2.huntkill.ui.hunt

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    // System back while killing = Stop & Zero (zero + back to config).
    BackHandler(enabled = state.isRunning) { viewModel.safetyStop() }

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

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Kill Phase", style = MaterialTheme.typography.headlineSmall)
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
        Text("Elapsed: ${formatElapsed(state.elapsedSeconds)}", style = MaterialTheme.typography.bodyMedium)

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

        LinearProgressIndicator(
            progress = { (state.percentComplete / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )

        // Detected hits being treated. The frequency currently killing (1-based
        // killIndex) is highlighted so the user sees the chosen frequencies.
        Text("Hits (${state.hits.size})", style = MaterialTheme.typography.titleSmall)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(state.hits) { index, hit ->
                val isCurrent = index == state.killIndex - 1
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (isCurrent) {
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    } else {
                        CardDefaults.cardColors()
                    },
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            "${index + 1}. ${hit.frequency.asHz()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "Deviation: ${"%.2f".format(hit.deviation)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
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
                onClick = viewModel::safetyStop,
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

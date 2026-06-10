package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.ui.common.DisclaimerBanner
import kotlinx.coroutines.delay

@Composable
fun KillScreen(
    viewModel: HuntViewModel,
    onFinished: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.phase) {
        when (state.phase) {
            HuntPhase.Done, HuntPhase.Cancelled, HuntPhase.Error -> onFinished()
            else -> Unit
        }
    }

    // Local per-frequency countdown, reset whenever the engine advances to a new
    // kill step. The engine owns the real dwell; this is the visible countdown.
    var remaining by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.killIndex, state.killDwellRemainingSeconds, state.phase) {
        if (state.phase == HuntPhase.Killing && state.killDwellRemainingSeconds > 0) {
            remaining = state.killDwellRemainingSeconds
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Kill Phase", style = MaterialTheme.typography.headlineSmall)
        Text(state.statusText, style = MaterialTheme.typography.bodyMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Frequency ${state.killIndex}/${state.killTotal}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("${"%.2f".format(state.currentFrequency)} Hz")
                Spacer(Modifier.height(8.dp))
                Text("Dwell remaining: ${remaining}s", style = MaterialTheme.typography.titleLarge)
            }
        }

        LinearProgressIndicator(
            progress = { (state.percentComplete / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = viewModel::safetyStop, modifier = Modifier.fillMaxWidth()) {
            Text("Stop (safety)")
        }

        Spacer(Modifier.height(8.dp))
        DisclaimerBanner()
    }
}

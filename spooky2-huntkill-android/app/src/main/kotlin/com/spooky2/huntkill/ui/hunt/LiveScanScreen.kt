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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.ui.common.DisclaimerBanner
import com.spooky2.huntkill.ui.common.KeepScreenOn
import com.spooky2.huntkill.ui.common.asHz
import com.spooky2.huntkill.ui.common.formatElapsed
import com.spooky2.huntkill.ui.common.vibrateOnce
import com.spooky2.huntkill.ui.theme.MonoNumberLarge
import com.spooky2.huntkill.ui.theme.MonoNumberSmall
import com.spooky2.huntkill.ui.theme.SLError
import com.spooky2.huntkill.ui.theme.SLPrimary
import com.spooky2.huntkill.ui.theme.SLSurface
import com.spooky2.huntkill.ui.theme.SectionLabel

@Composable
fun LiveScanScreen(
    viewModel: HuntViewModel,
    onKilling: () -> Unit,
    onDone: () -> Unit,
    onCancelled: () -> Unit,
    onDropouts: () -> Unit,
    onError: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val activeIndex by viewModel.activeIndex.collectAsState()
    val context = LocalContext.current

    KeepScreenOn()

    var showStopConfirm by remember { mutableStateOf(false) }
    BackHandler(enabled = state.isRunning) { showStopConfirm = true }
    if (showStopConfirm) {
        ConfirmStopDialog(
            title        = "Stop the scan?",
            text         = "The hunt is still running. Stopping zeroes the generator and discards this sweep.",
            confirmLabel = "Stop & zero",
            onConfirm    = { showStopConfirm = false; viewModel.cancel() },
            onDismiss    = { showStopConfirm = false },
        )
    }

    // Only auto-navigate on a phase change that belongs to the generator we're VIEWING.
    // Switching tabs swaps `state` to another controller (possibly already terminal);
    // that must NOT fire onDone/onKilling/etc. and tear down this screen. Skip the first
    // emission after a tab switch.
    var navIndex by remember { mutableStateOf(activeIndex) }
    LaunchedEffect(state.phase, activeIndex) {
        if (activeIndex != navIndex) { navIndex = activeIndex; return@LaunchedEffect }
        when (state.phase) {
            HuntPhase.HitsReady, HuntPhase.Killing -> { vibrateOnce(context); onKilling() }
            HuntPhase.HitsReadyWithDropouts        -> { vibrateOnce(context); onDropouts() }
            HuntPhase.Done                         -> onDone()
            HuntPhase.Cancelled                    -> onCancelled()
            HuntPhase.Error                        -> onError()
            else                                   -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)) {
        // Scrollable readings area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Live Scan", style = MaterialTheme.typography.headlineSmall)

            if (state.busyAction != null) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color    = SLPrimary,
                )
            }

            Text(
                state.statusText,
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines  = 1,
                softWrap  = false,
                overflow  = TextOverflow.Ellipsis,
            )

            // Big frequency readout in Space Mono — the headline number
            Text(
                state.currentFrequency.asHz(),
                style = MonoNumberLarge,
                color = SLPrimary,
            )

            // Progress row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "${"%.0f".format(state.percentComplete)}%",
                    style = MonoNumberSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (state.estimatedRemainingSeconds > 0) {
                        "~${formatElapsed(state.estimatedRemainingSeconds)} left"
                    } else {
                        "estimating…"
                    },
                    style = MonoNumberSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatElapsed(state.elapsedSeconds),
                    style = MonoNumberSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LinearProgressIndicator(
                progress    = { (state.percentComplete / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier    = Modifier.fillMaxWidth(),
                color       = SLPrimary,
                trackColor  = MaterialTheme.colorScheme.surfaceVariant,
            )

            // Telemetry row — all mono numbers
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column {
                    Text("AMP CV", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.amplitudeCv.toString(), style = MonoNumberSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("ANGLE", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("%.1f".format(state.currentReading), style = MonoNumberSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("AVG", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("%.1f".format(state.runningAverage), style = MonoNumberSmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            var selectedMarker by remember { mutableStateOf<GraphMarker?>(null) }

            // Graph section label
            Text("ANGLE READINGS", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (state.fullHistory.isNotEmpty()) {
                ScrollableReadingGraph(
                    readings    = state.fullHistory,
                    valid       = state.historyValid,
                    markers     = state.graphMarkers,
                    onMarkerTap = { selectedMarker = it },
                    modifier    = Modifier.fillMaxWidth().height(150.dp),
                )
            } else {
                AngleGraph(
                    readings = state.angleHistory,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                )
            }

            // Live candidates list below the graph — grows with the page's vertical scroll
            LiveCandidatesPanel(
                markers       = state.graphMarkers,
                onCandidateTap = { selectedMarker = it },
            )

            selectedMarker?.let { marker ->
                MarkerDetailSheet(
                    marker   = marker,
                    viewModel = viewModel,
                    onDismiss = { selectedMarker = null },
                )
            }
        } // end scrollable

        Spacer(Modifier.height(8.dp))

        // Run-mode toggles, pinned with the action buttons (same chips as the Kill
        // screen) so repeat/refine can be set while the sweep is still running.
        RunModeChips(state = state, viewModel = viewModel)
        Spacer(Modifier.height(4.dp))

        val isBusy = state.busyAction != null
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
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = SLError,
                ),
                border   = androidx.compose.foundation.BorderStroke(1.dp, SLError.copy(alpha = 0.6f)),
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color       = SLError,
                    )
                    Spacer(Modifier.size(6.dp))
                }
                Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.height(4.dp))
        DisclaimerBanner()
        GeneratorTabs(tabs = tabs, activeIndex = activeIndex, onSelect = viewModel::setActiveGenerator)
    }
}

@Composable
private fun AngleGraph(readings: List<Double>, modifier: Modifier = Modifier) {
    val lineColor = SLPrimary
    Canvas(modifier = modifier) {
        if (readings.size < 2) return@Canvas
        val minV  = readings.min()
        val maxV  = readings.max()
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

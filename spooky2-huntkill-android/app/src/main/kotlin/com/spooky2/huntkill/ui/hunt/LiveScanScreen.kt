package com.spooky2.huntkill.ui.hunt

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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

    // System back while running = Cancel (zero + back to config), never a silent run.
    BackHandler(enabled = state.isRunning) { viewModel.cancel() }

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
        if (state.fullHistory.isNotEmpty()) {
            // Sweep complete: full horizontally scrollable history with dropout tints.
            ScrollableReadingGraph(
                readings = state.fullHistory,
                valid = state.historyValid,
                modifier = Modifier.fillMaxWidth().height(160.dp),
            )
        } else {
            // Live tail while the sweep is in progress.
            AngleGraph(
                readings = state.angleHistory,
                modifier = Modifier.fillMaxWidth().height(160.dp),
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
                onClick = viewModel::cancel,
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

/**
 * Horizontally scrollable, decimated full-history graph.
 *
 * The whole reading history (up to ~15k points) is laid out on a virtual canvas
 * [pxPerPoint] wide per point; the visible viewport is drawn from a [horizontalScroll]
 * state. To keep scrolling smooth the visible range is decimated to one min/max pair
 * per ~3px bucket (a vertical segment), so the worst case is ~viewportWidth segments
 * regardless of point count.
 *
 * Auto-follow: while the user has NOT scrolled away from the right edge the view
 * snaps to the newest data as [readings] grows. Once they scroll back, following
 * stops; a "Live" button (shown only when not at the end) jumps back and resumes.
 * Flagged dropout steps are tinted with a translucent red band.
 */
@Composable
private fun ScrollableReadingGraph(
    readings: FloatArray,
    valid: BooleanArray,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val dropoutColor = MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Virtual canvas width: a fixed px-per-point, capped so 15k points stay a sane
    // total width. The whole history is drawn once (decimated to min/max buckets),
    // and horizontalScroll clips to the viewport.
    val pxPerPoint = with(density) { 2.dp.toPx() }
    val contentWidthDp = with(density) { (readings.size * pxPerPoint).toDp() }

    // At-end detection: true while scrolled to (or near) the right edge. maxValue is
    // 0 until the content overflows, in which case we are trivially "at end".
    val atEnd = scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue - 2

    // Auto-follow: snap to the newest data as the history grows, unless the user has
    // scrolled away from the end.
    LaunchedEffect(readings.size) {
        if (atEnd && scrollState.maxValue > 0) scrollState.scrollTo(scrollState.maxValue)
    }

    Box(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .width(contentWidthDp),
        ) {
            if (readings.size < 2) return@Canvas
            val minV = readings.min()
            val maxV = readings.max()
            val range = (maxV - minV).takeIf { it > 0f } ?: 1f
            val h = size.height

            // Dropout tint: shade contiguous invalid runs across the whole history.
            var runStart = -1
            for (i in 0..readings.size) {
                val invalid = i < readings.size && i < valid.size && !valid[i]
                if (invalid) {
                    if (runStart < 0) runStart = i
                } else if (runStart >= 0) {
                    drawRect(
                        color = dropoutColor,
                        topLeft = Offset(runStart * pxPerPoint, 0f),
                        size = Size((i - runStart) * pxPerPoint, h),
                    )
                    runStart = -1
                }
            }

            // Min/max decimation: one vertical segment per ~3px bucket so the number
            // of draw ops is bounded by the content WIDTH, not the point count.
            val pointsPerBucket = (3f / pxPerPoint).toInt().coerceAtLeast(1)
            var i = 0
            var prevX = -1f
            var prevY = 0f
            while (i < readings.size) {
                val end = (i + pointsPerBucket).coerceAtMost(readings.size)
                var lo = Float.MAX_VALUE
                var hi = -Float.MAX_VALUE
                for (j in i until end) {
                    val v = readings[j]
                    if (v < lo) lo = v
                    if (v > hi) hi = v
                }
                val x = ((i + end - 1) / 2f) * pxPerPoint
                val yLo = h * (1f - (lo - minV) / range)
                val yHi = h * (1f - (hi - minV) / range)
                drawLine(color = lineColor, start = Offset(x, yLo), end = Offset(x, yHi), strokeWidth = 2f)
                val yMid = (yLo + yHi) / 2f
                if (prevX >= 0f) {
                    drawLine(color = lineColor, start = Offset(prevX, prevY), end = Offset(x, yMid), strokeWidth = 1.5f)
                }
                prevX = x
                prevY = yMid
                i = end
            }
        }

        if (!atEnd) {
            Button(
                onClick = { scope.launch { scrollState.animateScrollTo(scrollState.maxValue) } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            ) {
                Text("Live", maxLines = 1)
            }
        }
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

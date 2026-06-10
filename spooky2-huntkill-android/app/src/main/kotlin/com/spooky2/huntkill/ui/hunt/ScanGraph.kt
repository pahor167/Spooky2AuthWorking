package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.core.lookup.LookupMatch
import com.spooky2.huntkill.ui.common.asHz
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Px width allotted per reading on the virtual (scrollable) graph canvas. */
private val PX_PER_POINT_DP = 2.dp

/** Marker dot radius. */
private val MARKER_RADIUS_DP = 5.dp

/** Touch slop for tapping a marker (generous so small dots stay easy to hit). */
private val MARKER_TOUCH_DP = 24.dp

/**
 * Horizontally scrollable, decimated full-history graph with clickable hit-frequency
 * markers.
 *
 * The whole reading history (up to ~15k points) is laid out on a virtual canvas
 * [PX_PER_POINT_DP] wide per point; the visible viewport is drawn from a [horizontalScroll]
 * state. The visible range is decimated to one min/max pair per ~3px bucket so scrolling
 * stays smooth regardless of point count.
 *
 * [markers] are drawn as red filled dots (provisional = semi-transparent, final = solid)
 * at (stepIndex → x, reading → y) using the SAME transform as the curve, so they scroll
 * with the data and stay correct under decimation. Tapping near a dot invokes
 * [onMarkerTap] with the nearest marker within the touch slop.
 *
 * Auto-follow: while the user has NOT scrolled away from the right edge the view snaps to
 * the newest data as [readings] grows. Flagged dropout steps get a translucent red band.
 */
@Composable
fun ScrollableReadingGraph(
    readings: FloatArray,
    valid: BooleanArray,
    markers: List<GraphMarker>,
    onMarkerTap: (GraphMarker) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val dropoutColor = MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
    val provisionalDot = Color.Red.copy(alpha = 0.45f)
    val finalDot = Color.Red
    val ringColor = Color.White
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val pxPerPoint = with(density) { PX_PER_POINT_DP.toPx() }
    val markerRadius = with(density) { MARKER_RADIUS_DP.toPx() }
    val touchSlop = with(density) { MARKER_TOUCH_DP.toPx() }
    val contentWidthDp = with(density) { (readings.size * pxPerPoint).toDp() }

    val atEnd = scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue - 2

    LaunchedEffect(readings.size) {
        if (atEnd && scrollState.maxValue > 0) scrollState.scrollTo(scrollState.maxValue)
    }

    // Min/max over the whole history backs the Y transform for both curve and markers.
    val minV = if (readings.isEmpty()) 0f else readings.min()
    val maxV = if (readings.isEmpty()) 0f else readings.max()
    val range = (maxV - minV).takeIf { it > 0f } ?: 1f

    Box(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .width(contentWidthDp)
                .pointerInput(markers, readings.size) {
                    detectTapGestures { tap ->
                        // tap is in CONTENT coordinates (the scrolled virtual canvas),
                        // matching the marker x = stepIndex * pxPerPoint transform.
                        val h = size.height.toFloat()
                        var best: GraphMarker? = null
                        var bestDist = Float.MAX_VALUE
                        for (m in markers) {
                            if (m.stepIndex >= readings.size) continue
                            val mx = m.stepIndex * pxPerPoint
                            val my = h * (1f - (readings[m.stepIndex] - minV) / range)
                            val dx = mx - tap.x
                            val dy = my - tap.y
                            val dist = abs(dx) + abs(dy)
                            if (dist < bestDist) {
                                bestDist = dist
                                best = m
                            }
                        }
                        val within = best?.let { m ->
                            val mx = m.stepIndex * pxPerPoint
                            val my = h * (1f - (readings[m.stepIndex] - minV) / range)
                            abs(mx - tap.x) <= touchSlop && abs(my - tap.y) <= touchSlop
                        } ?: false
                        if (within) best?.let(onMarkerTap)
                    }
                },
        ) {
            if (readings.size < 2) return@Canvas
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

            // Min/max decimation: one vertical segment per ~3px bucket.
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

            // Hit-frequency markers: red dot with a white outline ring for contrast.
            // X from stepIndex directly (same transform as the curve), Y from the
            // reading at that step. O(N<=10) draw ops.
            for (m in markers) {
                if (m.stepIndex >= readings.size) continue
                val mx = m.stepIndex * pxPerPoint
                val my = h * (1f - (readings[m.stepIndex] - minV) / range)
                drawCircle(color = ringColor, radius = markerRadius + 2f, center = Offset(mx, my))
                drawCircle(
                    color = if (m.isFinal) finalDot else provisionalDot,
                    radius = markerRadius,
                    center = Offset(mx, my),
                )
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

/**
 * Bottom sheet shown when a graph marker is tapped: the frequency, its deviation,
 * provisional/final status, and the reverse-lookup matches. For provisional hits the
 * matches are looked up on demand (a spinner shows while loading); for final hits the
 * already-computed [HuntViewModel] results are reused.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkerDetailSheet(
    marker: GraphMarker,
    viewModel: HuntViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    // null = loading; non-null = resolved (possibly empty for "no matches").
    var matches by remember(marker) { mutableStateOf<List<LookupMatch>?>(null) }

    LaunchedEffect(marker) {
        matches = runCatching { viewModel.lookupForFrequency(marker.frequency) }.getOrNull()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                marker.frequency.asHz(),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                if (marker.isFinal) "Confirmed hit" else "Provisional candidate",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("Deviation: ${"%.2f".format(marker.deviation)}")

            Spacer(Modifier.height(4.dp))
            Text("Database matches", style = MaterialTheme.typography.titleSmall)
            MarkerMatches(matches)
        }
    }
}

/** Render the reverse-lookup matches for a marker, with loading / empty / list states. */
@Composable
private fun MarkerMatches(matches: List<LookupMatch>?) {
    when {
        matches == null -> {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("Looking up matches…", style = MaterialTheme.typography.bodySmall)
            }
        }
        matches.isEmpty() -> {
            Text(
                "No matches",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                matches.forEach { match ->
                    Text(match.toReportLine(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.derivedStateOf
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

    // Auto-follow the newest data, driven by the SCROLL STATE (not pointer events,
    // which horizontalScroll consumes first). Rules:
    //  - a user drag disengages follow → the position the user set is held;
    //  - releasing while scrolled to the front edge (newest) re-engages follow;
    //  - the Live button re-engages follow and animates to the front.
    var following by remember { mutableStateOf(true) }
    val atEnd by remember {
        derivedStateOf { scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue - 4 }
    }
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            following = false // user grabbed the graph → hold their position
        } else if (atEnd) {
            following = true // settled at the newest edge → resume following
        }
    }
    // New data arrives: snap to the newest only while following (instant scroll does
    // not set isScrollInProgress, so it never trips the disengage rule above).
    LaunchedEffect(readings.size) {
        if (following && scrollState.maxValue > 0) scrollState.scrollTo(scrollState.maxValue)
    }

    // Y transform from VALID readings only: invalid steps carry 0/garbage
    // placeholders that would pin the baseline to 0 and squash the real curve.
    var minV = Float.MAX_VALUE
    var maxV = -Float.MAX_VALUE
    for (idx in readings.indices) {
        if (idx < valid.size && !valid[idx]) continue
        val v = readings[idx]
        if (v < minV) minV = v
        if (v > maxV) maxV = v
    }
    if (minV > maxV) { // no valid samples yet
        minV = 0f
        maxV = 1f
    }
    // Small padding so the curve doesn't hug the edges.
    val pad = ((maxV - minV) * 0.05f).takeIf { it > 0f } ?: 0.5f
    minV -= pad
    maxV += pad
    val range = (maxV - minV).takeIf { it > 0f } ?: 1f

    Box(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .width(contentWidthDp)
                // ONE pointer-input node handles both concerns so taps are never
                // swallowed by a competing gesture detector. We watch the Initial pass
                // for real drags (to disengage auto-follow) and the Main pass for taps
                // (to open the marker sheet). The Canvas sits AFTER width() and
                // horizontalScroll(), so its local coordinates are CONTENT coordinates —
                // the same space as the marker x = stepIndex * pxPerPoint transform — and
                // no scrollState.value correction is needed.
                .pointerInput(markers, readings.size) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPos = down.position
                        var dragged = false
                        // Track the gesture: a real positional move means the user is
                        // scrolling (handled by horizontalScroll + the scroll-state
                        // follow rules), so it is NOT a tap. A press that ends near where
                        // it started is a tap → hit-test markers.
                        do {
                            val event = awaitPointerEvent()
                            val moved = event.changes.any {
                                (it.position - downPos).getDistance() > touchSlop
                            }
                            if (moved) dragged = true
                        } while (event.changes.any { it.pressed })

                        if (!dragged) {
                            val tap = downPos
                            val h = size.height.toFloat()
                            var best: GraphMarker? = null
                            var bestDist = Float.MAX_VALUE
                            for (m in markers) {
                                if (m.stepIndex >= readings.size) continue
                                val mx = m.stepIndex * pxPerPoint
                                val my = h * (1f - (readings[m.stepIndex] - minV) / range)
                                val dist = abs(mx - tap.x) + abs(my - tap.y)
                                if (dist < bestDist) {
                                    bestDist = dist
                                    best = m
                                }
                            }
                            // Horizontal slop is generous (markers are spaced along x and
                            // the vertical reading may be off-screen tall); the y check is
                            // looser so a vertically-mismatched tap on the right column
                            // still opens the nearest marker.
                            val within = best?.let { m ->
                                val mx = m.stepIndex * pxPerPoint
                                abs(mx - tap.x) <= touchSlop
                            } ?: false
                            if (within) best?.let(onMarkerTap)
                        }
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

            // Min/max decimation: one vertical segment per ~3px bucket. Invalid steps
            // are excluded — a bucket with no valid samples is skipped entirely,
            // leaving a gap under the dropout band instead of a dive to 0.
            val pointsPerBucket = (3f / pxPerPoint).toInt().coerceAtLeast(1)
            var i = 0
            var prevX = -1f
            var prevY = 0f
            while (i < readings.size) {
                val end = (i + pointsPerBucket).coerceAtMost(readings.size)
                var lo = Float.MAX_VALUE
                var hi = -Float.MAX_VALUE
                for (j in i until end) {
                    if (j < valid.size && !valid[j]) continue
                    val v = readings[j]
                    if (v < lo) lo = v
                    if (v > hi) hi = v
                }
                if (lo > hi) { // bucket fully invalid → gap; restart the polyline after it
                    prevX = -1f
                    i = end
                    continue
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

        if (!following) {
            Button(
                onClick = {
                    // Instant (not animated) so it doesn't set isScrollInProgress, which
                    // would otherwise immediately disengage the follow we just enabled.
                    following = true
                    scope.launch { scrollState.scrollTo(scrollState.maxValue) }
                },
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

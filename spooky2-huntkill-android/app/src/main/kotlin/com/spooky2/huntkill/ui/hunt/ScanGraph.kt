package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.core.lookup.LookupMatch
import com.spooky2.huntkill.ui.common.asHz
import com.spooky2.huntkill.ui.theme.MonoNumberMedium
import com.spooky2.huntkill.ui.theme.MonoNumberSmall
import com.spooky2.huntkill.ui.theme.SLActive
import com.spooky2.huntkill.ui.theme.SLError
import com.spooky2.huntkill.ui.theme.SLOnActive
import com.spooky2.huntkill.ui.theme.SLGraphPanel
import com.spooky2.huntkill.ui.theme.SLOutline
import com.spooky2.huntkill.ui.theme.SLPrimary
import com.spooky2.huntkill.ui.theme.SectionLabel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Px width allotted per reading on the virtual (scrollable) graph canvas. */
private val PX_PER_POINT_DP = 2.dp

/** Marker dot radius. */
private val MARKER_RADIUS_DP = 5.dp

/** Touch slop for tapping a marker (generous so small dots stay easy to hit). */
private val MARKER_TOUCH_DP = 24.dp

/**
 * Horizontally scrollable, decimated full-history graph rendered on a dark scope panel.
 *
 * Trace: platinum. Dropout tint: translucent coral. Hit dots: solid coral with
 * light ring. Grid: 3 faint horizontal hairlines for scope readability.
 * Panel: dark inset (#0B0C0F) with 1dp outline and 12dp radius.
 *
 * Auto-follow: while the user has NOT scrolled away from the right edge the view snaps
 * to the newest data as [readings] grows. A "Live" button re-engages follow.
 */
@Composable
fun ScrollableReadingGraph(
    readings: FloatArray,
    valid: BooleanArray,
    markers: List<GraphMarker>,
    onMarkerTap: (GraphMarker) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * When true, a long-press marks a selection start, a second long-press marks the end,
     * and a third resets to a new start. Used by the review screen to let the user pick a
     * sweep-step range to manually re-scan. Off by default so the live graph is unchanged.
     */
    selectionEnabled: Boolean = false,
    /**
     * Invoked whenever the manual selection becomes COMPLETE (both ends set) with the
     * normalized inclusive step range, or with null when the selection is cleared/reset.
     * The host reads this to render the "Re-scan selection" action buttons.
     */
    onSelectionChange: ((IntRange?) -> Unit)? = null,
) {
    val lineColor    = SLPrimary
    val selectionBand = SLActive.copy(alpha = 0.18f)
    val dropoutColor = SLError.copy(alpha = 0.18f)
    val gridColor    = Color(0xFF1A1E25)
    val baselineColor = Color(0xFF222831)
    val provisionalDot = SLError.copy(alpha = 0.50f)
    val finalDot       = SLError
    val ringColor      = Color(0xFFF2F4F7)

    val density    = LocalDensity.current
    val scrollState = rememberScrollState()
    val scope       = rememberCoroutineScope()

    val pxPerPoint    = with(density) { PX_PER_POINT_DP.toPx() }
    val markerRadius  = with(density) { MARKER_RADIUS_DP.toPx() }
    val touchSlop     = with(density) { MARKER_TOUCH_DP.toPx() }
    val contentWidthDp = with(density) { (readings.size * pxPerPoint).toDp() }

    // Manual range selection: first long-press sets selStart, second sets selEnd, a
    // third resets to a new selStart. Both are 0-based sweep-step indices into [readings].
    var selStart by remember(selectionEnabled) { mutableStateOf<Int?>(null) }
    var selEnd by remember(selectionEnabled) { mutableStateOf<Int?>(null) }

    fun clearSelection() {
        selStart = null
        selEnd = null
        onSelectionChange?.invoke(null)
    }

    var following by remember { mutableStateOf(true) }
    val atEnd by remember {
        derivedStateOf { scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue - 4 }
    }
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            following = false
        } else if (atEnd) {
            following = true
        }
    }
    LaunchedEffect(readings.size) {
        if (following && scrollState.maxValue > 0) scrollState.scrollTo(scrollState.maxValue)
    }

    // Y range from valid readings only
    var minV = Float.MAX_VALUE
    var maxV = -Float.MAX_VALUE
    for (idx in readings.indices) {
        if (idx < valid.size && !valid[idx]) continue
        val v = readings[idx]
        if (v < minV) minV = v
        if (v > maxV) maxV = v
    }
    if (minV > maxV) { minV = 0f; maxV = 1f }
    val pad = ((maxV - minV) * 0.05f).takeIf { it > 0f } ?: 0.5f
    minV -= pad
    maxV += pad
    val range = (maxV - minV).takeIf { it > 0f } ?: 1f

    // Dark scope panel
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SLGraphPanel)
            .border(1.dp, SLOutline, RoundedCornerShape(12.dp)),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(scrollState)
                .width(contentWidthDp)
                // ONE detector for tap (open marker) AND long-press (selection) — they're
                // mutually exclusive in detectTapGestures, so a long-press never also
                // fires a marker tap, and it coexists with the horizontalScroll (a drag
                // cancels the tap and scroll takes over). Positions are in the scrolled
                // content's own coords (the Canvas IS the content), mapped to a step via
                // the same pxPerPoint the trace is drawn with.
                .pointerInput(markers, readings.size, selectionEnabled) {
                    detectTapGestures(
                        onTap = { pos ->
                            val h = size.height.toFloat()
                            var best: GraphMarker? = null
                            var bestDist = Float.MAX_VALUE
                            for (m in markers) {
                                if (m.stepIndex >= readings.size) continue
                                val mx   = m.stepIndex * pxPerPoint
                                val my   = h * (1f - (readings[m.stepIndex] - minV) / range)
                                val dist = abs(mx - pos.x) + abs(my - pos.y)
                                if (dist < bestDist) { bestDist = dist; best = m }
                            }
                            val within = best?.let { m ->
                                val mx = m.stepIndex * pxPerPoint
                                val my = h * (1f - (readings[m.stepIndex] - minV) / range)
                                abs(mx - pos.x) <= touchSlop && abs(my - pos.y) <= touchSlop
                            } ?: false
                            if (within) best?.let(onMarkerTap)
                        },
                        onLongPress = if (!selectionEnabled) null else { pos ->
                            if (readings.size >= 2) {
                                val step = (pos.x / pxPerPoint).roundToInt()
                                    .coerceIn(0, readings.lastIndex)
                                if (selStart == null || selEnd != null) {
                                    selStart = step; selEnd = null
                                    onSelectionChange?.invoke(null)
                                } else {
                                    val s = selStart ?: step
                                    selStart = minOf(s, step); selEnd = maxOf(s, step)
                                    onSelectionChange?.invoke(selStart!!..selEnd!!)
                                }
                            }
                        },
                    )
                },
        ) {
            if (readings.size < 2) return@Canvas
            val h = size.height
            val w = size.width

            // Subtle horizontal grid (3 hairlines) + centre baseline
            val gridPositions = listOf(0.25f, 0.5f, 0.75f)
            for (gy in gridPositions) {
                val lineY = h * gy
                drawLine(
                    color = if (gy == 0.5f) baselineColor else gridColor,
                    start = Offset(0f, lineY),
                    end   = Offset(w, lineY),
                    strokeWidth = 1f,
                )
            }

            // Dropout tint bands
            var runStart = -1
            for (i in 0..readings.size) {
                val invalid = i < readings.size && i < valid.size && !valid[i]
                if (invalid) {
                    if (runStart < 0) runStart = i
                } else if (runStart >= 0) {
                    drawRect(
                        color    = dropoutColor,
                        topLeft  = Offset(runStart * pxPerPoint, 0f),
                        size     = Size((i - runStart) * pxPerPoint, h),
                    )
                    runStart = -1
                }
            }

            // Decimated polyline trace in platinum
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
                if (lo > hi) { prevX = -1f; i = end; continue }
                val x   = ((i + end - 1) / 2f) * pxPerPoint
                val yLo = h * (1f - (lo - minV) / range)
                val yHi = h * (1f - (hi - minV) / range)
                drawLine(color = lineColor, start = Offset(x, yLo), end = Offset(x, yHi), strokeWidth = 2f)
                val yMid = (yLo + yHi) / 2f
                if (prevX >= 0f) {
                    drawLine(color = lineColor, start = Offset(prevX, prevY), end = Offset(x, yMid), strokeWidth = 1.5f)
                }
                prevX = x; prevY = yMid
                i = end
            }

            // Hit-frequency dots: coral with white ring
            for (m in markers) {
                if (m.stepIndex >= readings.size) continue
                val mx = m.stepIndex * pxPerPoint
                val my = h * (1f - (readings[m.stepIndex] - minV) / range)
                drawCircle(color = ringColor,                                      radius = markerRadius + 2f, center = Offset(mx, my))
                drawCircle(color = if (m.isFinal) finalDot else provisionalDot,   radius = markerRadius,      center = Offset(mx, my))
            }

            // Manual-selection overlay: a translucent band between the two chosen steps
            // plus a vertical line + handle at each end (SLActive theme accent).
            if (selectionEnabled) {
                val startX = selStart?.let { it * pxPerPoint }
                val endX = selEnd?.let { it * pxPerPoint }
                if (startX != null && endX != null) {
                    val bandLo = minOf(startX, endX)
                    val bandHi = maxOf(startX, endX)
                    drawRect(
                        color   = selectionBand,
                        topLeft = Offset(bandLo, 0f),
                        size    = Size((bandHi - bandLo).coerceAtLeast(1f), h),
                    )
                }
                for (edge in listOfNotNull(startX, endX)) {
                    drawLine(
                        color = SLActive,
                        start = Offset(edge, 0f),
                        end   = Offset(edge, h),
                        strokeWidth = 2f,
                    )
                    drawCircle(color = SLActive, radius = markerRadius, center = Offset(edge, h / 2f))
                }
            }
        }

        // Clear-selection affordance, shown once a start is placed.
        if (selectionEnabled && selStart != null) {
            Button(
                onClick = { clearSelection() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                shape   = RoundedCornerShape(6.dp),
                colors  = ButtonDefaults.buttonColors(
                    containerColor = SLActive,
                    contentColor   = SLOnActive,
                ),
            ) {
                Text("✕", style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }

        // Live / auto-follow button
        if (!following) {
            Button(
                onClick  = {
                    following = true
                    scope.launch { scrollState.scrollTo(scrollState.maxValue) }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                shape    = RoundedCornerShape(6.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = SLActive,
                    contentColor   = SLOnActive,
                ),
            ) {
                Text("Live", style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

/**
 * Bottom sheet shown when a graph marker is tapped: the frequency, deviation,
 * status, and reverse-lookup matches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkerDetailSheet(
    marker: GraphMarker,
    viewModel: HuntViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var matches by remember(marker) { mutableStateOf<List<LookupMatch>?>(null) }

    LaunchedEffect(marker) {
        matches = runCatching { viewModel.lookupForFrequency(marker.frequency) }.getOrNull()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
        tonalElevation   = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                marker.frequency.asHz(),
                style = MonoNumberMedium,
                color = SLPrimary,
            )
            Text(
                if (marker.isFinal) "Confirmed hit" else "Provisional candidate",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Deviation: ${"%.2f".format(marker.deviation)}",
                style = MonoNumberSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(4.dp))
            Text("DATABASE MATCHES", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MarkerMatches(matches)
        }
    }
}

@Composable
private fun MarkerMatches(matches: List<LookupMatch>?) {
    when {
        matches == null -> {
            androidx.compose.foundation.layout.Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = SLPrimary)
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

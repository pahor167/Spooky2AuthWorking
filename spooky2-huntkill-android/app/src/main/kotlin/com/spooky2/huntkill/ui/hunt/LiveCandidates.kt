package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.ui.common.asHz
import com.spooky2.huntkill.ui.theme.MonoNumberSmall
import com.spooky2.huntkill.ui.theme.SLOutline
import com.spooky2.huntkill.ui.theme.SLPrimary
import com.spooky2.huntkill.ui.theme.SectionLabel

/**
 * One row in the always-visible "Live candidates" panel: the provisional/final hit
 * frequency, its deviation, and the underlying [GraphMarker].
 */
data class CandidateRow(
    val marker: GraphMarker,
    val frequencyLabel: String,
    val deviationLabel: String,
)

/**
 * Pure mapping from the live [GraphMarker] list to the display rows shown in the
 * candidates panel, sorted by deviation descending (strongest first).
 */
fun candidateRows(markers: List<GraphMarker>): List<CandidateRow> =
    markers
        .sortedWith(compareByDescending<GraphMarker> { it.deviation }.thenBy { it.frequency })
        .map { m ->
            CandidateRow(
                marker         = m,
                frequencyLabel = m.frequency.asHz(),
                deviationLabel = "dev ${"%.1f".format(m.deviation)}",
            )
        }

/**
 * Always-visible summary of the current top-N hit candidates. Renders a vertical list
 * (strongest deviation first) that grows with the page's vertical scroll. Each row is
 * tappable.
 */
@Composable
fun LiveCandidatesPanel(
    markers: List<GraphMarker>,
    onCandidateTap: (GraphMarker) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows    = candidateRows(markers)
    val isFinal = rows.isNotEmpty() && rows.all { it.marker.isFinal }
    val label   = if (isFinal) "HITS (${rows.size})" else "CANDIDATES (${rows.size})"

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (rows.isEmpty()) {
            Text(
                "Scanning… no peaks yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            rows.forEach { row ->
                CandidateListRow(row = row, onTap = { onCandidateTap(row.marker) })
            }
        }
    }
}

/** One full-width tappable candidate row: frequency left, deviation right. */
@Composable
private fun CandidateListRow(row: CandidateRow, onTap: () -> Unit) {
    val final = row.marker.isFinal
    Surface(
        onClick = onTap,
        shape   = RoundedCornerShape(8.dp),
        color   = if (final) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border  = BorderStroke(1.dp, if (final) SLPrimary.copy(alpha = 0.4f) else SLOutline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                row.frequencyLabel,
                style    = MonoNumberSmall,
                color    = if (final) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                row.deviationLabel,
                style    = MonoNumberSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

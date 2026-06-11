package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
 * Always-visible summary of the current top-N hit candidates. Renders a horizontally-
 * scrollable single row of chips so it never grows tall. Each chip is tappable.
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rows.forEach { row ->
                    SuggestionChip(
                        onClick = { onCandidateTap(row.marker) },
                        label   = {
                            Text(
                                "${row.frequencyLabel}  ·  ${row.deviationLabel}",
                                style    = MonoNumberSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        shape  = RoundedCornerShape(8.dp),
                        colors = if (row.marker.isFinal) {
                            SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor     = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        } else {
                            SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor     = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled     = true,
                            borderColor = if (row.marker.isFinal) SLPrimary.copy(alpha = 0.4f) else SLOutline,
                        ),
                    )
                }
            }
        }
    }
}

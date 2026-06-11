package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.ui.common.asHz

/**
 * One row in the always-visible "Live candidates" panel: the provisional/final hit
 * frequency, its deviation, and the underlying [GraphMarker] (so a tap can reuse the
 * existing [MarkerDetailSheet] reverse-lookup flow).
 */
data class CandidateRow(
    val marker: GraphMarker,
    val frequencyLabel: String,
    val deviationLabel: String,
)

/**
 * Pure mapping from the live [GraphMarker] list to the display rows shown in the
 * candidates panel, sorted by deviation descending (strongest first). Kept free of
 * Compose so it is unit-testable.
 *
 * Ties are broken by frequency ascending for a stable, deterministic order.
 */
fun candidateRows(markers: List<GraphMarker>): List<CandidateRow> =
    markers
        .sortedWith(compareByDescending<GraphMarker> { it.deviation }.thenBy { it.frequency })
        .map { m ->
            CandidateRow(
                marker = m,
                frequencyLabel = m.frequency.asHz(),
                deviationLabel = "dev ${"%.1f".format(m.deviation)}",
            )
        }

/**
 * Always-visible summary of the current top-N hit candidates. During the sweep these are
 * provisional peaks (isFinal=false); after detection they are the confirmed hits.
 *
 * Renders a horizontally-scrollable single row of chips so it never grows tall enough to
 * push the graph off-screen. Each chip is tappable → [onCandidateTap] (wired to the
 * existing selected-marker + [MarkerDetailSheet] in [LiveScanScreen]).
 */
@Composable
fun LiveCandidatesPanel(
    markers: List<GraphMarker>,
    onCandidateTap: (GraphMarker) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = candidateRows(markers)
    // "Detected hits" once the candidates are final; "Candidates so far" while sweeping.
    val isFinal = rows.isNotEmpty() && rows.all { it.marker.isFinal }
    val title = if (isFinal) "Detected hits (${rows.size})" else "Candidates so far (${rows.size})"

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rows.forEach { row ->
                    SuggestionChip(
                        onClick = { onCandidateTap(row.marker) },
                        label = {
                            Text(
                                "${row.frequencyLabel}  ·  ${row.deviationLabel}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors = if (row.marker.isFinal) {
                            SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        } else {
                            SuggestionChipDefaults.suggestionChipColors()
                        },
                    )
                }
            }
        }
    }
}

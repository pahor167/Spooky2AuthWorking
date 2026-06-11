package com.spooky2.huntkill.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.data.RunRecord
import com.spooky2.huntkill.ui.theme.MonoNumberSmall
import com.spooky2.huntkill.ui.theme.SLOutline
import com.spooky2.huntkill.ui.theme.SLPrimary
import com.spooky2.huntkill.ui.theme.SectionLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val runDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

internal fun formatRunTimestamp(timestampMs: Long): String =
    runDateFormat.format(Date(timestampMs))

/**
 * Run-history list: every saved hunt, newest first.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenRun: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Run history", style = MaterialTheme.typography.headlineSmall)

        when {
            state.isLoading -> {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = SLPrimary)
                    Text("Loading…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            state.runs.isEmpty() -> {
                Text(
                    "No saved runs yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                LazyColumn(
                    modifier            = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(state.runs, key = { it.id }) { run ->
                        RunRow(
                            run      = run,
                            onClick  = { onOpenRun(run.id) },
                            onDelete = { viewModel.delete(run.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RunRow(
    run: RunRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border    = BorderStroke(1.dp, SLOutline),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    formatRunTimestamp(run.timestampMs),
                    style    = MonoNumberSmall,
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    run.generatorLabel ?: "Unknown generator",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${run.hits.size} frequencies",
                    style = MaterialTheme.typography.bodySmall,
                    color = SLPrimary,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete run",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(18.dp),
                )
            }
        }
    }
}

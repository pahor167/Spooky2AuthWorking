package com.spooky2.huntkill.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Date/time format for run rows, e.g. "2026-06-11 14:32". */
private val runDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

internal fun formatRunTimestamp(timestampMs: Long): String =
    runDateFormat.format(Date(timestampMs))

/**
 * Run-history list: every saved hunt, newest first. Each row shows date/time, the
 * generator label, and the frequency count; tapping opens the detail screen, and a
 * trailing delete icon removes the run.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenRun: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // Reload whenever the screen is (re)entered so a just-saved run appears.
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Run history", style = MaterialTheme.typography.headlineSmall)

        when {
            state.isLoading -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp))
                    Text("Loading…")
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
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.runs, key = { it.id }) { run ->
                        RunRow(
                            run = run,
                            onClick = { onOpenRun(run.id) },
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
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatRunTimestamp(run.timestampMs),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    run.generatorLabel ?: "Unknown generator",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${run.hits.size} frequencies",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete run")
            }
        }
    }
}

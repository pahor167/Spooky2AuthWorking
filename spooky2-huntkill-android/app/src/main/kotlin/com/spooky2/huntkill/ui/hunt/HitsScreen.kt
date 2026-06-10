package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.ui.common.DisclaimerBanner
import com.spooky2.huntkill.ui.common.asHz

@Composable
fun HitsScreen(
    viewModel: HuntViewModel,
    onStartKill: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Detected Hits (${state.hits.size})", style = MaterialTheme.typography.headlineSmall)

        if (state.hits.isEmpty()) {
            Text("Scanning… hits appear when a cycle completes.")
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.hits) { index, hit ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "${index + 1}. ${hit.frequency.asHz()}",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text("Deviation: ${"%.2f".format(hit.deviation)}")
                        Text("Reading: ${"%.1f".format(hit.reading)}")
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onStartKill,
            enabled = state.hits.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start Kill")
        }

        Spacer(Modifier.height(8.dp))
        DisclaimerBanner()
    }
}

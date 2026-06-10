package com.spooky2.huntkill.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Safety disclaimer. The real device emits electrical signals; demo mode here drives
 * a [FakeTransport][com.spooky2.huntkill.transport.fake.FakeTransport] replaying a
 * recorded dump, so no signals are emitted.
 */
@Composable
fun DisclaimerBanner(modifier: Modifier = Modifier) {
    Text(
        text = "Disclaimer: This app drives a device that emits electrical signals. " +
            "Demo mode uses recorded data — no hardware is driven and no signals are emitted.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
    )
}

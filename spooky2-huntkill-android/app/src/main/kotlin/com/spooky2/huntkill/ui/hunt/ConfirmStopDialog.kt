package com.spooky2.huntkill.ui.hunt

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Confirmation gate for leaving a RUNNING hunt/kill. Backing out mid-run zeroes the
 * generator and discards the sweep, so a single accidental back press or stray tap
 * must never do it silently — the user has to confirm.
 */
@Composable
internal fun ConfirmStopDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep running") }
        },
    )
}

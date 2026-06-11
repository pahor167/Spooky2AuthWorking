package com.spooky2.huntkill.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Generic two-button confirmation dialog.
 * Used wherever a destructive action requires explicit user confirmation before proceeding.
 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    dismissLabel: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text(title) },
        text    = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}

package com.spooky2.huntkill.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Keeps the screen on while this composable is in the composition.
 * Used during long-running hunt and kill phases to prevent the OS from
 * timing out the display and killing the foreground process.
 */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

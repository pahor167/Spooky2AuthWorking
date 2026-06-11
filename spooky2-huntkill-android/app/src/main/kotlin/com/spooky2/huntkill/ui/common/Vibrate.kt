package com.spooky2.huntkill.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Fire a single 300 ms vibration pulse.
 * Uses [VibratorManager] on API 31+ and falls back to the legacy [Vibrator] below that.
 * Never throws — callers fire this inside navigation LaunchedEffects, and a vibrator
 * failure must not prevent the screen transition from running.
 */
fun vibrateOnce(context: Context) {
    runCatching {
        val effect = VibrationEffect.createOneShot(300L, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(effect)
        }
    }
}

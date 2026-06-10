package com.spooky2.huntkill.ui.common

import java.util.Locale

/**
 * Compact, single-line frequency rendering used across the Live, Hits, and Kill
 * screens. Rounds to 2 decimals with thousands separators so long values like
 * `1795160.43666219` render as `"1,795,160.44 Hz"` instead of wrapping.
 */
fun Double.asHz(): String = "%,.2f Hz".format(Locale.US, this)

/** Format a whole number of seconds as `mm:ss` (e.g. 75 → "01:15"). */
fun formatElapsed(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}

package com.spooky2.huntkill.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ── GRAPHITE MONO — dark, clinical, near-monochrome aesthetic ────────────────
val SignalLabColorScheme = darkColorScheme(
    // Backgrounds
    background          = SLBackground,
    surface             = SLSurface,
    surfaceVariant      = SLSurfaceVariant,
    surfaceContainer    = SLSurfaceContainer,
    surfaceContainerLow = SLSurfaceContainer,
    surfaceContainerHigh = SLSurfaceElevated,

    // Structural
    outline             = SLOutline,
    outlineVariant      = SLOutlineVariant,

    // Primary — platinum / steel
    primary             = SLPrimary,
    onPrimary           = SLOnPrimary,
    primaryContainer    = SLPrimaryContainer,
    onPrimaryContainer  = SLOnPrimaryContainer,

    // Secondary — amber
    secondary           = SLSecondary,
    onSecondary         = SLOnSecondary,
    secondaryContainer  = SLSecondaryContainer,
    onSecondaryContainer = SLOnSecondaryContainer,

    // Tertiary — cold blue (active / selected)
    tertiary            = SLTertiary,
    onTertiary          = SLOnTertiary,
    tertiaryContainer   = SLTertiaryContainer,
    onTertiaryContainer = SLOnTertiaryContainer,

    // Error / kill — coral
    error               = SLError,
    onError             = SLOnError,
    errorContainer      = SLErrorContainer,
    onErrorContainer    = SLOnErrorContainer,

    // Text
    onBackground        = SLOnBackground,
    onSurface           = SLOnSurface,
    onSurfaceVariant    = SLOnSurfaceVariant,
)

@Composable
fun SignalLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SignalLabColorScheme,
        typography  = SignalLabTypography,
        content     = content,
    )
}

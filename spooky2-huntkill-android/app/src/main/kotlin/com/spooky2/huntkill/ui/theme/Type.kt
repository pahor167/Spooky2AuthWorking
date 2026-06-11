package com.spooky2.huntkill.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.spooky2.huntkill.R

// ── Google Fonts provider ────────────────────────────────────────────────────
val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage  = "com.google.android.gms",
    certificates     = R.array.com_google_android_gms_fonts_certs,
)

// ── Font families ────────────────────────────────────────────────────────────

/** Display / headlines: Archivo — characterful grotesque. Fallback to SansSerif. */
val ArchivoFamily: FontFamily = try {
    val name = GoogleFont("Archivo")
    FontFamily(
        Font(googleFont = name, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
        Font(googleFont = name, fontProvider = GoogleFontsProvider, weight = FontWeight.Medium),
        Font(googleFont = name, fontProvider = GoogleFontsProvider, weight = FontWeight.SemiBold),
        Font(googleFont = name, fontProvider = GoogleFontsProvider, weight = FontWeight.Bold),
    )
} catch (_: Throwable) {
    FontFamily.SansSerif
}

/** Body / labels: IBM Plex Sans. Fallback to SansSerif. */
val IbmPlexSansFamily: FontFamily = try {
    val name = GoogleFont("IBM Plex Sans")
    FontFamily(
        Font(googleFont = name, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
        Font(googleFont = name, fontProvider = GoogleFontsProvider, weight = FontWeight.Medium),
        Font(googleFont = name, fontProvider = GoogleFontsProvider, weight = FontWeight.SemiBold),
    )
} catch (_: Throwable) {
    FontFamily.SansSerif
}

/** Monospace technical readouts: Space Mono. Fallback to Monospace. */
val SpaceMonoFamily: FontFamily = try {
    val name = GoogleFont("Space Mono")
    FontFamily(
        Font(googleFont = name, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
        Font(googleFont = name, fontProvider = GoogleFontsProvider, weight = FontWeight.Bold),
    )
} catch (_: Throwable) {
    FontFamily.Monospace
}

// ── Signature readout style — use this for every number/Hz/CV/counter ───────
/**
 * MonoNumber: the defining detail of the SIGNAL LAB look.
 * Apply to: current-frequency readout, Hz values, deviations, dwell countdown,
 * elapsed/ETA, amplitude CV, step counters, readings/running-average.
 */
val MonoNumberLarge = TextStyle(
    fontFamily = SpaceMonoFamily,
    fontWeight  = FontWeight.Normal,
    fontSize    = 28.sp,
    lineHeight  = 34.sp,
    letterSpacing = (-0.5).sp,
)

val MonoNumberMedium = TextStyle(
    fontFamily = SpaceMonoFamily,
    fontWeight  = FontWeight.Normal,
    fontSize    = 18.sp,
    lineHeight  = 24.sp,
    letterSpacing = (-0.3).sp,
)

val MonoNumberSmall = TextStyle(
    fontFamily = SpaceMonoFamily,
    fontWeight  = FontWeight.Normal,
    fontSize    = 13.sp,
    lineHeight  = 18.sp,
    letterSpacing = 0.sp,
)

// ── Section label style ──────────────────────────────────────────────────────
/** Small uppercase letter-spaced label for section headers (e.g. "SCAN RANGE"). */
val SectionLabel = TextStyle(
    fontFamily    = IbmPlexSansFamily,
    fontWeight    = FontWeight.Medium,
    fontSize      = 10.sp,
    lineHeight    = 14.sp,
    letterSpacing = 1.2.sp,
)

// ── Material3 Typography ─────────────────────────────────────────────────────
val SignalLabTypography = Typography(
    // Display / oversized hero
    displayLarge = TextStyle(
        fontFamily   = ArchivoFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 57.sp,
        lineHeight   = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily   = ArchivoFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 45.sp,
        lineHeight   = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily   = ArchivoFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 36.sp,
        lineHeight   = 44.sp,
        letterSpacing = 0.sp,
    ),
    // Headlines (screen titles, big labels)
    headlineLarge = TextStyle(
        fontFamily   = ArchivoFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 32.sp,
        lineHeight   = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily   = ArchivoFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 26.sp,
        lineHeight   = 32.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily   = ArchivoFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 20.sp,
        lineHeight   = 26.sp,
        letterSpacing = 0.sp,
    ),
    // Title (card headers, sub-sections)
    titleLarge = TextStyle(
        fontFamily   = ArchivoFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 18.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily   = IbmPlexSansFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 15.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily   = IbmPlexSansFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 11.sp,
        lineHeight   = 14.sp,
        letterSpacing = 0.8.sp,
    ),
    // Body (narrative copy)
    bodyLarge = TextStyle(
        fontFamily   = IbmPlexSansFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 15.sp,
        lineHeight   = 22.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily   = IbmPlexSansFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 13.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily   = IbmPlexSansFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 11.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    // Labels (chips, buttons, captions)
    labelLarge = TextStyle(
        fontFamily   = IbmPlexSansFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 13.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily   = IbmPlexSansFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 11.sp,
        lineHeight   = 14.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily   = IbmPlexSansFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 10.sp,
        lineHeight   = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

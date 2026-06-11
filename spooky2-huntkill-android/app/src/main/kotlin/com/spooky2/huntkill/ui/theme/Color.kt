package com.spooky2.huntkill.ui.theme

import androidx.compose.ui.graphics.Color

// ── GRAPHITE MONO dark palette ───────────────────────────────────────────────
// Near-monochrome, professional: neutral graphite chassis, platinum/steel accent,
// a faint cold blue for active/selected states, amber for ramp, coral for kill.

// Backgrounds / surfaces
val SLBackground        = Color(0xFF0E0F12)
val SLSurface           = Color(0xFF16181C)
val SLSurfaceVariant    = Color(0xFF1E2127)
val SLSurfaceContainer  = Color(0xFF1A1D22)
val SLSurfaceElevated   = Color(0xFF1A1D22)

// Structural / outline
val SLOutline           = Color(0xFF2A2E36)
val SLOutlineVariant    = Color(0xFF21252C)

// Primary — platinum / steel accent (light neutral, not saturated)
val SLPrimary           = Color(0xFFC9D1DA)
val SLOnPrimary         = Color(0xFF14171C)
val SLPrimaryContainer  = Color(0xFF262B33)
val SLOnPrimaryContainer = Color(0xFFE8ECF2)

// Secondary — amber (ramp / secondary info), muted
val SLSecondary         = Color(0xFFD9A441)
val SLOnSecondary       = Color(0xFF1A0F00)
val SLSecondaryContainer = Color(0xFF3D2500)
val SLOnSecondaryContainer = Color(0xFFFFD8A0)

// Active / selected — cold blue (active & selected affordances only)
val SLActive            = Color(0xFF6E8BDF)
val SLOnActive          = Color(0xFF0A1024)
val SLActiveContainer   = Color(0xFF1B2434)
val SLOnActiveContainer = Color(0xFFA9BEF2)

// Tertiary mirrors the cold-blue active accent so Material picks it up
val SLTertiary          = SLActive
val SLOnTertiary        = SLOnActive
val SLTertiaryContainer = SLActiveContainer
val SLOnTertiaryContainer = SLOnActiveContainer

// Error / kill — coral
val SLError             = Color(0xFFFF5C52)
val SLOnError           = Color(0xFF1A0907)
val SLErrorContainer    = Color(0xFF3A1614)
val SLOnErrorContainer  = Color(0xFFFFB4AB)

// Text
val SLOnSurface         = Color(0xFFF2F4F7)
val SLOnSurfaceVariant  = Color(0xFF7E8794)
val SLOnBackground      = Color(0xFFF2F4F7)

// Graph panel
val SLGraphPanel        = Color(0xFF0B0C0F)

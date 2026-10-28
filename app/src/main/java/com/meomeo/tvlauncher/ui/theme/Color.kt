package com.meomeo.tvlauncher.ui.theme

import androidx.compose.ui.graphics.Color

// macOS-style palette (dark appearance). Surfaces are translucent materials laid over the
// pre-blurred wallpaper; see components/Glass.kt.
val Ink = Color(0xFF000000)

val MacBlue = Color(0xFF0A84FF)
val SystemRed = Color(0xFFFF453A)
val SystemGreen = Color(0xFF30D158)
val SystemOrange = Color(0xFFFF9F0A)
val SystemYellow = Color(0xFFFFD60A)
val SystemPink = Color(0xFFFF375F)
val SystemPurple = Color(0xFFBF5AF2)
val SystemTeal = Color(0xFF64D2FF)
val SystemGray = Color(0xFF8E8E93)

val TrafficRed = Color(0xFFFF5F57)
val TrafficYellow = Color(0xFFFEBC2E)
val TrafficGreen = Color(0xFF28C840)

val Label = Color.White
val LabelSecondary = Color.White.copy(alpha = 0.62f)
val LabelTertiary = Color.White.copy(alpha = 0.38f)

/** Thin outline of every material, like the hairline around macOS windows. */
val Hairline = Color.White.copy(alpha = 0.20f)
val Separator = Color.White.copy(alpha = 0.12f)

/** Tints laid over the blurred wallpaper. */
val WindowTint = Color(0xFF1C1C1E).copy(alpha = 0.58f)
val MenuBarTint = Color(0xFF1C1C1E).copy(alpha = 0.38f)
val DockTint = Color.White.copy(alpha = 0.20f)
val RowFill = Color.White.copy(alpha = 0.08f)

// Names used across the app.
val TextPrimary = Label
val TextSecondary = LabelSecondary
val Accent = MacBlue
val StarYellow = SystemYellow
val Danger = SystemRed
val Warning = SystemOrange
val Success = SystemGreen

package com.meomeo.tvlauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    secondary = AccentGlow,
    background = BgDark,
    surface = BgCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun MeoMeoLauncherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography().let { base ->
            // Every default style uses Quicksand, so Material components pick it up too.
            base.copy(
                displayLarge = base.displayLarge.copy(fontFamily = QuicksandFamily),
                displayMedium = base.displayMedium.copy(fontFamily = QuicksandFamily),
                displaySmall = base.displaySmall.copy(fontFamily = QuicksandFamily),
                headlineLarge = base.headlineLarge.copy(fontFamily = QuicksandFamily),
                headlineMedium = base.headlineMedium.copy(fontFamily = QuicksandFamily),
                headlineSmall = base.headlineSmall.copy(fontFamily = QuicksandFamily),
                titleLarge = base.titleLarge.copy(fontFamily = QuicksandFamily),
                titleMedium = base.titleMedium.copy(fontFamily = QuicksandFamily),
                titleSmall = base.titleSmall.copy(fontFamily = QuicksandFamily),
                bodyLarge = base.bodyLarge.copy(fontFamily = QuicksandFamily),
                bodyMedium = base.bodyMedium.copy(fontFamily = QuicksandFamily),
                bodySmall = base.bodySmall.copy(fontFamily = QuicksandFamily),
                labelLarge = base.labelLarge.copy(fontFamily = QuicksandFamily),
                labelMedium = base.labelMedium.copy(fontFamily = QuicksandFamily),
                labelSmall = base.labelSmall.copy(fontFamily = QuicksandFamily)
            )
        }
    ) {
        // Text() outside a Surface falls back to a bare TextStyle, so set the family here too.
        ProvideTextStyle(TextStyle(fontFamily = QuicksandFamily), content = content)
    }
}


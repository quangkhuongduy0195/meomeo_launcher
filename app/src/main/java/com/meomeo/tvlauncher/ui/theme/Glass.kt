package com.meomeo.tvlauncher.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Glass colours for the whole UI. Alpha stays fixed (15% idle / 50% focused); only the hue is
 * tinted, so glass reads as white frosted glass that picks up the mood of the wallpaper.
 */
@Immutable
data class GlassPalette(val idle: Color, val focused: Color)

val LocalGlassPalette = compositionLocalOf { GlassPalette(GlassIdle, GlassFocused) }

/**
 * @param wallpaperTone average colour of the current wallpaper; tints idle and focused glass.
 * @param appTone dominant colour of the focused app; additionally tints focused glass.
 */
fun glassPalette(wallpaperTone: Color, appTone: Color): GlassPalette = GlassPalette(
    idle = lerp(Color.White, wallpaperTone, 0.30f).copy(alpha = GlassIdle.alpha),
    focused = lerp(Color.White, lerp(wallpaperTone, appTone, 0.5f), 0.55f).copy(alpha = GlassFocused.alpha)
)

@Composable
fun glassIdleColor(): Color = LocalGlassPalette.current.idle

@Composable
fun glassFocusedColor(): Color = LocalGlassPalette.current.focused

/** Fill for an interactive surface: idle glass normally, focused glass when focused/selected. */
@Composable
fun glassFill(isFocused: Boolean): Color =
    animateColorAsState(
        targetValue = if (isFocused) glassFocusedColor() else glassIdleColor(),
        animationSpec = tween(200),
        label = "glassFill"
    ).value

/**
 * Opacity of the black scrim behind dialogs. Where Compose blur is available (API 31+) the screen
 * behind is blurred instead of blacked out, so only a light scrim is needed. Older devices cannot
 * blur, so they fall back to a heavy scrim to keep text on the glass panels legible.
 */
val DialogScrimAlpha: Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.35f else 0.80f

/** Blur radius of the frosted-glass backdrop inside the dock capsule. */
val DockBackdropBlur: Dp = 24.dp

/** Blur applied to the home screen while a dialog is open. */
val DialogBackdropBlur: Dp = 28.dp

/**
 * Dims everything behind the enclosing Dialog window. Glass panels are translucent, so without a
 * strong dim the home screen shows through and hurts text legibility.
 */
@Composable
fun DialogDim(amount: Float = DialogScrimAlpha) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect {
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setDimAmount(amount)
    }
}

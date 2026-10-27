package com.meomeo.tvlauncher.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * The backdrop that [liquidGlass] surfaces refract: the wallpaper layer of the window they live in.
 *
 * Each window (main screen, every Dialog, the lock screen) must provide its own. A backdrop is a
 * graphics layer of one window, so a Dialog (a separate window) must never inherit the main one.
 * Null means "no backdrop here": [liquidGlass] then falls back to flat translucent glass.
 */
val LocalGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** Blur needs RenderEffect (Android 12+). */
val BackdropBlurSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** The lens/refraction effect needs RuntimeShader (Android 13+). */
val BackdropLensSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * Liquid-glass surface: blurs and refracts the wallpaper behind it, adds a rim highlight and a
 * soft shadow, then tints it with the app's glass colour.
 *
 * Below Android 12 there is nothing to blur with, so it degrades to a darkened translucent panel;
 * on Android 12 it blurs but does not refract.
 *
 * @param lensHeight how far inside the edge the refraction reaches. Must be <= the shape's
 * corner radius or the lens shows discontinuities at the corners.
 * @param lensAmount how strongly the edge bends the backdrop.
 */
@Composable
fun Modifier.liquidGlass(
    shape: CornerBasedShape,
    isFocused: Boolean = false,
    blurRadius: Dp = 8.dp,
    lensHeight: Dp = 16.dp,
    lensAmount: Dp = 32.dp,
    withShadow: Boolean = true,
    surface: Color = if (isFocused) glassFocusedColor() else glassIdleColor()
): Modifier {
    val backdrop = LocalGlassBackdrop.current
    if (backdrop == null || !BackdropBlurSupported) {
        return this
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.35f))
            .background(surface)
    }
    val useLens = BackdropLensSupported
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(blurRadius.toPx())
            if (useLens) lens(lensHeight.toPx(), lensAmount.toPx())
        },
        shadow = if (withShadow) ({ com.kyant.backdrop.shadow.Shadow.Default }) else null,
        onDrawSurface = { drawRect(surface) }
    )
}

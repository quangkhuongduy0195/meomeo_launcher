package com.meomeo.tvlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.meomeo.tvlauncher.ui.theme.BackdropBlurSupported
import com.meomeo.tvlauncher.ui.theme.DialogBackdropBlur
import com.meomeo.tvlauncher.ui.theme.DialogScrimAlpha
import com.meomeo.tvlauncher.ui.theme.LocalGlassBackdrop

/** Draws the current wallpaper. Provided by MainScreen so dialogs can paint it inside their own window. */
val LocalWallpaperContent = compositionLocalOf<@Composable () -> Unit> { {} }

/**
 * A full-window dialog for liquid-glass panels.
 *
 * A Dialog is a separate window, so it cannot refract the home screen behind it. Instead it paints
 * the wallpaper (blurred and dimmed) as its own opaque background and records that as the
 * backdrop. Panels placed in [content] use `Modifier.liquidGlass` to refract it.
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        val wallpaper = LocalWallpaperContent.current
        val backdrop = rememberLayerBackdrop {
            drawRect(Color(0xFF060810))
            drawContent()
        }
        Box(Modifier.fillMaxSize()) {
            // Only the backdrop content goes in here; the glass panels must stay outside it.
            // The opaque base matters: the wallpaper itself is only ~55% opaque, and without a base
            // the home screen in the window below would show through the dialog window.
            Box(Modifier.fillMaxSize().background(Color(0xFF060810)).layerBackdrop(backdrop)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(if (BackdropBlurSupported) Modifier.blur(DialogBackdropBlur) else Modifier)
                ) {
                    wallpaper()
                }
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = DialogScrimAlpha)))
            }
            CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
                Box(Modifier.fillMaxSize(), contentAlignment = contentAlignment, content = content)
            }
        }
    }
}

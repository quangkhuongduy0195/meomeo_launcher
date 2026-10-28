package com.meomeo.tvlauncher.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import com.meomeo.tvlauncher.ui.theme.Hairline

// ---------------------------------------------------------------------------------------------
// Frosted glass without any per-frame blur.
//
// The wallpaper is decoded once at a tiny size; scaled back up it is already a blur. A frosted
// surface draws that blurred copy at window size, shifted so it lines up with the real wallpaper,
// clipped to the surface and tinted. That is exactly what a backdrop blur shows (the wallpaper
// region behind the surface) and costs one small bitmap draw, so it behaves the same on every TV.
// ---------------------------------------------------------------------------------------------

/**
 * The pre-blurred wallpaper of the current window, or null while it loads. Held as a [State] so
 * that a new wallpaper only recomposes the surfaces that draw it, not the screen that provides it.
 */
val LocalBlurredWallpaper = compositionLocalOf<State<ImageBitmap?>?> { null }

private const val BlurWidth = 192
private const val BlurHeight = 108
private const val BlurRadius = 7

/** macOS materials boost saturation under the blur. */
private val Saturate = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.5f) })

/** Decodes [data] (a File or a drawable id) at blur size. Keeps the previous bitmap while the next loads. */
@Composable
fun rememberBlurredWallpaper(data: Any): State<ImageBitmap?> {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(null, data) {
        val request = ImageRequest.Builder(context)
            .data(data)
            .size(BlurWidth, BlurHeight)
            .transformations(SmoothBlur(BlurRadius))
            .allowHardware(false)
            .build()
        context.imageLoader.execute(request).drawable?.toBitmap()?.asImageBitmap()?.let { value = it }
    }
}

/**
 * One-off gaussian-like blur (three box-blur passes) run by Coil off the main thread. Upscaling a
 * tiny bitmap alone leaves visible blocks; smoothing it first makes the result seamless.
 */
private class SmoothBlur(private val radius: Int) : Transformation {
    override val cacheKey = "smoothBlur-$radius"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val w = input.width
        val h = input.height
        var src = IntArray(w * h).also { input.getPixels(it, 0, w, 0, 0, w, h) }
        var dst = IntArray(w * h)
        repeat(3) {
            boxBlur(src, dst, w, h, radius, horizontal = true)
            boxBlur(dst, src, w, h, radius, horizontal = false)
        }
        return Bitmap.createBitmap(src, w, h, Bitmap.Config.ARGB_8888)
    }

    /** Sliding-window average along one axis, clamping at the edges. */
    private fun boxBlur(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val step = if (horizontal) 1 else w
        val window = 2 * r + 1
        for (line in 0 until lines) {
            val base = if (horizontal) line * w else line
            var a = 0; var rr = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val c = src[base + i.coerceIn(0, len - 1) * step]
                a += c ushr 24; rr += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF
            }
            for (i in 0 until len) {
                dst[base + i * step] = ((a / window) shl 24) or ((rr / window) shl 16) or ((g / window) shl 8) or (b / window)
                val add = src[base + (i + r + 1).coerceAtMost(len - 1) * step]
                val sub = src[base + (i - r).coerceAtLeast(0) * step]
                a += (add ushr 24) - (sub ushr 24)
                rr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                g += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                b += (add and 0xFF) - (sub and 0xFF)
            }
        }
    }
}

/** Fills its bounds with the blurred wallpaper (Launchpad, lock screen). */
@Composable
fun BlurredFill(modifier: Modifier = Modifier.fillMaxSize()) {
    val bitmap = LocalBlurredWallpaper.current?.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High,
            colorFilter = Saturate
        )
    } else {
        Box(modifier.background(Color(0xFF1C1C1E)))
    }
}

/**
 * A translucent material: the blurred wallpaper behind it, a [tint], and a hairline outline.
 * Use `Modifier.matchParentSize()` with empty [content] to make it a background layer (the dock
 * does this so magnified icons can rise above its edge).
 */
@Composable
fun FrostedBox(
    modifier: Modifier = Modifier,
    radius: Dp = 0.dp,
    tint: Color = Color.Transparent,
    border: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val shape = RoundedCornerShape(radius)
    val bitmap = LocalBlurredWallpaper.current?.value
    val window = LocalWindowInfo.current.containerSize
    var position by remember { mutableStateOf(IntOffset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { position = it.positionInWindow().let { p -> IntOffset(p.x.toInt(), p.y.toInt()) } }
            .clip(shape)
            .then(if (border) Modifier.border(1.dp, Hairline, shape) else Modifier)
    ) {
        Box(Modifier.matchParentSize()) {
            if (bitmap != null && window.width > 0) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    // Lay the window-sized copy out at the surface's negative window position so it
                    // lines up with the real wallpaper. (requiredSize would centre it instead.)
                    // `position` is read in the placement block, so a moving surface does not recompose.
                    modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(Constraints.fixed(window.width, window.height))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(-position.x, -position.y)
                        }
                    },
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    colorFilter = Saturate
                )
            }
            Box(Modifier.matchParentSize().background(tint))
        }
        content()
    }
}

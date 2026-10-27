package com.meomeo.tvlauncher.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.meomeo.tvlauncher.R
import com.meomeo.tvlauncher.ui.components.SHOW_HERO

/**
 * Wallpaper, ambient aura and readability scrims. Extracted so it can be drawn twice: once as the
 * screen background and once (blurred) inside the dock capsule to fake a backdrop blur.
 */
@Composable
fun WallpaperBackdrop(
    uiState: MainUiState,
    wallpaperRes: Int,
    ambientGlow: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 1. Wallpaper Subtle Texture (Supports Google Photos album & presets)
        if (uiState.selectedWallpaper == "wallpaper_google_photos") {
            val currentPhoto = uiState.googlePhotos.getOrNull(uiState.currentGooglePhotoIndex)
                ?: uiState.googlePhotos.firstOrNull()

            Crossfade(
                targetState = currentPhoto,
                animationSpec = tween(1200),
                label = "googlePhotosCrossfade"
            ) { photoFile ->
                if (photoFile != null && photoFile.exists()) {
                    val (imgWidth, imgHeight) = remember(photoFile.absolutePath) {
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(photoFile.absolutePath, options)
                        options.outWidth to options.outHeight
                    }
                    val aspectRatio = if (imgHeight > 0) imgWidth.toFloat() / imgHeight.toFloat() else 1.77f
                    val isWidescreen = aspectRatio >= 1.60f

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!isWidescreen) {
                            // 1. Ambient blurred backdrop: fills widescreen TV sides seamlessly with matching colors
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photoFile)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(32.dp),
                                contentScale = ContentScale.Crop,
                                alpha = 0.50f
                            )
                            // 2. Crisp foreground photo: full image at true aspect ratio, never cropped, high-quality filtering
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photoFile)
                                    .build(),
                                contentDescription = "Hình nền Google Photos",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                filterQuality = FilterQuality.High,
                                alpha = 1.0f
                            )
                        } else {
                            // Widescreen photo fills entire screen with high-quality filter
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photoFile)
                                    .build(),
                                contentDescription = "Hình nền Google Photos",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.High,
                                alpha = 1.0f
                            )
                        }
                    }
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.wallpaper_sunset),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.55f
                    )
                }
            }
        } else {
            Image(
                painter = painterResource(id = wallpaperRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.55f
            )
        }

        // 2. Dynamic Ambient Aura (Only for built-in abstract wallpapers, NOT for personal Google Photos!)
        if (uiState.selectedWallpaper != "wallpaper_google_photos") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ambientGlow.copy(alpha = 0.38f),
                                Color.Transparent
                            ),
                            radius = 1400f
                        )
                    )
            )
        }

        // 3. Ambient Top & Bottom Scrim (Film-grade vignette for crystal-clear readability)
        val vignetteBrush = if (uiState.selectedWallpaper == "wallpaper_google_photos") {
            Brush.verticalGradient(
                0.0f to Color.Black.copy(alpha = 0.72f),
                0.12f to Color.Black.copy(alpha = 0.45f),
                0.26f to Color.Transparent,
                0.68f to Color.Transparent,
                1.0f to Color.Black.copy(alpha = 0.70f)
            )
        } else {
            Brush.verticalGradient(
                0.0f to Color.Black.copy(alpha = 0.65f),
                0.14f to Color.Black.copy(alpha = 0.35f),
                0.28f to Color.Transparent,
                0.68f to Color.Transparent,
                1.0f to Color.Black.copy(alpha = 0.75f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(vignetteBrush)
        )

        if (SHOW_HERO) {
            // Left-side scrim keeps the hero text legible over busy photo wallpapers.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0.0f to Color.Black.copy(alpha = 0.55f),
                            0.55f to Color.Transparent
                        )
                    )
            )
        }
    }
}

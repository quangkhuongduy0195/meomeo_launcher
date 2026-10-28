package com.meomeo.tvlauncher.ui.components

import android.app.ActivityManager
import android.content.Context
import android.graphics.BitmapFactory
import android.view.KeyEvent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.meomeo.tvlauncher.R
import com.meomeo.tvlauncher.model.AppInfo
import com.meomeo.tvlauncher.ui.MainUiState
import com.meomeo.tvlauncher.ui.theme.DockRadius
import com.meomeo.tvlauncher.ui.theme.DockTint
import com.meomeo.tvlauncher.ui.theme.Label
import com.meomeo.tvlauncher.ui.theme.MacBlue
import com.meomeo.tvlauncher.ui.theme.MenuBarTint
import com.meomeo.tvlauncher.ui.theme.Separator
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import java.util.Locale

private val ViLocale = Locale("vi", "VN")

// ---------------------------------------------------------------------------------------------
// Desktop wallpaper
// ---------------------------------------------------------------------------------------------

/** What the wallpaper and its blurred twin are loaded from. */
fun wallpaperData(uiState: MainUiState, wallpaperRes: Int): Any =
    if (uiState.selectedWallpaper == "wallpaper_google_photos") {
        uiState.googlePhotos.getOrNull(uiState.currentGooglePhotoIndex)?.takeIf { it.exists() }
            ?: uiState.googlePhotos.firstOrNull()?.takeIf { it.exists() }
            ?: R.drawable.wallpaper_sunset
    } else {
        wallpaperRes
    }

/** The photo the slideshow shows next, so it can be decoded before it is needed. Null when there is none. */
fun nextWallpaperData(uiState: MainUiState): Any? {
    if (uiState.selectedWallpaper != "wallpaper_google_photos" || uiState.googlePhotos.size < 2) return null
    return uiState.googlePhotos.getOrNull((uiState.currentGooglePhotoIndex + 1) % uiState.googlePhotos.size)
        ?.takeIf { it.exists() }
}

/** Decode size of the wallpaper: the window, or two thirds of it on low-RAM TVs. */
private fun wallpaperSize(context: Context, window: IntSize): IntSize {
    val lowRam = (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true
    val factor = if (lowRam) 0.67f else 1f
    return IntSize((window.width * factor).toInt().coerceAtLeast(1), (window.height * factor).toInt().coerceAtLeast(1))
}

/**
 * Decodes [data] into at most [size], and never larger than the file itself: Coil would otherwise
 * stretch a small picture up to [size], which is blurry before it is even drawn.
 */
private suspend fun wallpaperRequest(context: Context, data: Any, size: IntSize): ImageRequest {
    val source = withContext(Dispatchers.IO) { sourceSize(context, data) }
    val target = if (source == IntSize.Zero) size
    else IntSize(minOf(size.width, source.width), minOf(size.height, source.height))
    return ImageRequest.Builder(context)
        .data(data)
        // An explicit size gives the decode a fixed cache key, so the prefetch below is a cache hit.
        .size(target.width, target.height)
        .scale(Scale.FIT)
        .build()
}

/**
 * The desktop picture. Widescreen images fill the screen; portrait photos are shown whole in the
 * middle over their own blurred copy, so faces are never cropped away.
 *
 * Changing the picture never touches the UI thread with decoding: the new image is decoded (and,
 * as a hardware bitmap, uploaded to the GPU) in the background, the old one stays on screen until
 * then, and only the finished image is cross-faded in. [nextData] is decoded ahead of time so the
 * switch is normally instant.
 */
@Composable
fun DesktopWallpaper(data: Any, nextData: Any?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val size = wallpaperSize(context, LocalWindowInfo.current.containerSize)

    val wallpaper by produceState<LoadedWallpaper?>(null, data, size) {
        if (size.width <= 1) return@produceState
        val result = context.imageLoader.execute(wallpaperRequest(context, data, size))
        // Keep the previous picture if the new one fails to load.
        val image = (result as? SuccessResult)?.drawable?.toBitmap()?.asImageBitmap() ?: return@produceState
        value = LoadedWallpaper(image, withContext(Dispatchers.IO) { sourceSize(context, data) })
    }

    LaunchedEffect(nextData, size) {
        if (nextData != null && size.width > 1) context.imageLoader.enqueue(wallpaperRequest(context, nextData, size))
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        val window = LocalWindowInfo.current.containerSize
        Crossfade(targetState = wallpaper, animationSpec = tween(450), label = "wallpaper") { loaded ->
            if (loaded != null) WallpaperImage(loaded, window)
        }
    }
}

/** A decoded wallpaper plus the pixel size of the file it came from (0 when unknown, e.g. a vector). */
private class LoadedWallpaper(val image: ImageBitmap, val sourceSize: IntSize)

/** Reads only the header of [data] (a File or a drawable id) to get its real pixel size. */
private fun sourceSize(context: Context, data: Any): IntSize {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    when (data) {
        is File -> BitmapFactory.decodeFile(data.path, options)
        is Int -> BitmapFactory.decodeResource(context.resources, data, options)
        else -> return IntSize.Zero
    }
    return if (options.outWidth > 0 && options.outHeight > 0) IntSize(options.outWidth, options.outHeight) else IntSize.Zero
}

/** Most a picture may be enlarged to fill the screen before it starts to look soft. */
private const val MaxFillUpscale = 1.25f

/**
 * A picture fills the screen only when that is safe: its shape is close to the screen's (little is
 * cropped) and it has enough pixels (little is enlarged). Tall, near-square or small pictures are
 * shown whole at their own size over a blurred copy instead.
 */
@Composable
private fun WallpaperImage(wallpaper: LoadedWallpaper, window: IntSize) {
    val image = wallpaper.image
    val source = wallpaper.sourceSize
    val wideEnough = image.width.toFloat() / image.height >= 1.3f
    val sharpEnough = source == IntSize.Zero || window.width <= 0 ||
        maxOf(window.width.toFloat() / source.width, window.height.toFloat() / source.height) <= MaxFillUpscale
    if (wideEnough && sharpEnough) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(0f, -0.3f)
        )
    } else {
        Box(Modifier.fillMaxSize()) {
            BlurredFill()
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
            // Inside never enlarges, so a small picture stays sharp instead of being stretched.
            Image(image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Inside)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Menu bar
// ---------------------------------------------------------------------------------------------

/**
 * The strip along the top: the focused app's name on the left, status buttons and the clock on
 * the right.
 */
@Composable
fun MenuBar(
    activeName: String,
    timerLabel: String?,
    onWifi: () -> Unit,
    onTimer: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    updateAvailable: Boolean = false
) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000L - value % 60_000L + 50L)
        }
    }
    val clock = remember(now) {
        SimpleDateFormat("EEE d 'Thg' M   HH:mm", ViLocale).format(Date(now)).replaceFirstChar { it.titlecase(ViLocale) }
    }

    Box(modifier.fillMaxWidth().height(36.dp)) {
        FrostedBox(Modifier.matchParentSize(), radius = 0.dp, tint = MenuBarTint, border = false)
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(1.dp).background(Separator))
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Rounded.Pets, contentDescription = "MeoMeo", tint = Label, modifier = Modifier.size(18.dp))
                Text("MeoMeo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Label)
                Text(activeName, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MenuBarButton(Icons.Rounded.Wifi, "Wi-Fi", onWifi)
                MenuBarButton(Icons.Rounded.Timer, timerLabel ?: "Hẹn giờ", onTimer, showLabel = timerLabel != null)
                MenuBarButton(Icons.Rounded.Settings, "Cài đặt", onSettings, badge = updateAvailable)
                Text(clock, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Label, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}

@Composable
private fun MenuBarButton(icon: ImageVector, label: String, onClick: () -> Unit, showLabel: Boolean = false, badge: Boolean = false) {
    FocusSurface(
        onClick = onClick,
        modifier = Modifier.height(26.dp),
        cornerRadius = 6.dp,
        focusedColor = Color.White.copy(alpha = 0.32f),
        ring = true
    ) { _ ->
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                Icon(icon, contentDescription = label, tint = Label, modifier = Modifier.size(17.dp))
                // A blue dot says "there is something new in here" without interrupting anyone.
                if (badge) Box(Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-2).dp).size(8.dp).clip(CircleShape).background(MacBlue))
            }
            if (showLabel) Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Label, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Dock
// ---------------------------------------------------------------------------------------------

/** Resting size of a dock icon. Focused icons magnify from here. */
val DockIconSize: Dp = 56.dp

/** Magnification of the item at [distance] slots from the focused one (0 = the focused item). */
fun dockMagnification(distance: Int): Float = when (distance) {
    0 -> 1.55f
    1 -> 1.28f
    2 -> 1.1f
    else -> 1f
}

/**
 * One slot in the dock. The slot's layout width grows with [magnification] so neighbours slide
 * apart, while the icon scales up from its bottom edge (so it rises out of the dock). A name
 * bubble floats above the focused icon.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DockSlot(
    label: String,
    magnification: Float,
    onFocusChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = magnification,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "dockMagnify"
    )

    Box(
        modifier = modifier
            .width(DockIconSize * scale)
            .height(DockIconSize)
            .zIndex(if (isFocused) 20f else 1f)
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusChange(it.isFocused)
            }
            .onKeyEvent { event ->
                if (onLongClick != null &&
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MENU &&
                    event.nativeKeyEvent.action == KeyEvent.ACTION_UP
                ) {
                    onLongClick()
                    true
                } else {
                    false
                }
            }
            // combinedClickable is itself the focus target; see FocusSurface.
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .size(DockIconSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
        ) { icon() }

        if (isFocused) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .wrapContentSize(Alignment.BottomCenter, unbounded = true)
                    .offset(y = -(DockIconSize * scale) - 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xE62C2C2E))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Label, maxLines = 1)
            }
        }
    }
}

/** A dock slot holding an app (or an empty placeholder). */
@Composable
fun DockAppSlot(
    app: AppInfo?,
    magnification: Float,
    onFocusGain: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(app?.packageName) { if (app != null) drawableToBitmap(app.icon ?: app.banner) else null }
    DockSlot(
        label = app?.label ?: "Trống",
        magnification = magnification,
        onFocusChange = { if (it) onFocusGain() },
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
    ) {
        if (app != null && bitmap != null) AppIconTile(bitmap, app.label, Modifier.fillMaxSize())
        else EmptyIconTile(Modifier.fillMaxSize())
    }
}

/** The last dock slot: opens the full app list. */
@Composable
fun DockLaunchpadSlot(
    magnification: Float,
    onFocusGain: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DockSlot(
        label = "Launchpad",
        magnification = magnification,
        onFocusChange = { if (it) onFocusGain() },
        onClick = onClick,
        onLongClick = null,
        modifier = modifier
    ) { LaunchpadAppIcon(Modifier.fillMaxSize()) }
}

/**
 * The frosted dock capsule. It is a background layer behind [content], not a clip on it, so
 * magnified icons can rise above its top edge.
 */
@Composable
fun Dock(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier) {
        FrostedBox(Modifier.matchParentSize(), radius = DockRadius, tint = DockTint)
        Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) { content() }
    }
}

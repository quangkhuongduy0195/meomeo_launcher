package com.meomeo.tvlauncher.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meomeo.tvlauncher.model.AppInfo
import com.meomeo.tvlauncher.ui.theme.AppIconShape
import com.meomeo.tvlauncher.ui.theme.Label
import com.meomeo.tvlauncher.ui.theme.LabelSecondary
import com.meomeo.tvlauncher.ui.theme.MacBlue
import com.meomeo.tvlauncher.ui.theme.StarYellow
import com.meomeo.tvlauncher.ui.theme.SystemGreen
import com.meomeo.tvlauncher.ui.theme.SystemOrange
import com.meomeo.tvlauncher.ui.theme.SystemPink
import com.meomeo.tvlauncher.ui.theme.SystemPurple
import com.meomeo.tvlauncher.ui.theme.SystemTeal
import com.meomeo.tvlauncher.ui.theme.SystemYellow

fun drawableToBitmap(drawable: Drawable?): Bitmap? {
    if (drawable == null) return null
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

/** True when the middle of each edge is (nearly) opaque, i.e. the icon has its own background. */
private fun Bitmap.hasOpaqueEdges(): Boolean {
    if (width < 8 || height < 8) return false
    val inset = (width * 0.04f).toInt().coerceAtLeast(1)
    val points = listOf(
        inset to height / 2,
        width - 1 - inset to height / 2,
        width / 2 to inset,
        width / 2 to height - 1 - inset
    )
    val alphaSum = points.sumOf { (x, y) -> (getPixel(x, y) ushr 24).toInt() }
    return alphaSum / points.size >= 200
}

/**
 * An app icon as a macOS squircle. Icons that already have their own background are drawn full
 * bleed; glyph-only icons (Play Store, Settings...) get a light tile, so every icon has a body.
 */
@Composable
fun AppIconTile(bitmap: Bitmap, label: String, modifier: Modifier = Modifier) {
    val hasOwnBackground = remember(bitmap) { bitmap.hasOpaqueEdges() }
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    if (hasOwnBackground) {
        Image(image, label, modifier.clip(AppIconShape), contentScale = ContentScale.Fit)
    } else {
        Box(
            modifier = modifier
                .clip(AppIconShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFF4F4F6), Color(0xFFD8D8DE)))),
            contentAlignment = Alignment.Center
        ) {
            Image(image, label, Modifier.fillMaxSize().padding(fraction(0.16f)), contentScale = ContentScale.Fit)
        }
    }
}

// Padding as a fraction needs a size; icons here are small and fixed, so a flat value is close enough.
private fun fraction(@Suppress("UNUSED_PARAMETER") f: Float) = 8.dp

/** The Launchpad icon: a silver squircle with a 3x3 grid of coloured dots. */
@Composable
fun LaunchpadAppIcon(modifier: Modifier = Modifier) {
    val dots = listOf(
        MacBlue, SystemGreen, SystemOrange,
        SystemPink, SystemPurple, SystemYellow,
        SystemTeal, SystemOrange, SystemGreen
    )
    Box(
        modifier = modifier
            .clip(AppIconShape)
            .background(Brush.verticalGradient(listOf(Color(0xFFBEBEC6), Color(0xFF6E6E78))))
            .drawBehind {
                val cell = size.width / 4f
                val r = cell * 0.30f
                dots.forEachIndexed { i, c ->
                    drawCircle(c, r, Offset(cell * (1 + i % 3), cell * (1 + i / 3)))
                }
            }
    )
}

/** An empty dock/picker slot. */
@Composable
fun EmptyIconTile(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(AppIconShape).background(Color.White.copy(alpha = 0.26f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "Gán ứng dụng", tint = Label.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
    }
}

private val IconLabelShadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, 2f), 6f)

/**
 * One cell of a Launchpad grid: icon with its name underneath. Focus scales the icon and lays a
 * soft highlight behind it. [slotNumber] and [selected] are for the dock customizer.
 */
@Composable
fun LaunchpadIcon(
    app: AppInfo?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onFocusGain: () -> Unit = {},
    selected: Boolean = false,
    slotNumber: Int? = null
) {
    val bitmap = remember(app?.packageName) { if (app != null) drawableToBitmap(app.icon ?: app.banner) else null }

    FocusSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        onFocusChange = { if (it) onFocusGain() },
        modifier = modifier,
        cornerRadius = 22.dp,
        color = Color.Transparent,
        focusedColor = Color.White.copy(alpha = 0.22f),
        focusScale = 1.06f,
        ring = true
    ) { focused ->
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                // Badges hang off the icon's corners, so they never cover its middle.
                Box(Modifier.fillMaxWidth(0.62f).aspectRatio(1f)) {
                    if (app != null && bitmap != null) AppIconTile(bitmap, app.label, Modifier.fillMaxSize())
                    else EmptyIconTile(Modifier.fillMaxSize())
                    if (app?.isFavorite == true) {
                        Icon(
                            Icons.Rounded.Star, contentDescription = "Yêu thích", tint = StarYellow,
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp).size(14.dp)
                        )
                    }
                    if (selected) {
                        Icon(
                            Icons.Rounded.CheckCircle, contentDescription = "Đang chọn", tint = Label,
                            modifier = Modifier.align(Alignment.TopStart).offset(x = (-6).dp, y = (-6).dp).size(18.dp)
                        )
                    }
                    if (slotNumber != null) {
                        Box(
                            Modifier.align(Alignment.TopStart).offset(x = (-7).dp, y = (-7).dp).size(18.dp)
                                .clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$slotNumber", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Label)
                        }
                    }
                }
            }
            Text(
                text = app?.label ?: "Trống",
                style = TextStyle(shadow = IconLabelShadow),
                fontSize = 12.sp,
                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
                color = Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
    }
}

package com.meomeo.tvlauncher.ui.components

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.meomeo.tvlauncher.model.AppInfo
import com.meomeo.tvlauncher.ui.theme.Accent
import com.meomeo.tvlauncher.ui.theme.AppIconShape
import com.meomeo.tvlauncher.ui.theme.ColorExtractor
import com.meomeo.tvlauncher.ui.theme.StarYellow
import com.meomeo.tvlauncher.ui.theme.glassIdleColor

/** Resting size of a dock icon. Focused icons magnify from here, macOS style. */
val DockIconSize: Dp = 56.dp

/** Magnification of the item at [distance] slots from the focused one (0 = the focused item). */
fun dockMagnification(distance: Int): Float = when (distance) {
    0 -> 1.6f
    1 -> 1.3f
    2 -> 1.1f
    else -> 1f
}

/**
 * One slot in the dock. The slot's layout width grows with [magnification] so neighbours slide
 * apart, while the icon itself scales up from its bottom edge (so it rises out of the dock). A
 * name pill floats above the focused icon.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockSlot(
    label: String,
    magnification: Float,
    onFocusChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = magnification,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
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
            .onKeyEvent { keyEvent ->
                // Remote MENU key behaves like a long press.
                if (onLongClick != null &&
                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MENU &&
                    keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP
                ) {
                    onLongClick()
                    true
                } else {
                    false
                }
            }
            // combinedClickable is itself the focus target; a separate .focusable() here would
            // steal focus from it, so OK/Enter would never reach the click handler.
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
                },
            content = icon
        )

        if (isFocused) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .wrapContentSize(Alignment.BottomCenter, unbounded = true)
                    .offset(y = -(DockIconSize * scale) - 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
fun DockCard(
    slotIndex: Int,
    app: AppInfo?,
    magnification: Float,
    onFocusGain: (AppInfo?, Color) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconBitmap = remember(app?.packageName) {
        if (app != null) drawableToBitmap(app.icon ?: app.banner) else null
    }
    val dominantColor = remember(app?.packageName, iconBitmap) {
        ColorExtractor.getDominantColor(app?.packageName ?: "default", iconBitmap)
    }

    DockSlot(
        label = app?.label ?: "Trống",
        magnification = magnification,
        onFocusChange = { focused -> if (focused) onFocusGain(app, dominantColor) },
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
    ) {
        if (app != null && iconBitmap != null) {
            AppDockIcon(bitmap = iconBitmap, label = app.label)
            if (app.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(16.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Yêu thích", tint = StarYellow, modifier = Modifier.size(10.dp))
                }
            }
        } else {
            GlassDockTile {
                Icon(Icons.Rounded.Add, contentDescription = "Gán app", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
fun AllAppsDockCard(
    magnification: Float,
    onFocusGain: (Color) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DockSlot(
        label = "Tất cả ứng dụng",
        magnification = magnification,
        onFocusChange = { focused -> if (focused) onFocusGain(Accent) },
        onClick = onClick,
        onLongClick = null,
        modifier = modifier
    ) {
        // Launchpad-style tile
        GlassDockTile {
            Icon(Icons.Rounded.Apps, contentDescription = "Tất cả ứng dụng", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

/** An app icon drawn straight onto the dock, rounded like a macOS icon. */
@Composable
private fun AppDockIcon(bitmap: Bitmap, label: String) {
    // Icons that are just a glyph on transparency (Play Store, Settings...) float awkwardly on the
    // glass, so give them a tile. Icons with their own background are drawn full-bleed.
    val hasOwnBackground = remember(bitmap) { bitmap.hasOpaqueEdges() }
    if (hasOwnBackground) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = label,
            modifier = Modifier.fillMaxSize().clip(AppIconShape),
            contentScale = ContentScale.Fit
        )
    } else {
        GlassDockTile {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = label,
                modifier = Modifier.fillMaxSize().padding(9.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun GlassDockTile(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(AppIconShape)
            .background(glassIdleColor()),
        contentAlignment = Alignment.Center,
        content = content
    )
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

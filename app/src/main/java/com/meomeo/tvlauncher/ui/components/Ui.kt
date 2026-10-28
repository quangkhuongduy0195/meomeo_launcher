package com.meomeo.tvlauncher.ui.components

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.meomeo.tvlauncher.ui.theme.Danger
import com.meomeo.tvlauncher.ui.theme.Label
import com.meomeo.tvlauncher.ui.theme.LabelSecondary
import com.meomeo.tvlauncher.ui.theme.LabelTertiary
import com.meomeo.tvlauncher.ui.theme.MacBlue
import com.meomeo.tvlauncher.ui.theme.RowFill
import com.meomeo.tvlauncher.ui.theme.RowRadius
import com.meomeo.tvlauncher.ui.theme.Separator
import com.meomeo.tvlauncher.ui.theme.SystemGreen
import com.meomeo.tvlauncher.ui.theme.TrafficGreen
import com.meomeo.tvlauncher.ui.theme.TrafficRed
import com.meomeo.tvlauncher.ui.theme.TrafficYellow
import com.meomeo.tvlauncher.ui.theme.WindowRadius
import com.meomeo.tvlauncher.ui.theme.WindowTint

// ---------------------------------------------------------------------------------------------
// Focusable surface. On a TV remote "focused" must be obvious, so the accent colour fills the
// whole row like a selected item in a macOS menu or sidebar.
// ---------------------------------------------------------------------------------------------

/**
 * A focusable, clickable surface. Focus cross-fades [color] to [focusedColor], optionally scales
 * it and draws a white ring. The remote MENU key acts as a long press.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FocusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onFocusChange: (Boolean) -> Unit = {},
    cornerRadius: Dp = RowRadius,
    color: Color = Color.Transparent,
    focusedColor: Color = MacBlue,
    focusScale: Float = 1f,
    ring: Boolean = false,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.(focused: Boolean) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(if (focused) 1f else 0f, tween(110), label = "focus")
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                val s = 1f + (focusScale - 1f) * progress
                scaleX = s
                scaleY = s
            }
            .onFocusChanged {
                focused = it.isFocused
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
            .clip(shape)
            .drawBehind { drawRect(lerp(color, focusedColor, progress)) }
            .then(if (ring && focused) Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), shape) else Modifier)
            // combinedClickable is itself the focus target; a separate focusable() would steal
            // focus from it and OK/Enter would never reach the click handler.
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = contentAlignment
    ) {
        content(focused)
    }
}

// ---------------------------------------------------------------------------------------------
// System Settings-style rows
// ---------------------------------------------------------------------------------------------

/** Rounded container that groups rows, like a section of macOS System Settings. */
@Composable
fun InsetGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RowRadius + 4.dp))
            .background(RowFill)
            .padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
        content = content
    )
}

/** Coloured rounded square with a white glyph: the icon style of System Settings. */
@Composable
fun IconTile(icon: ImageVector, color: Color, modifier: Modifier = Modifier, size: Dp = 30.dp) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(size * 0.26f)).background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
    }
}

/** A list row: icon tile, title, optional subtitle, trailing content. Fills with the accent when focused. */
@Composable
fun MacRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    danger: Boolean = false,
    chevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) { focused ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconTile(icon, iconColor)
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (danger && !focused) Danger else Label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = if (focused) Label.copy(alpha = 0.8f) else LabelSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing?.invoke()
            if (chevron) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = if (focused) Label else LabelTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** A [MacRow] with the green macOS switch. */
@Composable
fun ToggleRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    MacRow(
        icon = icon,
        iconColor = iconColor,
        title = title,
        subtitle = subtitle,
        onClick = onToggle,
        modifier = modifier,
        trailing = { MacSwitch(checked) }
    )
}

@Composable
fun MacSwitch(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 42.dp, height = 25.dp)
            .clip(CircleShape)
            .background(if (checked) SystemGreen else Color.White.copy(alpha = 0.28f))
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.size(21.dp).clip(CircleShape).background(Color.White))
    }
}

/** Push button. [primary] is the blue default button of a macOS alert. */
@Composable
fun MacButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    icon: ImageVector? = null
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier,
        cornerRadius = 9.dp,
        color = if (primary) MacBlue else Color.White.copy(alpha = 0.16f),
        focusedColor = if (primary) lerp(MacBlue, Color.White, 0.25f) else Color.White.copy(alpha = 0.34f),
        focusScale = 1.03f,
        ring = true
    ) { _ ->
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = if (danger) Danger else Label, modifier = Modifier.size(18.dp))
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (danger) Danger else Label,
                maxLines = 1
            )
        }
    }
}

/** Small translucent label, e.g. "5 ứng dụng". */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Label, maxLines = 1)
    }
}

/** Section caption above an [InsetGroup]. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = LabelSecondary,
        modifier = modifier.padding(start = 6.dp, bottom = 6.dp, top = 12.dp)
    )
}

/** The three window buttons. Decorative: Back closes a window on a remote. */
@Composable
fun TrafficLights(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(TrafficRed, TrafficYellow, TrafficGreen).forEach {
            Box(Modifier.size(12.dp).clip(CircleShape).background(it))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Dialog scaffolds. A Compose Dialog is its own window, so each one paints its own material.
// ---------------------------------------------------------------------------------------------

private val Scrim = Color.Black.copy(alpha = 0.30f)

private val DialogWindow = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)

/** Flips to true one frame after the dialog appears, so it can animate in. */
@Composable
private fun rememberEntered(): Boolean {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    return entered
}

/** A macOS window in the middle of the screen: title bar with traffic lights, then [content]. */
@Composable
fun MacWindow(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    width: Dp = 480.dp,
    maxHeight: Dp = 470.dp,
    contentAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogWindow) {
        val entered = rememberEntered()
        Box(Modifier.fillMaxSize().background(Scrim), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visible = entered,
                enter = fadeIn(tween(150)) + scaleIn(tween(200), initialScale = 0.96f)
            ) {
                FrostedBox(
                    modifier = modifier.width(width).heightIn(max = maxHeight),
                    radius = WindowRadius,
                    tint = WindowTint
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 16.dp)) {
                            TrafficLights(Modifier.align(Alignment.CenterStart))
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Label.copy(alpha = 0.9f),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Separator))
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalAlignment = contentAlignment,
                            content = content
                        )
                    }
                }
            }
        }
    }
}

/** A small context menu, like the one a right-click opens. */
@Composable
fun MenuPopup(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 320.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogWindow) {
        val entered = rememberEntered()
        Box(Modifier.fillMaxSize().background(Scrim), contentAlignment = Alignment.Center) {
            AnimatedVisibility(visible = entered, enter = fadeIn(tween(120)) + scaleIn(tween(150), initialScale = 0.94f)) {
                FrostedBox(modifier = modifier.width(width), radius = 14.dp, tint = WindowTint) {
                    Column(Modifier.padding(6.dp), content = content)
                }
            }
        }
    }
}

/** A full-screen page over the blurred wallpaper, like Launchpad. */
@Composable
fun LaunchpadDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogWindow) {
        val entered = rememberEntered()
        Box(Modifier.fillMaxSize()) {
            BlurredFill()
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)))
            AnimatedVisibility(visible = entered, enter = fadeIn(tween(160))) {
                Column(
                    modifier = modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content
                )
            }
        }
    }
}

/** Divider line inside menus. */
@Composable
fun MenuSeparator() {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp).height(1.dp).background(Separator))
}

/** Centered subtitle text used by alert-style windows. */
@Composable
fun AlertText(title: String, message: String?, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Label, textAlign = TextAlign.Center)
        if (message != null) {
            Spacer(Modifier.height(4.dp))
            Text(message, fontSize = 13.sp, color = LabelSecondary, textAlign = TextAlign.Center)
        }
    }
}

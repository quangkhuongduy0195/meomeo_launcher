package com.meomeo.tvlauncher.ui.components

import com.meomeo.tvlauncher.ui.theme.liquidGlass
import com.meomeo.tvlauncher.ui.theme.QuicksandFamily
import com.meomeo.tvlauncher.ui.theme.glassFill
import android.view.KeyEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meomeo.tvlauncher.ui.theme.Accent
import com.meomeo.tvlauncher.ui.theme.AccentGlow
import com.meomeo.tvlauncher.ui.theme.TextPrimary
import com.meomeo.tvlauncher.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TopHeader(
    modifier: Modifier = Modifier,
    settingsFocusRequester: FocusRequester? = null,
    dockFocusRequester: FocusRequester? = null,
    onOpenWifi: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }
    var currentDate by remember {
        mutableStateOf(SimpleDateFormat("EEEE, d 'tháng' M", Locale("vi", "VN")).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE, d 'tháng' M", Locale("vi", "VN")).format(now)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("vi", "VN")) else it.toString() }
            delay(1000)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Time & Date (With crisp drop shadows for maximum legibility on bright backgrounds)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currentTime,
                style = TextStyle(
                    fontFamily = QuicksandFamily,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        offset = Offset(0f, 3f),
                        blurRadius = 10f
                    )
                )
            )
            Spacer(modifier = Modifier.width(20.dp))
            Box(
                modifier = Modifier
                    .size(width = 2.dp, height = 32.dp)
                    .background(
                        Color.White.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = currentDate,
                    style = TextStyle(
                        fontFamily = QuicksandFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.85f),
                            offset = Offset(0f, 2f),
                            blurRadius = 8f
                        )
                    )
                )
                Text(
                    text = "MeoMeo TV",
                    style = TextStyle(
                        fontFamily = QuicksandFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.75f),
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.85f),
                            offset = Offset(0f, 2f),
                            blurRadius = 8f
                        )
                    )
                )
            }
        }

        // Right: Fast Quick Actions (Remote Focusable)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderIconButton(
                icon = Icons.Rounded.Wifi,
                contentDescription = "Wi-Fi",
                onClick = onOpenWifi,
                modifier = Modifier.focusProperties {
                    if (dockFocusRequester != null) down = dockFocusRequester
                }
            )
            HeaderIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = "Cài đặt",
                onClick = onOpenSettings,
                modifier = (if (settingsFocusRequester != null) Modifier.focusRequester(settingsFocusRequester) else Modifier)
                    .focusProperties {
                        if (dockFocusRequester != null) down = dockFocusRequester
                    }
            )
        }
    }
}

@Composable
fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1.0f,
        label = "headerButtonScale"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color.White.copy(alpha = 0.92f),
        label = "headerButtonIcon"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(46.dp)
            .liquidGlass(
                shape = RoundedCornerShape(14.dp),
                isFocused = isFocused,
                blurRadius = 6.dp,
                lensHeight = 6.dp,
                lensAmount = 12.dp,
                withShadow = false
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if ((keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                     keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) &&
                    keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}


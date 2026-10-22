package com.meomeo.tvlauncher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.meomeo.tvlauncher.ui.theme.Accent
import com.meomeo.tvlauncher.ui.theme.AccentGlow
import com.meomeo.tvlauncher.ui.theme.BgCard
import com.meomeo.tvlauncher.ui.theme.TextPrimary
import com.meomeo.tvlauncher.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    includeSideload: Boolean,
    onToggleSideload: () -> Unit,
    onOpenDockCustomizer: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    googlePhotosCount: Int = 0,
    isGooglePhotosSyncing: Boolean = false,
    onSyncGooglePhotos: () -> Unit = {},
    onOpenAndroidSettings: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        try {
            firstItemFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .width(460.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(BgCard)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(26.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = AccentGlow,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Cài đặt MeoMeo Launcher",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Customize 7 Dock Apps
                MenuDialogItem(
                    icon = Icons.Rounded.Apps,
                    iconTint = AccentGlow,
                    text = "Tùy chỉnh 7 ứng dụng thanh đáy",
                    onClick = {
                        onOpenDockCustomizer()
                    },
                    modifier = Modifier.focusRequester(firstItemFocusRequester)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Wallpaper selector
                MenuDialogItem(
                    icon = Icons.Rounded.Wallpaper,
                    iconTint = Color(0xFFF43F5E),
                    text = "Đổi hình nền Google TV",
                    onClick = {
                        onOpenWallpaperPicker()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sync Google Photos immediately
                MenuDialogItem(
                    icon = Icons.Rounded.CloudSync,
                    iconTint = Color(0xFF10B981),
                    text = if (isGooglePhotosSyncing) "Đang tải ảnh Google Photos..." else if (googlePhotosCount > 0) "Đồng bộ lại Google Photos ($googlePhotosCount ảnh)" else "Đồng bộ Album Google Photos",
                    onClick = {
                        onSyncGooglePhotos()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sideload Toggle Item
                SettingToggleItem(
                    icon = Icons.Rounded.PhoneAndroid,
                    title = "Hiện app di động (Sideload)",
                    subtitle = "Quét thêm các app không có banner TV",
                    isChecked = includeSideload,
                    onToggle = onToggleSideload
                )

                Spacer(modifier = Modifier.height(10.dp))

                // System Settings
                MenuDialogItem(
                    icon = Icons.Rounded.Settings,
                    iconTint = AccentGlow,
                    text = "Mở Cài đặt Android TV",
                    onClick = {
                        onOpenAndroidSettings()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Wifi Settings
                MenuDialogItem(
                    icon = Icons.Rounded.Wifi,
                    iconTint = AccentGlow,
                    text = "Mở Cài đặt Wi-Fi",
                    onClick = {
                        onOpenWifiSettings()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "MeoMeo Launcher v1.0.0 • Thiết kế tối giản cho Google TV",
                    fontSize = 11.sp,
                    color = TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

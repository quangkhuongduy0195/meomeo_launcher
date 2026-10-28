package com.meomeo.tvlauncher.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meomeo.tvlauncher.data.AppRepository
import com.meomeo.tvlauncher.ui.theme.LabelTertiary
import com.meomeo.tvlauncher.ui.theme.MacBlue
import com.meomeo.tvlauncher.ui.theme.SystemGray
import com.meomeo.tvlauncher.ui.theme.SystemGreen
import com.meomeo.tvlauncher.ui.theme.SystemOrange
import com.meomeo.tvlauncher.ui.theme.SystemPink
import com.meomeo.tvlauncher.ui.theme.SystemPurple
import kotlinx.coroutines.delay

/** "System Settings" for the launcher: grouped rows with coloured icon tiles. */
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
    timerStatus: String? = null,
    onOpenTimer: () -> Unit = {},
    appVersionName: String = "",
    updateSummary: String? = null,
    onOpenUpdate: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            firstItemFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    MacWindow(onDismiss = onDismiss, title = "Cài đặt", width = 520.dp, maxHeight = 480.dp) {
        SectionLabel("Giao diện")
        InsetGroup {
            MacRow(
                icon = Icons.Rounded.Apps,
                iconColor = MacBlue,
                title = "Dock",
                subtitle = "Chọn ${AppRepository.DOCK_SLOTS_COUNT} ứng dụng yêu thích",
                chevron = true,
                onClick = onOpenDockCustomizer,
                modifier = Modifier.focusRequester(firstItemFocusRequester)
            )
            MacRow(
                icon = Icons.Rounded.Wallpaper,
                iconColor = SystemPurple,
                title = "Hình nền",
                chevron = true,
                onClick = onOpenWallpaperPicker
            )
            MacRow(
                icon = Icons.Rounded.CloudSync,
                iconColor = SystemGreen,
                title = when {
                    isGooglePhotosSyncing -> "Đang tải ảnh Google Photos..."
                    googlePhotosCount > 0 -> "Đồng bộ lại Google Photos"
                    else -> "Đồng bộ album Google Photos"
                },
                subtitle = if (googlePhotosCount > 0) "$googlePhotosCount ảnh" else null,
                onClick = onSyncGooglePhotos
            )
        }

        SectionLabel("Gia đình")
        InsetGroup {
            MacRow(
                icon = Icons.Rounded.Timer,
                iconColor = SystemOrange,
                title = "Hẹn giờ xem",
                subtitle = timerStatus,
                chevron = true,
                onClick = onOpenTimer
            )
        }

        SectionLabel("Ứng dụng")
        InsetGroup {
            ToggleRow(
                icon = Icons.Rounded.PhoneAndroid,
                iconColor = SystemPink,
                title = "Hiện app di động",
                subtitle = "Quét cả app không có banner TV",
                checked = includeSideload,
                onToggle = onToggleSideload
            )
        }

        SectionLabel("Phần mềm")
        InsetGroup {
            MacRow(
                icon = Icons.Rounded.SystemUpdate,
                iconColor = MacBlue,
                title = "Cập nhật",
                subtitle = updateSummary ?: "Phiên bản v$appVersionName",
                chevron = true,
                onClick = onOpenUpdate
            )
        }

        SectionLabel("Hệ thống")
        InsetGroup {
            MacRow(
                icon = Icons.Rounded.Settings,
                iconColor = SystemGray,
                title = "Cài đặt Android TV",
                chevron = true,
                onClick = {
                    onOpenAndroidSettings()
                    onDismiss()
                }
            )
            MacRow(
                icon = Icons.Rounded.Wifi,
                iconColor = MacBlue,
                title = "Cài đặt Wi-Fi",
                chevron = true,
                onClick = {
                    onOpenWifiSettings()
                    onDismiss()
                }
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "MeoMeo Launcher v$appVersionName",
            fontSize = 11.sp,
            color = LabelTertiary,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp).align(Alignment.CenterHorizontally)
        )
    }
}

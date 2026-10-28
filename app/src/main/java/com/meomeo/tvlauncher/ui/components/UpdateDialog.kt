package com.meomeo.tvlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meomeo.tvlauncher.ui.UpdateState
import com.meomeo.tvlauncher.ui.theme.Danger
import com.meomeo.tvlauncher.ui.theme.Label
import com.meomeo.tvlauncher.ui.theme.LabelSecondary
import com.meomeo.tvlauncher.ui.theme.MacBlue
import com.meomeo.tvlauncher.ui.theme.SystemOrange
import kotlinx.coroutines.delay

/** One-line summary for the Settings row; null when there is nothing to report. */
fun updateSummary(state: UpdateState): String? = when (state) {
    is UpdateState.Available -> "Có bản mới v${state.info.versionName}"
    is UpdateState.Downloading -> "Đang tải bản mới..."
    is UpdateState.Installing -> "Đang cài đặt..."
    else -> null
}

/** Check GitHub for a newer build, download it and hand it to Android's installer. */
@Composable
fun UpdateDialog(
    state: UpdateState,
    currentVersion: String,
    onCheck: () -> Unit,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryFocus = remember { FocusRequester() }
    // Retry until the page has attached its button: the dialog is its own window.
    LaunchedEffect(state::class) {
        repeat(10) {
            delay(80)
            if (try { primaryFocus.requestFocus() } catch (_: Exception) { false }) return@LaunchedEffect
        }
    }

    MacWindow(
        onDismiss = onDismiss,
        title = "Cập nhật phần mềm",
        width = 560.dp,
        contentAlignment = Alignment.CenterHorizontally
    ) {
        IconTile(Icons.Rounded.SystemUpdate, MacBlue, size = 56.dp)
        Spacer(Modifier.height(12.dp))

        when (state) {
            UpdateState.Idle, UpdateState.Checking -> {
                AlertText("Đang kiểm tra...", "Phiên bản hiện tại: v$currentVersion")
                Spacer(Modifier.height(18.dp))
                CircularProgressIndicator(Modifier.size(24.dp), color = LabelSecondary, strokeWidth = 2.5.dp)
                Spacer(Modifier.height(18.dp))
                MacButton("Đóng", onClick = onDismiss, modifier = Modifier.focusRequester(primaryFocus))
            }

            UpdateState.UpToDate -> {
                AlertText("Bạn đang dùng bản mới nhất", "Phiên bản hiện tại: v$currentVersion")
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacButton("Xong", primary = true, onClick = onDismiss, modifier = Modifier.focusRequester(primaryFocus))
                    MacButton("Kiểm tra lại", onClick = onCheck)
                }
            }

            is UpdateState.Available -> {
                val size = state.info.sizeBytes.takeIf { it > 0 }?.let { "  •  %.1f MB".format(it / 1_048_576f) }.orEmpty()
                AlertText("Có bản mới v${state.info.versionName}", "Đang dùng v$currentVersion$size")
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacButton("Tải và cập nhật", primary = true, onClick = onUpdate, modifier = Modifier.focusRequester(primaryFocus))
                    MacButton("Để sau", onClick = onDismiss)
                }
            }

            is UpdateState.Downloading -> {
                AlertText("Đang tải v${state.info.versionName}", "${(state.progress * 100).toInt()}%")
                Spacer(Modifier.height(18.dp))
                ProgressBar(state.progress)
                Spacer(Modifier.height(18.dp))
                MacButton("Chạy nền", onClick = onDismiss, modifier = Modifier.focusRequester(primaryFocus))
            }

            is UpdateState.Installing -> {
                AlertText(
                    "Đang cài đặt v${state.info.versionName}",
                    "Nếu Android hỏi, hãy chọn Cài đặt. Launcher sẽ tự khởi động lại khi xong."
                )
                Spacer(Modifier.height(18.dp))
                CircularProgressIndicator(Modifier.size(24.dp), color = MacBlue, strokeWidth = 2.5.dp)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacButton("Đóng", onClick = onDismiss, modifier = Modifier.focusRequester(primaryFocus))
                    // If the system screen never appeared or was closed, install again.
                    MacButton("Cài lại", onClick = onUpdate)
                }
            }

            is UpdateState.NeedsPermission -> {
                AlertText(
                    "Cần cho phép cài ứng dụng",
                    if (state.settingsOpened) {
                        "Hãy bật \"Cho phép từ nguồn này\" cho MeoMeo Launcher trong trang vừa mở, nhấn Back để quay lại rồi chọn Thử lại."
                    } else {
                        "TV này không có trang cấp quyền. Hãy chạy lệnh dưới đây từ máy tính, rồi chọn Thử lại."
                    }
                )
                if (!state.settingsOpened) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "adb shell appops set com.meomeo.tvlauncher REQUEST_INSTALL_PACKAGES allow",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SystemOrange,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacButton("Thử lại", primary = true, onClick = onUpdate, modifier = Modifier.focusRequester(primaryFocus))
                    MacButton("Đóng", onClick = onDismiss)
                }
            }

            is UpdateState.Failed -> {
                AlertText("Không thể cập nhật", null)
                Spacer(Modifier.height(6.dp))
                Text(state.message, fontSize = 13.sp, color = Danger, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacButton("Kiểm tra lại", primary = true, onClick = onCheck, modifier = Modifier.focusRequester(primaryFocus))
                    MacButton("Đóng", onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: Float) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).clip(CircleShape).background(MacBlue))
    }
}

package com.meomeo.tvlauncher.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meomeo.tvlauncher.ui.AlbumLinkStatus
import com.meomeo.tvlauncher.ui.AlbumLinkUi
import com.meomeo.tvlauncher.ui.theme.Danger
import com.meomeo.tvlauncher.ui.theme.Label
import com.meomeo.tvlauncher.ui.theme.LabelSecondary
import com.meomeo.tvlauncher.ui.theme.MacBlue
import com.meomeo.tvlauncher.ui.theme.Success
import kotlinx.coroutines.delay

/**
 * Change the Google Photos album from a phone. The TV shows a QR code; the phone opens the page it
 * points to, pastes the album link and sends it, and the TV syncs the album and reports back here.
 */
@Composable
fun AlbumLinkDialog(
    link: AlbumLinkUi,
    currentAlbum: String,
    isDefaultAlbum: Boolean,
    onUseDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val doneFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(10) {
            delay(80)
            if (try { doneFocus.requestFocus() } catch (_: Exception) { false }) return@LaunchedEffect
        }
    }

    MacWindow(onDismiss = onDismiss, title = "Album Google Photos", width = 780.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (link.pageUrl != null) {
                QrCode(link.pageUrl, Modifier.size(230.dp))
            } else {
                Box(
                    Modifier.size(230.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = LabelSecondary, modifier = Modifier.size(64.dp))
                }
            }

            Column(Modifier.weight(1f)) {
                if (link.pageUrl == null) {
                    Text("TV chưa kết nối mạng", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Label)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Hãy kết nối TV với Wi-Fi hoặc cáp mạng, sau đó mở lại hộp thoại này.",
                        fontSize = 14.sp,
                        color = LabelSecondary
                    )
                } else {
                    Text("Nhập link album bằng điện thoại", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Label)
                    Spacer(Modifier.height(10.dp))
                    Step(1, "Kết nối điện thoại cùng Wi-Fi với TV")
                    Step(2, "Quét mã QR, hoặc mở địa chỉ bên dưới trên trình duyệt")
                    Step(3, "Dán link album đã chia sẻ rồi nhấn Gửi tới TV")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = link.pageUrl,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Label,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    link.alternateUrls.forEach {
                        Text(
                            text = "hoặc $it",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = LabelSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                StatusLine(link.status)
            }
        }

        if (link.status == AlbumLinkStatus.Waiting && link.pageUrl != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Không mở được trang? Điện thoại phải dùng cùng Wi-Fi với TV (không phải Wi-Fi khách hay 4G/5G). " +
                    "Nếu router bật \"cách ly thiết bị\" (AP isolation), hãy tắt tính năng đó.",
                fontSize = 12.sp,
                color = LabelSecondary
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Album hiện tại: " + shorten(currentAlbum) + if (isDefaultAlbum) "  (mặc định)" else "",
            fontSize = 12.sp,
            color = LabelSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MacButton(text = "Xong", primary = true, onClick = onDismiss, modifier = Modifier.focusRequester(doneFocus))
            if (!isDefaultAlbum) MacButton(text = "Dùng lại album mặc định", onClick = onUseDefault)
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(MacBlue), contentAlignment = Alignment.Center) {
            Text("$number", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Label)
        }
        Text(text, fontSize = 14.sp, color = Label)
    }
}

@Composable
private fun StatusLine(status: AlbumLinkStatus) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when (status) {
            AlbumLinkStatus.Waiting -> {
                CircularProgressIndicator(Modifier.size(18.dp), color = LabelSecondary, strokeWidth = 2.dp)
                Text("Đang chờ link từ điện thoại...", fontSize = 14.sp, color = LabelSecondary)
            }
            AlbumLinkStatus.PhoneOpened -> {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MacBlue, modifier = Modifier.size(20.dp))
                Text("Điện thoại đã kết nối. Hãy dán link rồi nhấn Gửi tới TV.", fontSize = 14.sp, color = Label)
            }
            AlbumLinkStatus.Syncing -> {
                CircularProgressIndicator(Modifier.size(18.dp), color = MacBlue, strokeWidth = 2.dp)
                Text("Đang tải album...", fontSize = 14.sp, color = Label)
            }
            is AlbumLinkStatus.Done -> {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(20.dp))
                Text("Đã đồng bộ ${status.photoCount} ảnh. Hình nền đã được cập nhật.", fontSize = 14.sp, color = Label)
            }
            is AlbumLinkStatus.Failed -> {
                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Danger, modifier = Modifier.size(20.dp))
                Text(status.message, fontSize = 13.sp, color = Danger)
            }
            AlbumLinkStatus.NoNetwork -> Unit
        }
    }
}

/** "https://photos.app.goo.gl/snYDHT7Cc74EvYj4A" -> "photos.app.goo.gl/snYDHT7Cc7…" */
private fun shorten(url: String): String {
    val bare = url.removePrefix("https://").removePrefix("http://")
    return if (bare.length > 36) bare.take(35) + "…" else bare
}

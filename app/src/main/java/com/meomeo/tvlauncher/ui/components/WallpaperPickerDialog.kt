package com.meomeo.tvlauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.meomeo.tvlauncher.R
import com.meomeo.tvlauncher.ui.theme.Label
import com.meomeo.tvlauncher.ui.theme.LabelSecondary
import com.meomeo.tvlauncher.ui.theme.MacBlue
import kotlinx.coroutines.delay
import java.io.File

data class WallpaperItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val resId: Int? = null,
    val isGooglePhotos: Boolean = false
)

/** The Wallpaper pane: thumbnails to pick from, and a button to refresh the Google Photos album. */
@Composable
fun WallpaperPickerDialog(
    currentWallpaperId: String,
    googlePhotos: List<File>,
    isGooglePhotosSyncing: Boolean,
    onSyncGooglePhotos: () -> Unit,
    onSelectWallpaper: (String) -> Unit,
    onChangeAlbum: () -> Unit,
    albumUrl: String,
    onDismiss: () -> Unit
) {
    val firstFocus = remember { FocusRequester() }
    val buttonsFocus = remember { FocusRequester() }

    val options = remember(googlePhotos.size) {
        listOf(
            WallpaperItem(
                id = "wallpaper_google_photos",
                title = "Google Photos",
                subtitle = if (googlePhotos.isNotEmpty()) "${googlePhotos.size} ảnh" else "Đang kết nối...",
                isGooglePhotos = true
            ),
            WallpaperItem("wallpaper_sunset", "Hoàng hôn", "Tông ấm", R.drawable.wallpaper_sunset),
            WallpaperItem("wallpaper_aurora", "Cực quang", "Tông xanh", R.drawable.wallpaper_aurora),
            WallpaperItem("wallpaper_cyber_neon", "Neon", "Hiện đại", R.drawable.wallpaper_cyber_neon)
        )
    }

    LaunchedEffect(Unit) {
        delay(80)
        try {
            firstFocus.requestFocus()
        } catch (_: Exception) {}
    }

    MacWindow(onDismiss = onDismiss, title = "Hình nền", width = 760.dp, contentAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                WallpaperCard(
                    option = option,
                    isSelected = option.id == currentWallpaperId,
                    googlePhotos = googlePhotos,
                    onClick = { onSelectWallpaper(option.id) },
                    modifier = Modifier
                        .weight(1f)
                        // The buttons are not vertically aligned with every card, so name the target.
                        .focusProperties { down = buttonsFocus }
                        .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Album Google Photos: " + albumUrl.removePrefix("https://").take(40),
            fontSize = 12.sp,
            color = LabelSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MacButton(
                text = "Đổi album bằng điện thoại",
                icon = Icons.Rounded.QrCode2,
                primary = true,
                onClick = onChangeAlbum,
                modifier = Modifier.focusRequester(buttonsFocus)
            )
            MacButton(
                text = if (isGooglePhotosSyncing) "Đang tải ảnh..." else "Đồng bộ lại",
                icon = Icons.Rounded.Sync,
                onClick = onSyncGooglePhotos
            )
        }
    }
}

@Composable
private fun WallpaperCard(
    option: WallpaperItem,
    isSelected: Boolean,
    googlePhotos: List<File>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier,
        cornerRadius = 14.dp,
        focusedColor = Color.White.copy(alpha = 0.16f),
        focusScale = 1.04f,
        ring = true,
        contentAlignment = Alignment.TopCenter
    ) { _ ->
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().aspectRatio(1.25f).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.35f))) {
                if (option.isGooglePhotos) {
                    val preview = googlePhotos.firstOrNull()
                    if (preview != null && preview.exists()) {
                        AsyncImage(
                            model = preview,
                            contentDescription = option.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alignment = BiasAlignment(0f, -0.4f)
                        )
                    } else {
                        Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = LabelSecondary, modifier = Modifier.align(Alignment.Center).size(32.dp))
                    }
                } else if (option.resId != null) {
                    Image(painterResource(option.resId), option.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(MacBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Đang chọn", tint = Label, modifier = Modifier.size(15.dp))
                    }
                }
            }
            Text(
                text = option.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (option.subtitle != null) {
                Text(option.subtitle, fontSize = 11.sp, color = LabelSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

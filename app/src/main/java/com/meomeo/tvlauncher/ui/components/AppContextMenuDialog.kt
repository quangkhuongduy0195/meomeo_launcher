package com.meomeo.tvlauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meomeo.tvlauncher.model.AppInfo
import com.meomeo.tvlauncher.ui.theme.Danger
import com.meomeo.tvlauncher.ui.theme.Label
import com.meomeo.tvlauncher.ui.theme.LabelSecondary
import kotlinx.coroutines.delay

/** The right-click menu of an app: a small frosted popup with a header and a blue selection. */
@Composable
fun AppContextMenuDialog(
    app: AppInfo,
    slotIndex: Int? = null,
    onDismiss: () -> Unit,
    onChangeSlotApp: (() -> Unit)? = null,
    onToggleFavorite: () -> Unit,
    onOpenInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    // Land focus on the first option right away so the remote works without an extra D-pad press.
    val firstItemFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100)
        try {
            firstItemFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }
    val hasChangeSlotOption = slotIndex != null && onChangeSlotApp != null
    val bitmap = remember(app.packageName) { drawableToBitmap(app.icon ?: app.banner) }

    MenuPopup(onDismiss = onDismiss, width = 340.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (bitmap != null) AppIconTile(bitmap, app.label, Modifier.size(44.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, fontSize = 11.sp, color = LabelSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        MenuSeparator()

        if (slotIndex != null && onChangeSlotApp != null) {
            MenuItem(
                icon = Icons.Rounded.Edit,
                text = "Đổi ứng dụng ở vị trí ${slotIndex + 1}",
                modifier = Modifier.focusRequester(firstItemFocusRequester)
            ) {
                onChangeSlotApp()
                onDismiss()
            }
        }
        MenuItem(
            icon = if (app.isFavorite) Icons.Rounded.StarOutline else Icons.Rounded.Star,
            text = if (app.isFavorite) "Xóa khỏi Yêu thích" else "Thêm vào Yêu thích",
            modifier = if (hasChangeSlotOption) Modifier else Modifier.focusRequester(firstItemFocusRequester)
        ) {
            onToggleFavorite()
            onDismiss()
        }
        MenuItem(icon = Icons.Rounded.Info, text = "Thông tin ứng dụng") {
            onOpenInfo()
            onDismiss()
        }
        MenuSeparator()
        MenuItem(icon = Icons.Rounded.Delete, text = "Gỡ cài đặt", danger = true) {
            onUninstall()
            onDismiss()
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    FocusSurface(onClick = onClick, modifier = modifier.fillMaxWidth(), cornerRadius = 8.dp, contentAlignment = Alignment.CenterStart) { focused ->
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val color = if (danger && !focused) Danger else Label
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

package com.meomeo.tvlauncher.ui.components

import com.meomeo.tvlauncher.ui.theme.glassIdleColor
import com.meomeo.tvlauncher.ui.theme.DialogDim
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.meomeo.tvlauncher.model.AppInfo
import com.meomeo.tvlauncher.ui.theme.Accent
import com.meomeo.tvlauncher.ui.theme.AccentGlow
import com.meomeo.tvlauncher.ui.theme.BgCard
import com.meomeo.tvlauncher.ui.theme.StarYellow
import com.meomeo.tvlauncher.ui.theme.TextPrimary
import com.meomeo.tvlauncher.ui.theme.TextSecondary
import kotlinx.coroutines.delay

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

    Dialog(onDismissRequest = onDismiss) {
        DialogDim()
        Box(
            modifier = Modifier
                .width(380.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(glassIdleColor())
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = app.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = app.packageName,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Option: Change Dock App (if from dock)
                if (slotIndex != null && onChangeSlotApp != null) {
                    MenuDialogItem(
                        modifier = Modifier.focusRequester(firstItemFocusRequester),
                        icon = Icons.Rounded.Edit,
                        iconTint = Color.White,
                        text = "Đổi ứng dụng ở vị trí ${slotIndex + 1}",
                        onClick = {
                            onChangeSlotApp()
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Option 1: Favorite Toggle
                MenuDialogItem(
                    icon = if (app.isFavorite) Icons.Rounded.StarOutline else Icons.Rounded.Star,
                    iconTint = if (app.isFavorite) TextSecondary else StarYellow,
                    text = if (app.isFavorite) "Xóa khỏi danh sách Yêu thích" else "Thêm vào danh sách Yêu thích",
                    onClick = {
                        onToggleFavorite()
                        onDismiss()
                    },
                    modifier = if (hasChangeSlotOption) Modifier else Modifier.focusRequester(firstItemFocusRequester)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: App Info
                MenuDialogItem(
                    icon = Icons.Rounded.Info,
                    iconTint = Color.White,
                    text = "Thông tin ứng dụng (Cài đặt)",
                    onClick = {
                        onOpenInfo()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 3: Uninstall
                MenuDialogItem(
                    icon = Icons.Rounded.Delete,
                    iconTint = Color(0xFFEF4444),
                    text = "Gỡ cài đặt",
                    onClick = {
                        onUninstall()
                        onDismiss()
                    }
                )
            }
        }
    }
}

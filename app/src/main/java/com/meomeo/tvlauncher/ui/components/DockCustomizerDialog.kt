package com.meomeo.tvlauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meomeo.tvlauncher.model.AppInfo
import com.meomeo.tvlauncher.ui.theme.DockRadius
import com.meomeo.tvlauncher.ui.theme.DockTint
import com.meomeo.tvlauncher.ui.theme.LabelSecondary
import kotlinx.coroutines.delay

/**
 * Edit the dock. The window shows the dock itself, slot by slot; selecting a slot opens a
 * Launchpad page to choose its app.
 */
@Composable
fun DockCustomizerDialog(
    dockApps: List<AppInfo?>,
    allApps: List<AppInfo>,
    onSetDockSlot: (Int, AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var focusedSlotIndex by remember { mutableIntStateOf(0) }
    var pickingForSlot by remember { mutableStateOf<Int?>(null) }

    val slotFocusRequesters = remember(dockApps.size) { List(dockApps.size.coerceAtLeast(1)) { FocusRequester() } }
    val firstAppFocusRequester = remember { FocusRequester() }

    // Each page is its own window and attaches a moment late, so keep trying until focus sticks.
    LaunchedEffect(pickingForSlot) {
        repeat(15) {
            delay(80)
            val target = if (pickingForSlot != null) {
                firstAppFocusRequester
            } else {
                slotFocusRequesters[focusedSlotIndex.coerceIn(0, (dockApps.size - 1).coerceAtLeast(0))]
            }
            val focused = try { target.requestFocus() } catch (_: Exception) { false }
            if (focused) return@LaunchedEffect
        }
    }

    val target = pickingForSlot
    if (target == null) {
        MacWindow(onDismiss = onDismiss, title = "Dock", width = 820.dp, contentAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Chọn một vị trí rồi nhấn OK để đổi ứng dụng. Ô cuối luôn là Launchpad.",
                fontSize = 13.sp,
                color = LabelSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            // A miniature of the real dock.
            Box(Modifier.padding(top = 6.dp)) {
                FrostedBox(Modifier.matchParentSize(), radius = DockRadius, tint = DockTint)
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dockApps.forEachIndexed { index, app ->
                        LaunchpadIcon(
                            app = app,
                            slotNumber = index + 1,
                            onClick = {
                                focusedSlotIndex = index
                                pickingForSlot = index
                            },
                            onFocusGain = { focusedSlotIndex = index },
                            modifier = Modifier
                                .size(width = 88.dp, height = 96.dp)
                                .focusRequester(slotFocusRequesters[index.coerceIn(0, slotFocusRequesters.size - 1)])
                        )
                    }
                    Box(Modifier.width(1.dp).height(44.dp).background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f)))
                    LaunchpadAppIcon(Modifier.size(52.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Nhấn Back khi hoàn tất", fontSize = 12.sp, color = LabelSecondary, fontWeight = FontWeight.Medium)
        }
    } else {
        LaunchpadDialog(onDismiss = { pickingForSlot = null }) {
            LaunchpadHeader("Chọn ứng dụng thay thế", trailing = "Vị trí ${target + 1} / ${dockApps.size}")
            AppGrid(
                apps = allApps,
                firstItemFocusRequester = firstAppFocusRequester,
                modifier = Modifier.weight(1f)
            ) { app, cellModifier ->
                LaunchpadIcon(
                    app = app,
                    selected = dockApps.getOrNull(target)?.packageName == app.packageName,
                    onClick = {
                        focusedSlotIndex = target
                        onSetDockSlot(target, app)
                        pickingForSlot = null
                    },
                    modifier = cellModifier
                )
            }
        }
    }
}

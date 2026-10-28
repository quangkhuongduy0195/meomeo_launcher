package com.meomeo.tvlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meomeo.tvlauncher.R
import com.meomeo.tvlauncher.model.AppInfo
import com.meomeo.tvlauncher.ui.components.AllAppsDialog
import com.meomeo.tvlauncher.ui.components.AppContextMenuDialog
import com.meomeo.tvlauncher.ui.components.AppPickerDialog
import com.meomeo.tvlauncher.ui.components.Dock
import com.meomeo.tvlauncher.ui.components.DockAppSlot
import com.meomeo.tvlauncher.ui.components.DockCustomizerDialog
import com.meomeo.tvlauncher.ui.components.DockLaunchpadSlot
import com.meomeo.tvlauncher.ui.components.DesktopWallpaper
import com.meomeo.tvlauncher.ui.components.LocalBlurredWallpaper
import com.meomeo.tvlauncher.ui.components.LockScreen
import com.meomeo.tvlauncher.ui.components.MenuBar
import com.meomeo.tvlauncher.ui.components.SettingsDialog
import com.meomeo.tvlauncher.ui.components.WallpaperPickerDialog
import com.meomeo.tvlauncher.ui.components.dockMagnification
import com.meomeo.tvlauncher.ui.components.nextWallpaperData
import com.meomeo.tvlauncher.ui.components.rememberBlurredWallpaper
import com.meomeo.tvlauncher.ui.components.wallpaperData
import com.meomeo.tvlauncher.ui.theme.MacBlue
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var activeApp by remember { mutableStateOf<AppInfo?>(null) }
    var isLaunchpadFocused by remember { mutableStateOf(false) }

    // One requester per dock slot (apps + the trailing Launchpad) so focus can be restored to
    // exactly where the user was after a dialog closes.
    val slotCount = uiState.dockApps.size + 1
    val slotRequesters = remember(slotCount) { List(slotCount) { FocusRequester() } }
    var lastFocusIndex by remember { mutableIntStateOf(0) }
    // Magnification only applies while focus is actually inside the dock.
    var dockHasFocus by remember { mutableStateOf(false) }

    val isAnyDialogOpen = uiState.selectedAppForMenu != null ||
        uiState.isSettingsOpen ||
        uiState.isDockCustomizerOpen ||
        uiState.isWallpaperPickerOpen ||
        uiState.isAllAppsDialogOpen ||
        uiState.slotPickingFor != null ||
        uiState.timerFlow != null
    // The lock screen is not a dialog, but it also takes focus away from the dock and has to
    // hand it back when it goes away.
    val isCovered = isAnyDialogOpen || uiState.isLocked

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.dockApps.isNotEmpty()) {
            try {
                slotRequesters[0].requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Dialogs live in their own windows, so closing one leaves the dock without focus.
    // Put it back on the last focused slot so the next D-pad press behaves as expected.
    LaunchedEffect(isCovered) {
        if (!isCovered && !uiState.isLoading) {
            delay(150)
            try {
                slotRequesters[lastFocusIndex.coerceIn(0, slotRequesters.lastIndex)].requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(uiState.dockApps) {
        if (activeApp == null && uiState.dockApps.isNotEmpty()) {
            activeApp = uiState.dockApps.firstOrNull()
        }
    }

    val wallpaperRes = when (uiState.selectedWallpaper) {
        "wallpaper_aurora" -> R.drawable.wallpaper_aurora
        "wallpaper_cyber_neon" -> R.drawable.wallpaper_cyber_neon
        else -> R.drawable.wallpaper_sunset
    }
    val wallpaper = wallpaperData(uiState, wallpaperRes)
    // Every frosted surface (menu bar, dock, windows) shows this blurred copy of the wallpaper.
    val blurred = rememberBlurredWallpaper(wallpaper)

    CompositionLocalProvider(LocalBlurredWallpaper provides blurred) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        DesktopWallpaper(wallpaper, nextWallpaperData(uiState))

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MacBlue, strokeWidth = 3.dp)
            }
        } else if (!uiState.isLocked) {
            MenuBar(
                activeName = when {
                    isLaunchpadFocused -> "Launchpad"
                    else -> activeApp?.label ?: "Ô trống"
                },
                timerLabel = timerStatusLabel(uiState.timerEndAt),
                onWifi = { viewModel.openWifiSettings() },
                onTimer = { viewModel.openTimerSettings() },
                onSettings = { viewModel.openSettingsDialog() },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            Dock(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)) {
                val focusedIndex = if (dockHasFocus) lastFocusIndex else -1
                fun magnificationFor(index: Int): Float =
                    if (focusedIndex < 0) 1f else dockMagnification(abs(index - focusedIndex))

                Row(
                    modifier = Modifier.onFocusChanged { dockHasFocus = it.hasFocus },
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    // Bottom-aligned so magnified icons rise out of the dock like macOS.
                    verticalAlignment = Alignment.Bottom
                ) {
                    uiState.dockApps.forEachIndexed { index, app ->
                        DockAppSlot(
                            app = app,
                            magnification = magnificationFor(index),
                            onFocusGain = {
                                lastFocusIndex = index
                                isLaunchpadFocused = false
                                activeApp = app
                            },
                            onClick = {
                                if (app != null) viewModel.launchApp(app) else viewModel.openSlotPicker(index)
                            },
                            onLongClick = {
                                if (app != null) viewModel.showAppMenu(app, slotIndex = index) else viewModel.openSlotPicker(index)
                            },
                            modifier = Modifier.focusRequester(slotRequesters[index])
                        )
                    }

                    // Divider, like the one before the Trash in the macOS dock
                    Box(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .width(1.5.dp)
                            .height(40.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )

                    DockLaunchpadSlot(
                        magnification = magnificationFor(uiState.dockApps.size),
                        onFocusGain = {
                            lastFocusIndex = uiState.dockApps.size
                            isLaunchpadFocused = true
                            activeApp = null
                        },
                        onClick = { viewModel.openAllAppsDialog() },
                        modifier = Modifier.focusRequester(slotRequesters[uiState.dockApps.size])
                    )
                }
            }
        }

        // Time is up: opaque lock screen on top of everything else in the window.
        if (uiState.isLocked) {
            LockScreen(
                lockoutUntil = uiState.pinLockoutUntil,
                onVerify = viewModel::verifyPasscode,
                resetKey = uiState.timerFlow?.ordinal ?: -1
            )
        }

        // --- Dialogs ---

        // Settings Dialog
        if (uiState.isSettingsOpen) {
            SettingsDialog(
                includeSideload = uiState.includeSideload,
                onToggleSideload = { viewModel.toggleIncludeSideload() },
                onOpenDockCustomizer = { viewModel.openDockCustomizer() },
                onOpenWallpaperPicker = { viewModel.openWallpaperPicker() },
                googlePhotosCount = uiState.googlePhotos.size,
                isGooglePhotosSyncing = uiState.isGooglePhotosSyncing,
                onSyncGooglePhotos = { viewModel.syncGooglePhotosNow() },
                onOpenAndroidSettings = { viewModel.openSystemSettings() },
                onOpenWifiSettings = { viewModel.openWifiSettings() },
                timerStatus = timerStatusLabel(uiState.timerEndAt),
                onOpenTimer = { viewModel.openTimerSettings() },
                onDismiss = { viewModel.closeSettingsDialog() }
            )
        }

        // 8 Dock Apps Customizer Dialog (From Settings)
        if (uiState.isDockCustomizerOpen) {
            DockCustomizerDialog(
                dockApps = uiState.dockApps,
                allApps = uiState.allApps,
                onSetDockSlot = { slotIndex, selectedApp ->
                    viewModel.setDockSlot(slotIndex, selectedApp, keepCustomizerOpen = true)
                },
                onDismiss = { viewModel.closeDockCustomizer() }
            )
        }

        // Wallpaper Picker Dialog
        if (uiState.isWallpaperPickerOpen) {
            WallpaperPickerDialog(
                currentWallpaperId = uiState.selectedWallpaper,
                googlePhotos = uiState.googlePhotos,
                isGooglePhotosSyncing = uiState.isGooglePhotosSyncing,
                onSyncGooglePhotos = { viewModel.syncGooglePhotosNow() },
                onSelectWallpaper = { wallpaperId ->
                    viewModel.selectWallpaper(wallpaperId)
                },
                onDismiss = { viewModel.closeWallpaperPicker() }
            )
        }

        // App Picker Dialog (When selecting which app to assign to Slot X)
        uiState.slotPickingFor?.let { slotIndex ->
            AppPickerDialog(
                slotIndex = slotIndex,
                allApps = uiState.allApps,
                onSelectApp = { selectedApp ->
                    viewModel.setDockSlot(slotIndex, selectedApp)
                },
                onDismiss = { viewModel.closeSlotPicker() }
            )
        }

        // All Apps Drawer Dialog
        if (uiState.isAllAppsDialogOpen) {
            AllAppsDialog(
                allApps = uiState.allApps,
                onLaunchApp = { app ->
                    viewModel.closeAllAppsDialog()
                    viewModel.launchApp(app)
                },
                onShowAppMenu = { app -> viewModel.showAppMenu(app) },
                onDismiss = { viewModel.closeAllAppsDialog() }
            )
        }

        // Viewing timer dialogs (PIN entry / setup / timer settings)
        TimerFlowHost(uiState = uiState, viewModel = viewModel)

        // App Context Menu Dialog — last so it stacks above every other dialog (e.g. All Apps)
        uiState.selectedAppForMenu?.let { app ->
            AppContextMenuDialog(
                app = app,
                slotIndex = uiState.selectedSlotForMenu,
                onDismiss = { viewModel.hideAppMenu() },
                onChangeSlotApp = {
                    uiState.selectedSlotForMenu?.let { slot ->
                        viewModel.openSlotPicker(slot)
                    }
                },
                onToggleFavorite = { viewModel.toggleFavorite(app) },
                onOpenInfo = { viewModel.openAppSettings(app) },
                onUninstall = { viewModel.uninstallApp(app) }
            )
        }
    }
    }
}

/** Short status for the Settings entry, e.g. "còn 12 phút"; null when no timer is running. */
private fun timerStatusLabel(endAt: Long): String? {
    if (endAt == 0L) return null
    val msLeft = endAt - System.currentTimeMillis()
    return if (msLeft <= 0) "đã hết giờ" else "còn ${(msLeft + 59_999) / 60_000} phút"
}

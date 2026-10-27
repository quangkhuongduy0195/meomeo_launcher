package com.meomeo.tvlauncher.ui

import androidx.compose.ui.focus.onFocusChanged
import com.meomeo.tvlauncher.ui.components.dockMagnification
import kotlin.math.abs
import com.meomeo.tvlauncher.ui.components.LockScreen
import android.os.Build
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import com.meomeo.tvlauncher.ui.theme.DockBackdropBlur
import com.meomeo.tvlauncher.ui.theme.DialogBackdropBlur
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import com.meomeo.tvlauncher.ui.theme.ColorExtractor
import com.meomeo.tvlauncher.ui.theme.LocalGlassPalette
import com.meomeo.tvlauncher.ui.theme.glassPalette
import com.meomeo.tvlauncher.ui.theme.glassIdleColor
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import com.meomeo.tvlauncher.R
import com.meomeo.tvlauncher.model.AppInfo
import com.meomeo.tvlauncher.ui.components.AllAppsDialog
import com.meomeo.tvlauncher.ui.components.AllAppsDockCard
import com.meomeo.tvlauncher.ui.components.AppContextMenuDialog
import com.meomeo.tvlauncher.ui.components.AppPickerDialog
import com.meomeo.tvlauncher.ui.components.DockCard
import com.meomeo.tvlauncher.ui.components.DockCustomizerDialog
import com.meomeo.tvlauncher.ui.components.HeroContent
import com.meomeo.tvlauncher.ui.components.HeroSection
import com.meomeo.tvlauncher.ui.components.SettingsDialog
import com.meomeo.tvlauncher.ui.components.TopHeader
import com.meomeo.tvlauncher.ui.components.WallpaperPickerDialog
import com.meomeo.tvlauncher.ui.theme.Accent
import com.meomeo.tvlauncher.ui.theme.AccentGlow
import com.meomeo.tvlauncher.ui.theme.TextPrimary
import com.meomeo.tvlauncher.ui.theme.TextSecondary

@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var activeApp by remember { mutableStateOf<AppInfo?>(null) }
    var activeColor by remember { mutableStateOf(Color(0xFF6366F1)) }
    var isAllAppsFocused by remember { mutableStateOf(false) }

    val settingsFocusRequester = remember { FocusRequester() }
    // One requester per dock item (apps + the trailing "All apps" card) so focus can be restored
    // to exactly where the user was after a dialog closes.
    val dockItemCount = uiState.dockApps.size + 1
    val dockRequesters = remember(dockItemCount) { List(dockItemCount) { FocusRequester() } }
    val dockFocusRequester = dockRequesters[0]
    var lastDockFocusIndex by remember { mutableIntStateOf(0) }
    // Dock magnification only applies while focus is actually inside the dock.
    var dockHasFocus by remember { mutableStateOf(false) }

    val isAnyDialogOpen = uiState.selectedAppForMenu != null ||
        uiState.isSettingsOpen ||
        uiState.isDockCustomizerOpen ||
        uiState.isWallpaperPickerOpen ||
        uiState.isAllAppsDialogOpen ||
        uiState.slotPickingFor != null ||
        uiState.timerFlow != null
    // The lock screen is not a dialog (it is opaque, so it must not be blurred), but it also
    // takes focus away from the dock and has to hand it back when it goes away.
    val isDockCovered = isAnyDialogOpen || uiState.isLocked

    val ambientGlow by animateColorAsState(
        targetValue = activeColor,
        animationSpec = tween(500),
        label = "ambientColor"
    )

    androidx.compose.runtime.LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.dockApps.isNotEmpty()) {
            try {
                dockFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Dialogs live in their own windows, so closing one leaves the dock without focus.
    // Put it back on the last focused dock item so the next D-pad press behaves as expected.
    androidx.compose.runtime.LaunchedEffect(isDockCovered) {
        if (!isDockCovered && !uiState.isLoading) {
            kotlinx.coroutines.delay(150)
            try {
                dockRequesters[lastDockFocusIndex.coerceIn(0, dockRequesters.lastIndex)].requestFocus()
            } catch (_: Exception) {}
        }
    }

    androidx.compose.runtime.LaunchedEffect(uiState.dockApps) {
        if (activeApp == null && uiState.dockApps.isNotEmpty()) {
            val first = uiState.dockApps.firstOrNull()
            if (first != null) {
                activeApp = first
                val bmp = com.meomeo.tvlauncher.ui.components.drawableToBitmap(first.icon ?: first.banner)
                activeColor = com.meomeo.tvlauncher.ui.theme.ColorExtractor.getDominantColor(first.packageName, bmp)
            }
        }
    }

    val wallpaperRes = when (uiState.selectedWallpaper) {
        "wallpaper_aurora" -> R.drawable.wallpaper_aurora
        "wallpaper_cyber_neon" -> R.drawable.wallpaper_cyber_neon
        else -> R.drawable.wallpaper_sunset
    }

    // --- Glass tint: wallpaper tone (stable) + focused app tone (for the focused fill only).
    val currentPhotoFile = uiState.googlePhotos.getOrNull(uiState.currentGooglePhotoIndex)
        ?: uiState.googlePhotos.firstOrNull()
    val sunsetTone = Color(0xFFFFB98A)
    val photoTone by produceState(sunsetTone, currentPhotoFile) {
        value = if (currentPhotoFile != null && currentPhotoFile.exists()) {
            withContext(Dispatchers.IO) {
                val opts = BitmapFactory.Options().apply { inSampleSize = 16 }
                BitmapFactory.decodeFile(currentPhotoFile.absolutePath, opts)
                    ?.let { ColorExtractor.averageColor(it) }
            } ?: sunsetTone
        } else sunsetTone
    }
    val wallpaperTone = when (uiState.selectedWallpaper) {
        "wallpaper_aurora" -> Color(0xFF9DE0D0)
        "wallpaper_cyber_neon" -> Color(0xFFC4A8FF)
        "wallpaper_google_photos" -> photoTone
        else -> sunsetTone
    }
    val animatedWallpaperTone by animateColorAsState(wallpaperTone, tween(800), label = "wallpaperTone")
    // Empty slot / "All apps" have no meaningful app colour, so they follow the wallpaper alone.
    val appTone = if (isAllAppsFocused || activeApp == null) animatedWallpaperTone else ambientGlow
    val glassPalette = glassPalette(animatedWallpaperTone, appTone)

    CompositionLocalProvider(LocalGlassPalette provides glassPalette) {
    // Blur (rather than black out) the screen behind dialogs. No-op below API 31.
    val backdropBlur by animateDpAsState(
        targetValue = if (isAnyDialogOpen) DialogBackdropBlur else 0.dp,
        animationSpec = tween(250),
        label = "backdropBlur"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (backdropBlur > 0.dp) Modifier.blur(backdropBlur) else Modifier)
            .background(Color(0xFF060810)) // Pure deep OLED black base
    ) {
        WallpaperBackdrop(uiState = uiState, wallpaperRes = wallpaperRes, ambientGlow = ambientGlow)

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Accent,
                    strokeWidth = 3.dp
                )
            }
        } else if (!uiState.isLocked) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header: Real-time clock, localized date, Wi-Fi & Settings
                TopHeader(
                    settingsFocusRequester = settingsFocusRequester,
                    dockFocusRequester = dockFocusRequester,
                    onOpenWifi = { viewModel.openWifiSettings() },
                    onOpenSettings = { viewModel.openSettingsDialog() }
                )

                // Center: Cinematic Hero Showcase Spotlight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 48.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Re-resolve from state so changes such as toggling a favourite show immediately.
                    val focusedApp = activeApp?.let { focused ->
                        uiState.dockApps.firstOrNull { it?.packageName == focused.packageName } ?: focused
                    }
                    val heroContent = when {
                        isAllAppsFocused -> HeroContent.AllApps(uiState.allApps.size)
                        focusedApp != null -> HeroContent.App(focusedApp)
                        else -> HeroContent.EmptySlot
                    }
                    HeroSection(content = heroContent)
                }

                // Bottom: Floating Glass Shelf Dock (8 Apps)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Floating Frosted Glass Capsule Behind Cards.
                    // The glass is a sibling layer (not a clip on the row) so focused cards can
                    // scale up and cast their glow beyond the capsule edge without being cut off.
                    var capsuleTopLeft by remember { mutableStateOf(Offset.Zero) }
                    val screen = LocalConfiguration.current
                    val capsuleShape = RoundedCornerShape(24.dp)
                    Box(modifier = Modifier.onGloballyPositioned { capsuleTopLeft = it.positionInRoot() }) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(capsuleShape)
                        ) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                // Backdrop blur: Compose has no "blur what is behind me", so draw
                                // the wallpaper again at full-screen size, shifted so it lines up
                                // with the real one, and blur that copy inside the capsule.
                                Box(
                                    modifier = Modifier
                                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                                        .requiredSize(screen.screenWidthDp.dp, screen.screenHeightDp.dp)
                                        .offset {
                                            IntOffset(-capsuleTopLeft.x.roundToInt(), -capsuleTopLeft.y.roundToInt())
                                        }
                                        .blur(DockBackdropBlur)
                                ) {
                                    WallpaperBackdrop(
                                        uiState = uiState,
                                        wallpaperRes = wallpaperRes,
                                        ambientGlow = ambientGlow
                                    )
                                }
                            } else {
                                // No blur below API 31: darken instead so labels stay readable.
                                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.35f)))
                            }
                            Box(Modifier.matchParentSize().background(glassIdleColor()))
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .border(1.2.dp, Color.White.copy(alpha = 0.12f), capsuleShape)
                            )
                        }
                        val focusedDockIndex = if (dockHasFocus) lastDockFocusIndex else -1
                        fun magnificationFor(index: Int): Float =
                            if (focusedDockIndex < 0) 1f else dockMagnification(abs(index - focusedDockIndex))

                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .onFocusChanged { dockHasFocus = it.hasFocus },
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            // Bottom-aligned so magnified icons rise out of the dock like macOS.
                            verticalAlignment = Alignment.Bottom
                        ) {
                            uiState.dockApps.forEachIndexed { index, app ->
                                DockCard(
                                    slotIndex = index,
                                    app = app,
                                    magnification = magnificationFor(index),
                                    onFocusGain = { focusedApp, color ->
                                        lastDockFocusIndex = index
                                        isAllAppsFocused = false
                                        activeApp = focusedApp
                                        activeColor = color
                                    },
                                    onClick = {
                                        if (app != null) {
                                            viewModel.launchApp(app)
                                        } else {
                                            viewModel.openSlotPicker(index)
                                        }
                                    },
                                    onLongClick = {
                                        if (app != null) {
                                            viewModel.showAppMenu(app, slotIndex = index)
                                        } else {
                                            viewModel.openSlotPicker(index)
                                        }
                                    },
                                    modifier = Modifier
                                        .focusProperties {
                                            up = settingsFocusRequester
                                        }
                                        .focusRequester(dockRequesters[index])
                                )
                            }

                            // Divider, like the one before the Trash in the macOS dock
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .width(1.5.dp)
                                    .height(40.dp)
                                    .background(Color.White.copy(alpha = 0.25f))
                            )

                            // 8th Permanent Slot: "Tất cả" (All Apps) Shortcut
                            AllAppsDockCard(
                                magnification = magnificationFor(uiState.dockApps.size),
                                onFocusGain = { color ->
                                    lastDockFocusIndex = uiState.dockApps.size
                                    isAllAppsFocused = true
                                    activeApp = null
                                    activeColor = color
                                },
                                onClick = {
                                    viewModel.openAllAppsDialog()
                                },
                                modifier = Modifier
                                    .focusProperties {
                                        up = settingsFocusRequester
                                    }
                                    .focusRequester(dockRequesters[uiState.dockApps.size])
                            )
                        }
                    }
                }
            }
        }

        // Time is up: opaque lock screen on top of everything else in the window.
        if (uiState.isLocked) {
            LockScreen(
                background = {
                    WallpaperBackdrop(uiState = uiState, wallpaperRes = wallpaperRes, ambientGlow = ambientGlow)
                },
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

package com.meomeo.tvlauncher.ui

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
    val dockFocusRequester = remember { FocusRequester() }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060810)) // Pure deep OLED black base
    ) {
        // 1. Wallpaper Subtle Texture (Supports Google Photos album & presets)
        if (uiState.selectedWallpaper == "wallpaper_google_photos") {
            val currentPhoto = uiState.googlePhotos.getOrNull(uiState.currentGooglePhotoIndex)
                ?: uiState.googlePhotos.firstOrNull()

            Crossfade(
                targetState = currentPhoto,
                animationSpec = tween(1200),
                label = "googlePhotosCrossfade"
            ) { photoFile ->
                if (photoFile != null && photoFile.exists()) {
                    val (imgWidth, imgHeight) = remember(photoFile.absolutePath) {
                        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath, options)
                        options.outWidth to options.outHeight
                    }
                    val aspectRatio = if (imgHeight > 0) imgWidth.toFloat() / imgHeight.toFloat() else 1.77f
                    val isWidescreen = aspectRatio >= 1.60f

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!isWidescreen) {
                            // 1. Ambient blurred backdrop: fills widescreen TV sides seamlessly with matching colors
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photoFile)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(32.dp),
                                contentScale = ContentScale.Crop,
                                alpha = 0.50f
                            )
                            // 2. Crisp foreground photo: full image at true aspect ratio, never cropped, high-quality filtering
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photoFile)
                                    .build(),
                                contentDescription = "Hình nền Google Photos",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                filterQuality = FilterQuality.High,
                                alpha = 1.0f
                            )
                        } else {
                            // Widescreen photo fills entire screen with high-quality filter
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photoFile)
                                    .build(),
                                contentDescription = "Hình nền Google Photos",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.High,
                                alpha = 1.0f
                            )
                        }
                    }
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.wallpaper_sunset),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.55f
                    )
                }
            }
        } else {
            Image(
                painter = painterResource(id = wallpaperRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.55f
            )
        }

        // 2. Dynamic Ambient Aura (Only for built-in abstract wallpapers, NOT for personal Google Photos!)
        if (uiState.selectedWallpaper != "wallpaper_google_photos") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ambientGlow.copy(alpha = 0.38f),
                                Color.Transparent
                            ),
                            radius = 1400f
                        )
                    )
            )
        }

        // 3. Ambient Top & Bottom Scrim (Film-grade vignette for crystal-clear readability)
        val vignetteBrush = if (uiState.selectedWallpaper == "wallpaper_google_photos") {
            Brush.verticalGradient(
                0.0f to Color.Black.copy(alpha = 0.72f),
                0.12f to Color.Black.copy(alpha = 0.45f),
                0.26f to Color.Transparent,
                0.68f to Color.Transparent,
                1.0f to Color.Black.copy(alpha = 0.70f)
            )
        } else {
            Brush.verticalGradient(
                0.0f to Color.Black.copy(alpha = 0.65f),
                0.14f to Color.Black.copy(alpha = 0.35f),
                0.28f to Color.Transparent,
                0.68f to Color.Transparent,
                1.0f to Color.Black.copy(alpha = 0.75f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(vignetteBrush)
        )

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
        } else {
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
                        .padding(horizontal = 56.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isAllAppsFocused) {
                        Column {
                            // Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Accent.copy(alpha = 0.28f))
                                    .border(1.dp, Accent.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "KHO ỨNG DỤNG • HỆ THỐNG",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Large Cinematic Title
                            Text(
                                text = "Tất cả ứng dụng",
                                style = TextStyle(
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.85f),
                                        offset = Offset(0f, 3f),
                                        blurRadius = 10f
                                    )
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick Remote Hints
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color.White.copy(alpha = 0.18f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "OK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "Mở toàn bộ ${uiState.allApps.size} ứng dụng",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        val currentApp = activeApp ?: uiState.dockApps.firstOrNull()
                        if (currentApp != null) {
                        Column {
                            // Badges
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ambientGlow.copy(alpha = 0.28f))
                                        .border(1.dp, ambientGlow.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (currentApp.isTvApp) "ỨNG DỤNG TV" else "ỨNG DỤNG DI ĐỘNG",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                if (currentApp.isFavorite) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x33FBBF24))
                                            .border(1.dp, Color(0x66FBBF24), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "★ YÊU THÍCH",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Large Cinematic Title with high-contrast shadow
                            Text(
                                text = currentApp.label,
                                style = TextStyle(
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.85f),
                                        offset = Offset(0f, 3f),
                                        blurRadius = 10f
                                    )
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Quick Remote Hints
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color.White.copy(alpha = 0.18f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "OK",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = "Mở",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color.White.copy(alpha = 0.18f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Giữ OK",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = "Đổi vị trí",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

                // Bottom: Floating Glass Shelf Dock (8 Apps)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Floating Frosted Glass Capsule Behind Cards
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.08f),
                                        Color.Black.copy(alpha = 0.40f)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            uiState.dockApps.forEachIndexed { index, app ->
                                DockCard(
                                    slotIndex = index,
                                    app = app,
                                    onFocusGain = { focusedApp, color ->
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
                                        .then(if (index == 0) Modifier.focusRequester(dockFocusRequester) else Modifier)
                                )
                            }

                            // 8th Permanent Slot: "Tất cả" (All Apps) Shortcut
                            AllAppsDockCard(
                                onFocusGain = { color ->
                                    isAllAppsFocused = true
                                    activeApp = null
                                    activeColor = color
                                },
                                onClick = {
                                    viewModel.openAllAppsDialog()
                                },
                                modifier = Modifier.focusProperties {
                                    up = settingsFocusRequester
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- Dialogs ---

        // App Context Menu Dialog
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
                onDismiss = { viewModel.closeAllAppsDialog() }
            )
        }
    }
}

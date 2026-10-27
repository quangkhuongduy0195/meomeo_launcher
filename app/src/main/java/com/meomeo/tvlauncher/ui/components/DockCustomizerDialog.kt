package com.meomeo.tvlauncher.ui.components

import com.meomeo.tvlauncher.ui.theme.AppIconShape
import com.meomeo.tvlauncher.ui.theme.DialogDim
import com.meomeo.tvlauncher.ui.theme.glassIdleColor
import com.meomeo.tvlauncher.ui.theme.glassFocusedColor
import com.meomeo.tvlauncher.ui.theme.glassFill
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.meomeo.tvlauncher.model.AppInfo
import com.meomeo.tvlauncher.ui.theme.Accent
import com.meomeo.tvlauncher.ui.theme.AccentGlow
import com.meomeo.tvlauncher.ui.theme.ColorExtractor
import com.meomeo.tvlauncher.ui.theme.TextPrimary
import com.meomeo.tvlauncher.ui.theme.TextSecondary

@Composable
fun DockCustomizerDialog(
    dockApps: List<AppInfo?>,
    allApps: List<AppInfo>,
    onSetDockSlot: (Int, AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var focusedSlotIndex by remember { mutableIntStateOf(0) }
    var pickingForSlot by remember { mutableStateOf<Int?>(null) }

    val currentSlotApp = dockApps.getOrNull(focusedSlotIndex)
    val currentBitmap = remember(currentSlotApp?.packageName) {
        if (currentSlotApp != null) drawableToBitmap(currentSlotApp.icon ?: currentSlotApp.banner) else null
    }

    val currentDominantColor = remember(currentSlotApp?.packageName, currentBitmap) {
        ColorExtractor.getDominantColor(currentSlotApp?.packageName ?: "default", currentBitmap)
    }

    val ambientColor by animateColorAsState(
        targetValue = currentDominantColor,
        animationSpec = tween(400),
        label = "customizerAmbient"
    )

    val slotFocusRequesters = remember(dockApps.size) { List(dockApps.size.coerceAtLeast(1)) { FocusRequester() } }
    val firstAppFocusRequester = remember { FocusRequester() }

    LaunchedEffect(pickingForSlot) {
        kotlinx.coroutines.delay(100)
        try {
            if (pickingForSlot != null) {
                firstAppFocusRequester.requestFocus()
            } else {
                val targetIndex = focusedSlotIndex.coerceIn(0, (dockApps.size - 1).coerceAtLeast(0))
                slotFocusRequesters[targetIndex].requestFocus()
            }
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = {
            if (pickingForSlot != null) {
                pickingForSlot = null
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        DialogDim()
        // Intercept back button when inside picker mode inside the Dialog window
        BackHandler(enabled = pickingForSlot != null) {
            pickingForSlot = null
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f) // Spacious Cinema ratio
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(32.dp))
                .background(glassIdleColor())
                .border(1.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(32.dp))
        ) {
            // Dynamic Ambient Aura in Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ambientColor.copy(alpha = 0.40f),
                                Color.Transparent
                            ),
                            radius = 1100f
                        )
                    )
            )

            AnimatedContent(
                targetState = pickingForSlot,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { width -> width / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> -width / 3 } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> width / 3 } + fadeOut())
                    }
                },
                label = "customizerPageTransition"
            ) { slotPickingTarget ->
                if (slotPickingTarget == null) {
                    // ==========================================
                    // VIEW A: 8-Slot Stage Showcase
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 36.dp, vertical = 26.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFFF43F5E), Color(0xFF8B5CF6))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoFixHigh,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Quản lý ${dockApps.size} ứng dụng thanh đáy",
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Di chuyển remote để chọn vị trí • Nhấn OK để đổi ứng dụng (Vị trí 8 là Tất cả)",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(glassIdleColor())
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Phím Back để hoàn tất",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }

                        // Center: Active Slot Showcase Stage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Big Squircle Icon (Clean, no nested ugly boxes)
                            Box(
                                modifier = Modifier
                                    .size(94.dp)
                                    .shadow(
                                        elevation = 28.dp,
                                        shape = RoundedCornerShape(26.dp),
                                        ambientColor = ambientColor,
                                        spotColor = ambientColor
                                    )
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(glassFocusedColor()),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentBitmap != null) {
                                    Image(
                                        bitmap = currentBitmap.asImageBitmap(),
                                        contentDescription = currentSlotApp?.label,
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(18.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Trống",
                                        tint = Color.White,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(28.dp))

                            // App Info & Prompt
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ambientColor)
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "VỊ TRÍ ${focusedSlotIndex + 1} / ${dockApps.size}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Text(
                                        text = if (currentSlotApp?.isTvApp == true) "Ứng dụng TV chuẩn" else "Ứng dụng Android",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = currentSlotApp?.label ?: "Vị trí đang trống",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Bấm OK bên dưới để mở danh sách ứng dụng và đổi vị trí này",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Bottom: Live 8-Slot Horizontal Shelf
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(glassIdleColor())
                                    .border(1.2.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    dockApps.forEachIndexed { index, app ->
                                        StageSlotCard(
                                            slotIndex = index,
                                            app = app,
                                            isCurrentlyActive = index == focusedSlotIndex,
                                            onFocused = {
                                                focusedSlotIndex = index
                                            },
                                            onClick = {
                                                focusedSlotIndex = index
                                                pickingForSlot = index
                                            },
                                            modifier = Modifier.focusRequester(slotFocusRequesters[index.coerceIn(0, slotFocusRequesters.size - 1)])
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // VIEW B: App Picker Grid for Slot
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 36.dp, vertical = 26.dp)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(colors = listOf(glassFocusedColor(), glassFocusedColor()))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Apps,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Chọn ứng dụng thay thế",
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(glassFocusedColor())
                                                .padding(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "VỊ TRÍ ${slotPickingTarget + 1} / ${dockApps.size}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Chọn một ứng dụng để hiển thị cố định ở vị trí này (${allApps.size} ứng dụng có sẵn)",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(glassIdleColor())
                                        .clickable { pickingForSlot = null }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "← Phím Back để quay lại",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // App Grid
                        val gridState = rememberLazyGridState()

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            state = gridState,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp, start = 10.dp, end = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(
                                items = allApps,
                                key = { _, it -> it.packageName }
                            ) { index, app ->
                                val isCurrentlyInThisSlot = dockApps.getOrNull(slotPickingTarget)?.packageName == app.packageName
                                PickerAppTile(
                                    app = app,
                                    isSelectedCurrent = isCurrentlyInThisSlot,
                                    onClick = {
                                        focusedSlotIndex = slotPickingTarget
                                        onSetDockSlot(slotPickingTarget, app)
                                        pickingForSlot = null
                                    },
                                    modifier = if (index == 0) Modifier.focusRequester(firstAppFocusRequester) else Modifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StageSlotCard(
    slotIndex: Int,
    app: AppInfo?,
    isCurrentlyActive: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val iconBitmap = remember(app?.packageName) {
        if (app != null) drawableToBitmap(app.icon ?: app.banner) else null
    }

    val slotColor = remember(app?.packageName, iconBitmap) {
        ColorExtractor.getDominantColor(app?.packageName ?: "slot_$slotIndex", iconBitmap)
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.20f else if (isCurrentlyActive) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "stageSlotScale"
    )



    val cardShape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .width(88.dp)
            .scale(scale)
            .zIndex(if (isFocused) 20f else if (isCurrentlyActive) 5f else 1f)
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) {
                    onFocused()
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Number badge above card
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isFocused || isCurrentlyActive) glassFocusedColor() else glassIdleColor())
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${slotIndex + 1}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Card Container
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(cardShape)
                .background(glassFill(isFocused)),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap.asImageBitmap(),
                    contentDescription = app?.label,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Trống",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = app?.label ?: "Trống",
            fontSize = 12.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            color = if (isFocused) Color.White else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun PickerAppTile(
    app: AppInfo,
    isSelectedCurrent: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val bitmap = remember(app.packageName) {
        drawableToBitmap(app.icon ?: app.banner)
    }

    val dominantColor = remember(app.packageName, bitmap) {
        ColorExtractor.getDominantColor(app.packageName, bitmap)
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.14f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pickerTileScale"
    )



    val tileShape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .zIndex(if (isFocused) 10f else 1f)
            .clip(tileShape)
            .background(glassFill(isFocused))

            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(AppIconShape),
                    contentScale = ContentScale.Fit
                )
            }

            if (isSelectedCurrent) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Đang chọn",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = app.label,
            fontSize = 13.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            color = if (isFocused) Color.White else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        )
    }
}

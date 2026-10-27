package com.meomeo.tvlauncher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import com.meomeo.tvlauncher.ui.theme.LocalGlassPalette
import com.meomeo.tvlauncher.ui.theme.QuicksandFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.meomeo.tvlauncher.timer.ViewingTimerStore
import com.meomeo.tvlauncher.ui.PinResult
import com.meomeo.tvlauncher.ui.theme.DialogDim
import com.meomeo.tvlauncher.ui.theme.TextPrimary
import com.meomeo.tvlauncher.ui.theme.TextSecondary
import com.meomeo.tvlauncher.ui.theme.glassFill
import com.meomeo.tvlauncher.ui.theme.glassIdleColor
import kotlinx.coroutines.delay

private val ErrorColor = Color(0xFFFF8A80)

// ---------------------------------------------------------------------------------------------
// Numeric keypad + single PIN input
// ---------------------------------------------------------------------------------------------

/**
 * The only input on screen: [ViewingTimerStore.PIN_LENGTH] dots that fill as digits are typed,
 * with an on-screen numeric keypad. Remote number keys and DEL work too. [onComplete] fires as
 * soon as the last digit is entered; bump [resetKey] to clear the dots (e.g. after a wrong PIN).
 */
@Composable
fun PasscodePad(
    title: String,
    subtitle: String,
    errorMessage: String?,
    resetKey: Int,
    enabled: Boolean,
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var digits by remember(resetKey) { mutableStateOf("") }
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(resetKey) {
        delay(120)
        try {
            firstFocus.requestFocus()
        } catch (_: Exception) {}
    }

    fun append(digit: Char) {
        if (!enabled || digits.length >= ViewingTimerStore.PIN_LENGTH) return
        digits += digit
        if (digits.length == ViewingTimerStore.PIN_LENGTH) onComplete(digits)
    }

    fun backspace() {
        if (digits.isNotEmpty()) digits = digits.dropLast(1)
    }

    Column(
        modifier = modifier.onPreviewKeyEvent { event ->
            val key = event.nativeKeyEvent
            if (key.action != KeyEvent.ACTION_DOWN || key.repeatCount != 0) return@onPreviewKeyEvent false
            when (key.keyCode) {
                in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                    append('0' + (key.keyCode - KeyEvent.KEYCODE_0)); true
                }
                in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> {
                    append('0' + (key.keyCode - KeyEvent.KEYCODE_NUMPAD_0)); true
                }
                KeyEvent.KEYCODE_DEL -> { backspace(); true }
                else -> false
            }
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(ViewingTimerStore.PIN_LENGTH) { index ->
                val filled = index < digits.length
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (filled) Color.White else Color.Transparent)
                        .border(2.dp, Color.White.copy(alpha = if (filled) 1f else 0.45f), CircleShape)
                )
            }
        }

        // Fixed-height slot so the keypad does not jump when an error appears.
        Box(modifier = Modifier.height(36.dp), contentAlignment = Alignment.Center) {
            if (errorMessage != null) {
                Text(text = errorMessage, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ErrorColor, textAlign = TextAlign.Center)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            listOf("123", "456", "789").forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { digit ->
                        PadKey(
                            label = digit.toString(),
                            onClick = { append(digit) },
                            modifier = if (digit == '5') Modifier.focusRequester(firstFocus) else Modifier
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PadKey(label = "Xóa", onClick = { digits = "" })
                PadKey(label = "0", onClick = { append('0') })
                PadKey(icon = Icons.AutoMirrored.Rounded.Backspace, description = "Xóa một số", onClick = { backspace() })
            }
        }
    }
}

@Composable
private fun PadKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    description: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.08f else 1f, label = "padKeyScale")

    Box(
        modifier = modifier
            .size(width = 84.dp, height = 58.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(glassFill(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(26.dp))
        } else if (label != null) {
            Text(
                text = label,
                fontSize = if (label.length == 1) 24.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// PIN flows
// ---------------------------------------------------------------------------------------------

/** Enter the existing PIN. Handles wrong-attempt messages and the lockout countdown. */
@Composable
fun VerifyPinContent(
    title: String,
    subtitle: String,
    lockoutUntil: Long,
    onVerify: (String) -> PinResult,
    resetKey: Int = 0,
    modifier: Modifier = Modifier
) {
    var wrongKey by remember { mutableIntStateOf(0) }
    var showWrong by remember { mutableStateOf(false) }
    val now by produceState(System.currentTimeMillis(), lockoutUntil) {
        while (true) {
            value = System.currentTimeMillis()
            delay(500)
        }
    }
    val lockedOut = now < lockoutUntil
    val error = when {
        lockedOut -> "Nhập sai quá nhiều lần. Thử lại sau ${((lockoutUntil - now) / 1000) + 1} giây"
        showWrong -> "Mã PIN không đúng"
        else -> null
    }

    PasscodePad(
        title = title,
        subtitle = subtitle,
        errorMessage = error,
        resetKey = resetKey * 1000 + wrongKey,
        enabled = !lockedOut,
        onComplete = { pin ->
            showWrong = onVerify(pin) == PinResult.WRONG
            wrongKey++ // clears the dots after a wrong attempt (harmless after success)
        },
        modifier = modifier
    )
}

/** Choose a new PIN, then type it again to confirm. */
@Composable
fun CreatePinContent(
    title: String,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var first by remember { mutableStateOf<String?>(null) }
    var mismatch by remember { mutableStateOf(false) }
    var resetKey by remember { mutableIntStateOf(0) }

    PasscodePad(
        title = if (first == null) title else "Xác nhận mã PIN",
        subtitle = if (first == null) "Đặt mã PIN gồm ${ViewingTimerStore.PIN_LENGTH} chữ số" else "Nhập lại mã PIN vừa tạo",
        errorMessage = if (mismatch) "Hai mã PIN không khớp. Hãy thử lại" else null,
        resetKey = resetKey,
        enabled = true,
        onComplete = { pin ->
            val firstPin = first
            when {
                firstPin == null -> { first = pin; mismatch = false }
                firstPin == pin -> onSaved(pin)
                else -> { first = null; mismatch = true }
            }
            resetKey++
        },
        modifier = modifier
    )
}

// ---------------------------------------------------------------------------------------------
// Dialog shell + timer settings
// ---------------------------------------------------------------------------------------------

@Composable
fun TimerDialogFrame(
    onDismiss: () -> Unit,
    width: Dp = 440.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        DialogDim()
        Column(
            modifier = Modifier
                .width(width)
                .clip(RoundedCornerShape(28.dp))
                .background(glassIdleColor())
                .border(1.2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                .heightIn(max = 500.dp)
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

/**
 * Pick the viewing time (1..60 minutes), start the timer, cancel it, or change the PIN.
 * @param endAt wall-clock end of the running timer, 0 if none.
 */
@Composable
fun TimerSettingsDialog(
    initialMinutes: Int,
    endAt: Long,
    overlayGranted: Boolean,
    onGrantOverlay: () -> Unit,
    onStart: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onChangePin: () -> Unit,
    onDismiss: () -> Unit
) {
    var minutes by remember { mutableIntStateOf(initialMinutes) }
    val firstFocus = remember { FocusRequester() }
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    LaunchedEffect(Unit) {
        delay(120)
        try {
            firstFocus.requestFocus()
        } catch (_: Exception) {}
    }

    val status = when {
        endAt == 0L -> "Chưa đặt hẹn giờ"
        now >= endAt -> "Đã hết giờ xem"
        else -> {
            val left = (endAt - now) / 1000
            "Đang chạy • còn %02d:%02d".format(left / 60, left % 60)
        }
    }

    TimerDialogFrame(onDismiss = onDismiss, width = 520.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            Text("Hẹn giờ xem", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(Modifier.height(4.dp))
        Text(status, fontSize = 14.sp, color = TextSecondary)

        Spacer(Modifier.height(22.dp))
        Text(
            "Thời gian xem (tối đa ${ViewingTimerStore.MAX_MINUTES} phút)",
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val min = ViewingTimerStore.MIN_MINUTES
            val max = ViewingTimerStore.MAX_MINUTES
            StepButton("-5", { minutes = (minutes - 5).coerceAtLeast(min) }, Modifier.focusRequester(firstFocus))
            StepButton("-1", { minutes = (minutes - 1).coerceAtLeast(min) })
            Text(
                text = "$minutes phút",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(130.dp)
            )
            StepButton("+1", { minutes = (minutes + 1).coerceAtMost(max) })
            StepButton("+5", { minutes = (minutes + 5).coerceAtMost(max) })
        }

        if (!overlayGranted) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Chưa cấp quyền \"Hiển thị trên ứng dụng khác\". Khi đang xem app khác, màn hình khóa có thể không tự bật lên khi hết giờ.",
                fontSize = 13.sp,
                color = Color(0xFFFFD180),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            MenuDialogItem(
                icon = Icons.Rounded.Layers,
                iconTint = Color(0xFFFFD180),
                text = "Cấp quyền hiển thị trên ứng dụng khác",
                onClick = onGrantOverlay
            )
        }

        Spacer(Modifier.height(if (overlayGranted) 22.dp else 10.dp))
        MenuDialogItem(
            icon = Icons.Rounded.PlayArrow,
            iconTint = Color.White,
            text = if (endAt == 0L) "Bắt đầu hẹn giờ $minutes phút" else "Đặt lại hẹn giờ $minutes phút",
            onClick = { onStart(minutes) }
        )
        if (endAt != 0L) {
            Spacer(Modifier.height(10.dp))
            MenuDialogItem(
                icon = Icons.Rounded.TimerOff,
                iconTint = Color(0xFFEF4444),
                text = "Hủy hẹn giờ",
                onClick = onCancelTimer
            )
        }
        Spacer(Modifier.height(10.dp))
        MenuDialogItem(
            icon = Icons.Rounded.Lock,
            iconTint = Color.White,
            text = "Đổi mã PIN",
            onClick = onChangePin
        )
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "stepScale")
    Box(
        modifier = modifier
            .size(width = 64.dp, height = 52.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(glassFill(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ---------------------------------------------------------------------------------------------
// Lock screen (macOS style)
// ---------------------------------------------------------------------------------------------

private val LockTextShadow = Shadow(color = Color.Black.copy(alpha = 0.30f), offset = Offset(0f, 4f), blurRadius = 18f)
private val ViLocale = Locale("vi", "VN")

/**
 * Shown when viewing time is up, modelled on the macOS lock screen: the wallpaper, a small date
 * over a huge glassy clock, and a round icon at the bottom. The PIN keypad only appears once the
 * icon is selected; Back returns to the icon.
 *
 * @param background the home-screen wallpaper layers, so the lock screen looks like the home screen.
 */
@Composable
fun LockScreen(
    background: @Composable () -> Unit,
    lockoutUntil: Long,
    onVerify: (String) -> PinResult,
    resetKey: Int,
    modifier: Modifier = Modifier
) {
    // A new flow state (e.g. coming back from the timer panel) starts at the icon again.
    var showPin by remember(resetKey) { mutableStateOf(false) }
    val iconFocus = remember { FocusRequester() }

    BackHandler(enabled = showPin) { showPin = false }
    LaunchedEffect(showPin, resetKey) {
        if (!showPin) {
            delay(150)
            try {
                iconFocus.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val blur by animateDpAsState(if (showPin) 32.dp else 0.dp, tween(300), label = "lockBlur")
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    // Mostly white (legible on any photo) with a hint of the wallpaper tone, like the macOS clock.
    val clockTint = lerp(Color.White, LocalGlassPalette.current.focused.copy(alpha = 1f), 0.30f).copy(alpha = 0.95f)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxSize().then(if (blur > 0.dp) Modifier.blur(blur) else Modifier)) {
            background()
            // Light scrim so bright wallpapers do not wash out the clock.
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))

            AnimatedVisibility(
                visible = !showPin,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(200))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 44.dp, bottom = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val date = Date(now)
                    Text(
                        text = SimpleDateFormat("EEEE, d 'tháng' M", ViLocale).format(date)
                            .replaceFirstChar { it.titlecase(ViLocale) },
                        style = TextStyle(fontFamily = QuicksandFamily, shadow = LockTextShadow),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = clockTint
                    )
                    Text(
                        text = SimpleDateFormat("HH:mm", ViLocale).format(date),
                        style = TextStyle(fontFamily = QuicksandFamily, shadow = LockTextShadow),
                        fontSize = 150.sp,
                        fontWeight = FontWeight.Bold,
                        color = clockTint
                    )
                    Spacer(Modifier.weight(1f))
                    LockIconButton(onClick = { showPin = true }, modifier = Modifier.focusRequester(iconFocus))
                }
            }
        }

        if (showPin) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(28.dp))
                    .background(glassIdleColor())
                    .border(1.2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VerifyPinContent(
                    title = "Nhập mã PIN",
                    subtitle = "Để đặt lại hoặc hủy hẹn giờ xem",
                    lockoutUntil = lockoutUntil,
                    onVerify = onVerify,
                    resetKey = resetKey
                )
            }
        }
    }
}

/** The round "avatar" at the bottom of the lock screen; selecting it reveals the PIN keypad. */
@Composable
private fun LockIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isFocused) 1.12f else 1f, label = "lockIconScale")
    val label = TextStyle(fontFamily = QuicksandFamily, shadow = LockTextShadow)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = modifier
                .size(76.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(glassFill(isFocused))
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = "Nhập mã PIN", tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Đã hết giờ xem", style = label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text("Nhấn OK để nhập mã PIN", style = label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
    }
}

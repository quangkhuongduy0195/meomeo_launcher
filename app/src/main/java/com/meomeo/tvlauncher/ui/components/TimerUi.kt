package com.meomeo.tvlauncher.ui.components

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meomeo.tvlauncher.R
import com.meomeo.tvlauncher.timer.ViewingTimerStore
import com.meomeo.tvlauncher.ui.PinResult
import com.meomeo.tvlauncher.ui.theme.Danger
import com.meomeo.tvlauncher.ui.theme.Label
import com.meomeo.tvlauncher.ui.theme.LabelSecondary
import com.meomeo.tvlauncher.ui.theme.SystemGray
import com.meomeo.tvlauncher.ui.theme.SystemGreen
import com.meomeo.tvlauncher.ui.theme.SystemOrange
import com.meomeo.tvlauncher.ui.theme.SystemPurple
import com.meomeo.tvlauncher.ui.theme.SystemRed
import com.meomeo.tvlauncher.ui.theme.WindowRadius
import com.meomeo.tvlauncher.ui.theme.WindowTint
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------------------------
// Numeric keypad + single PIN input
// ---------------------------------------------------------------------------------------------

/**
 * The only input on screen: [ViewingTimerStore.PIN_LENGTH] dots that fill as digits are typed,
 * with a round on-screen keypad. Remote number keys and DEL work too. [onComplete] fires as
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
        AlertText(title, subtitle)

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(ViewingTimerStore.PIN_LENGTH) { index ->
                val filled = index < digits.length
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (filled) Color.White else Color.Transparent)
                        .border(1.5.dp, Color.White.copy(alpha = if (filled) 1f else 0.55f), CircleShape)
                )
            }
        }

        // Fixed-height slot so the keypad does not jump when an error appears.
        Box(modifier = Modifier.height(34.dp), contentAlignment = Alignment.Center) {
            if (errorMessage != null) {
                Text(text = errorMessage, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Danger, textAlign = TextAlign.Center)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            listOf("123", "456", "789").forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { digit ->
                        PadKey(
                            label = digit.toString(),
                            onClick = { append(digit) },
                            modifier = if (digit == '5') Modifier.focusRequester(firstFocus) else Modifier
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
    FocusSurface(
        onClick = onClick,
        modifier = modifier.size(66.dp),
        cornerRadius = 33.dp,
        color = Color.White.copy(alpha = 0.16f),
        focusScale = 1.06f,
        ring = true
    ) { _ ->
        if (icon != null) {
            Icon(icon, contentDescription = description, tint = Label, modifier = Modifier.size(24.dp))
        } else if (label != null) {
            Text(
                text = label,
                fontSize = if (label.length == 1) 28.sp else 14.sp,
                fontWeight = if (label.length == 1) FontWeight.Normal else FontWeight.Medium,
                color = Label
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

/** A narrow window for PIN entry and the timer panel. */
@Composable
fun TimerDialogFrame(
    onDismiss: () -> Unit,
    width: Dp = 380.dp,
    title: String = "Hẹn giờ xem",
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    MacWindow(
        onDismiss = onDismiss,
        title = title,
        width = width,
        maxHeight = 520.dp,
        contentAlignment = Alignment.CenterHorizontally,
        content = content
    )
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
        IconTile(Icons.Rounded.Timer, SystemOrange, size = 52.dp)
        Spacer(Modifier.height(10.dp))
        AlertText("Hẹn giờ xem", status)

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val min = ViewingTimerStore.MIN_MINUTES
            val max = ViewingTimerStore.MAX_MINUTES
            StepButton("-5", { minutes = (minutes - 5).coerceAtLeast(min) }, Modifier.focusRequester(firstFocus))
            StepButton("-1", { minutes = (minutes - 1).coerceAtLeast(min) })
            Text(
                text = "$minutes phút",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Label,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(150.dp)
            )
            StepButton("+1", { minutes = (minutes + 1).coerceAtMost(max) })
            StepButton("+5", { minutes = (minutes + 5).coerceAtMost(max) })
        }
        Spacer(Modifier.height(4.dp))
        Text("Tối đa ${ViewingTimerStore.MAX_MINUTES} phút", fontSize = 12.sp, color = LabelSecondary)

        if (!overlayGranted) {
            Spacer(Modifier.height(14.dp))
            Text(
                "Chưa cấp quyền \"Hiển thị trên ứng dụng khác\". Khi đang xem app khác, màn hình khóa có thể không tự bật lên khi hết giờ.",
                fontSize = 12.sp,
                color = SystemOrange,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            InsetGroup {
                MacRow(
                    icon = Icons.Rounded.Layers,
                    iconColor = SystemOrange,
                    title = "Cấp quyền hiển thị trên ứng dụng khác",
                    chevron = true,
                    onClick = onGrantOverlay
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        InsetGroup {
            MacRow(
                icon = Icons.Rounded.PlayArrow,
                iconColor = SystemGreen,
                title = if (endAt == 0L) "Bắt đầu hẹn giờ $minutes phút" else "Đặt lại hẹn giờ $minutes phút",
                onClick = { onStart(minutes) }
            )
            if (endAt != 0L) {
                MacRow(
                    icon = Icons.Rounded.TimerOff,
                    iconColor = SystemRed,
                    title = "Hủy hẹn giờ",
                    danger = true,
                    onClick = onCancelTimer
                )
            }
            MacRow(
                icon = Icons.Rounded.Lock,
                iconColor = SystemGray,
                title = "Đổi mã PIN",
                chevron = true,
                onClick = onChangePin
            )
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier.size(width = 56.dp, height = 44.dp),
        cornerRadius = 10.dp,
        color = Color.White.copy(alpha = 0.16f),
        focusScale = 1.06f,
        ring = true
    ) { _ ->
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Label)
    }
}

// ---------------------------------------------------------------------------------------------
// Lock screen
// ---------------------------------------------------------------------------------------------

private val ViLocale = Locale("vi", "VN")
private val LockShadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 4f), 16f)

/**
 * Shown when viewing time is up, modelled on the macOS lock screen: the blurred wallpaper, a
 * small date over a huge clock, and the user's avatar at the bottom. The PIN keypad only appears
 * once the avatar is selected; Back returns to the clock.
 */
@Composable
fun LockScreen(
    lockoutUntil: Long,
    onVerify: (String) -> PinResult,
    resetKey: Int,
    modifier: Modifier = Modifier
) {
    // A new flow state (e.g. coming back from the timer panel) starts at the avatar again.
    var showPin by remember(resetKey) { mutableStateOf(false) }
    val avatarFocus = remember { FocusRequester() }

    BackHandler(enabled = showPin) { showPin = false }
    LaunchedEffect(showPin, resetKey) {
        if (!showPin) {
            delay(150)
            try {
                avatarFocus.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000L - value % 60_000L + 50L)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        BlurredFill()
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)))

        AnimatedVisibility(
            visible = !showPin,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(120))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 48.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Pill("Đã hết giờ xem")
                Spacer(Modifier.height(18.dp))
                Text(
                    text = SimpleDateFormat("EEEE, d 'tháng' M", ViLocale).format(Date(now))
                        .replaceFirstChar { it.titlecase(ViLocale) },
                    style = TextStyle(shadow = LockShadow),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Label.copy(alpha = 0.92f)
                )
                Text(
                    text = SimpleDateFormat("HH:mm", ViLocale).format(Date(now)),
                    style = TextStyle(shadow = LockShadow),
                    fontSize = 150.sp,
                    fontWeight = FontWeight.Bold,
                    color = Label.copy(alpha = 0.92f)
                )
                Spacer(Modifier.weight(1f))
                FocusSurface(
                    onClick = { showPin = true },
                    modifier = Modifier.focusRequester(avatarFocus).size(76.dp),
                    cornerRadius = 38.dp,
                    color = Color.Transparent,
                    focusedColor = Color.Transparent,
                    focusScale = 1.1f,
                    ring = true
                ) { _ ->
                    // The adaptive launcher icon: its layers span 108dp but only the centre 72dp shows.
                    Box(Modifier.fillMaxSize().clip(CircleShape)) {
                        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Image(painterResource(R.drawable.ic_launcher_foreground), "Mở khóa", Modifier.fillMaxSize().scale(1.5f))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("MeoMeo", style = TextStyle(shadow = LockShadow), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Label)
                Text("Nhấn OK để nhập mã PIN", style = TextStyle(shadow = LockShadow), fontSize = 13.sp, color = Label.copy(alpha = 0.75f))
            }
        }

        if (showPin) {
            FrostedBox(
                modifier = Modifier.align(Alignment.Center),
                radius = WindowRadius,
                tint = WindowTint
            ) {
                Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
}

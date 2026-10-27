package com.meomeo.tvlauncher.ui

import androidx.compose.runtime.Composable
import com.meomeo.tvlauncher.ui.components.CreatePinContent
import com.meomeo.tvlauncher.ui.components.TimerDialogFrame
import com.meomeo.tvlauncher.ui.components.TimerSettingsDialog
import com.meomeo.tvlauncher.ui.components.VerifyPinContent

/** Renders whichever viewing-timer dialog the ViewModel says is active. */
@Composable
fun TimerFlowHost(uiState: MainUiState, viewModel: MainViewModel) {
    when (uiState.timerFlow) {
        null -> Unit

        TimerFlow.VERIFY -> TimerDialogFrame(onDismiss = viewModel::closeTimerFlow) {
            VerifyPinContent(
                title = "Nhập mã PIN",
                subtitle = "Nhập mã PIN để mở cài đặt hẹn giờ",
                lockoutUntil = uiState.pinLockoutUntil,
                onVerify = viewModel::verifyPasscode
            )
        }

        TimerFlow.CREATE_PIN -> TimerDialogFrame(onDismiss = viewModel::closeTimerFlow) {
            CreatePinContent(title = "Tạo mã PIN", onSaved = viewModel::savePasscode)
        }

        TimerFlow.CHANGE_PIN -> TimerDialogFrame(onDismiss = viewModel::showTimerSettings) {
            CreatePinContent(title = "Đổi mã PIN", onSaved = viewModel::savePasscode)
        }

        TimerFlow.SETTINGS -> TimerSettingsDialog(
            initialMinutes = uiState.timerMinutes,
            endAt = uiState.timerEndAt,
            overlayGranted = uiState.overlayGranted,
            onGrantOverlay = viewModel::openOverlayPermissionSettings,
            onStart = viewModel::startTimer,
            onCancelTimer = viewModel::cancelTimer,
            onChangePin = viewModel::startChangePin,
            onDismiss = viewModel::closeTimerFlow
        )
    }
}

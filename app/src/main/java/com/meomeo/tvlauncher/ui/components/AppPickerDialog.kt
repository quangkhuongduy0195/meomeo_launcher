package com.meomeo.tvlauncher.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.meomeo.tvlauncher.data.AppRepository
import com.meomeo.tvlauncher.model.AppInfo
import kotlinx.coroutines.delay

/** Pick the app for one dock slot, from a Launchpad page. */
@Composable
fun AppPickerDialog(
    slotIndex: Int,
    allApps: List<AppInfo>,
    onSelectApp: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            firstItemFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    LaunchpadDialog(onDismiss = onDismiss) {
        LaunchpadHeader("Chọn ứng dụng", trailing = "Vị trí ${slotIndex + 1} / ${AppRepository.DOCK_SLOTS_COUNT}")
        AppGrid(
            apps = allApps,
            firstItemFocusRequester = firstItemFocusRequester,
            modifier = Modifier.weight(1f)
        ) { app, cellModifier ->
            LaunchpadIcon(app = app, onClick = { onSelectApp(app) }, modifier = cellModifier)
        }
    }
}

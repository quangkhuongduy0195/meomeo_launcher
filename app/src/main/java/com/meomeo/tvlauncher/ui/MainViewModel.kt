package com.meomeo.tvlauncher.ui

import android.app.Application
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meomeo.tvlauncher.data.AppRepository
import com.meomeo.tvlauncher.data.GooglePhotosWallpaperManager
import com.meomeo.tvlauncher.model.AppInfo
import com.meomeo.tvlauncher.timer.TimerScheduler
import com.meomeo.tvlauncher.timer.ViewingTimerStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** Steps of the viewing-timer UI; at most one is shown at a time. */
enum class TimerFlow { VERIFY, CREATE_PIN, SETTINGS, CHANGE_PIN }

enum class PinResult { OK, WRONG, LOCKED_OUT }

data class MainUiState(
    val isLoading: Boolean = true,
    val allApps: List<AppInfo> = emptyList(),
    val favoriteApps: List<AppInfo> = emptyList(),
    val dockApps: List<AppInfo?> = List(AppRepository.DOCK_SLOTS_COUNT) { null },
    val includeSideload: Boolean = false,
    val selectedWallpaper: String = "wallpaper_google_photos",
    val googlePhotos: List<File> = emptyList(),
    val currentGooglePhotoIndex: Int = 0,
    val isGooglePhotosSyncing: Boolean = false,
    val googlePhotosLastSyncTime: Long = 0L,
    val selectedAppForMenu: AppInfo? = null,
    val selectedSlotForMenu: Int? = null,
    val isSettingsOpen: Boolean = false,
    val isDockCustomizerOpen: Boolean = false,
    val isWallpaperPickerOpen: Boolean = false,
    /** The wallpaper picker was opened from the home screen, so closing it must not open Settings. */
    val wallpaperPickerFromHome: Boolean = false,
    val isAllAppsDialogOpen: Boolean = false,
    val slotPickingFor: Int? = null, // When picker is open to choose app for this slot

    // --- Viewing timer ---
    val hasPasscode: Boolean = false,
    val timerMinutes: Int = ViewingTimerStore.DEFAULT_MINUTES,
    /** Wall-clock millis when time runs out; 0 = no timer. */
    val timerEndAt: Long = 0L,
    /** Time is up: everything is hidden behind the passcode screen. */
    val isLocked: Boolean = false,
    val timerFlow: TimerFlow? = null,
    /** Passcode was accepted on the lock screen, so the timer panel may be shown (and must offer start/cancel). */
    val isUnlockedForSettings: Boolean = false,
    /** Passcode entry is blocked until this wall-clock time after too many wrong attempts. */
    val pinLockoutUntil: Long = 0L,
    /** "Display over other apps" granted: required for the lock screen to appear over other apps. */
    val overlayGranted: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val googlePhotosManager = GooglePhotosWallpaperManager.getInstance(application)
    private val timerStore = ViewingTimerStore(application)
    private var failedPinAttempts = 0

    private val _uiState = MutableStateFlow(
        MainUiState(
            hasPasscode = timerStore.hasPasscode(),
            timerMinutes = timerStore.minutes,
            timerEndAt = timerStore.endAt,
            isLocked = timerStore.isExpired(),
            overlayGranted = Settings.canDrawOverlays(application),
            includeSideload = repository.isIncludeSideload(),
            selectedWallpaper = repository.getSelectedWallpaper(),
            googlePhotos = googlePhotosManager.getLocalPhotos(),
            googlePhotosLastSyncTime = googlePhotosManager.getLastSyncTime()
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Automatically ensure wallpaper_google_photos is active if not explicitly changed
        if (repository.getSelectedWallpaper() == "wallpaper_sunset") {
            repository.setSelectedWallpaper("wallpaper_google_photos")
            _uiState.update { it.copy(selectedWallpaper = "wallpaper_google_photos") }
        }
        loadApps()
        startGooglePhotosSync()
        startPhotoSlideshow()
        resumeTimer()
    }

    fun syncGooglePhotosNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGooglePhotosSyncing = true) }
            val result = googlePhotosManager.syncAlbum()
            result.onSuccess { files ->
                _uiState.update {
                    it.copy(
                        isGooglePhotosSyncing = false,
                        googlePhotos = files,
                        googlePhotosLastSyncTime = googlePhotosManager.getLastSyncTime(),
                        currentGooglePhotoIndex = 0
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isGooglePhotosSyncing = false) }
            }
        }
    }

    private fun startGooglePhotosSync() {
        viewModelScope.launch {
            syncGooglePhotosNow()
            // Auto periodic sync every 15 minutes to automatically catch changes in Google Photos album
            while (true) {
                delay(15 * 60 * 1000L)
                syncGooglePhotosNow()
            }
        }
    }

    private fun startPhotoSlideshow() {
        viewModelScope.launch {
            while (true) {
                // Rotate wallpaper smoothly every 45 seconds if multiple photos are in the album
                delay(45 * 1000L)
                val currentPhotos = _uiState.value.googlePhotos
                if (currentPhotos.size > 1 && _uiState.value.selectedWallpaper == "wallpaper_google_photos") {
                    _uiState.update { state ->
                        state.copy(
                            currentGooglePhotoIndex = (state.currentGooglePhotoIndex + 1) % currentPhotos.size
                        )
                    }
                }
            }
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            // Only block the UI with a spinner on the first load. onResume reloads on every return
            // to Home; swapping the whole screen for a spinner there causes a flash and loses focus.
            _uiState.update { it.copy(isLoading = it.allApps.isEmpty()) }
            val apps = repository.getInstalledApps(includeSideload = _uiState.value.includeSideload)
            val favorites = apps.filter { it.isFavorite }

            // Resolve 8 dock apps
            var dockPackages = repository.getDockPackageNames()
            if (dockPackages.isEmpty() && apps.isNotEmpty()) {
                // Initialize default 8 dock apps from installed apps
                dockPackages = apps.take(AppRepository.DOCK_SLOTS_COUNT).map { it.packageName }
                repository.saveDockPackageNames(dockPackages)
            }

            val appMap = apps.associateBy { it.packageName }
            val resolvedDock = (0 until AppRepository.DOCK_SLOTS_COUNT).map { idx ->
                val pkg = dockPackages.getOrNull(idx)
                if (!pkg.isNullOrBlank()) appMap[pkg] else null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    allApps = apps,
                    favoriteApps = favorites,
                    dockApps = resolvedDock,
                    selectedWallpaper = repository.getSelectedWallpaper()
                )
            }
        }
    }

    fun setDockSlot(slotIndex: Int, app: AppInfo, keepCustomizerOpen: Boolean = false) {
        repository.setDockSlot(slotIndex, app.packageName, _uiState.value.allApps)
        _uiState.update { current ->
            val updatedDock = current.dockApps.toMutableList()
            while (updatedDock.size <= slotIndex) {
                updatedDock.add(null)
            }
            if (slotIndex in 0 until updatedDock.size) {
                updatedDock[slotIndex] = app
            }
            current.copy(
                dockApps = updatedDock,
                slotPickingFor = null,
                isDockCustomizerOpen = if (keepCustomizerOpen) current.isDockCustomizerOpen else false,
                isSettingsOpen = if (keepCustomizerOpen) current.isSettingsOpen else false
            )
        }
    }

    fun openSlotPicker(slotIndex: Int) {
        _uiState.update { it.copy(isSettingsOpen = false, isDockCustomizerOpen = false, isWallpaperPickerOpen = false, slotPickingFor = slotIndex) }
    }

    fun closeSlotPicker() {
        _uiState.update { it.copy(slotPickingFor = null) }
    }

    fun selectWallpaper(wallpaperId: String) {
        repository.setSelectedWallpaper(wallpaperId)
        _uiState.update { it.copy(selectedWallpaper = wallpaperId, isWallpaperPickerOpen = false) }
    }

    fun openDockCustomizer() {
        _uiState.update { it.copy(isSettingsOpen = false, isDockCustomizerOpen = true, isWallpaperPickerOpen = false, slotPickingFor = null) }
    }

    fun closeDockCustomizer() {
        _uiState.update { it.copy(isDockCustomizerOpen = false, isSettingsOpen = true, slotPickingFor = null) }
    }

    fun openWallpaperPicker(fromHome: Boolean = false) {
        _uiState.update {
            it.copy(
                isSettingsOpen = false, isDockCustomizerOpen = false, isWallpaperPickerOpen = true,
                wallpaperPickerFromHome = fromHome, slotPickingFor = null
            )
        }
    }

    fun closeWallpaperPicker() {
        _uiState.update { it.copy(isWallpaperPickerOpen = false, isSettingsOpen = !it.wallpaperPickerFromHome) }
    }

    fun openSettingsDialog() {
        _uiState.update { it.copy(isSettingsOpen = true, isDockCustomizerOpen = false, isWallpaperPickerOpen = false, isAllAppsDialogOpen = false, slotPickingFor = null) }
    }

    fun closeSettingsDialog() {
        _uiState.update { it.copy(isSettingsOpen = false, isDockCustomizerOpen = false, isWallpaperPickerOpen = false, isAllAppsDialogOpen = false, slotPickingFor = null) }
    }

    fun openAllAppsDialog() {
        _uiState.update { it.copy(isAllAppsDialogOpen = true, isSettingsOpen = false, isDockCustomizerOpen = false, isWallpaperPickerOpen = false, slotPickingFor = null) }
    }

    fun closeAllAppsDialog() {
        _uiState.update { it.copy(isAllAppsDialogOpen = false) }
    }

    fun toggleFavorite(app: AppInfo) {
        val willBeFav = repository.toggleFavorite(app.packageName)
        _uiState.update { state ->
            val updatedAll = state.allApps.map {
                if (it.packageName == app.packageName) it.copy(isFavorite = willBeFav) else it
            }
            val updatedFav = updatedAll.filter { it.isFavorite }
            // Dock holds its own copies of AppInfo; keep them in sync so the star badge updates.
            val updatedDock = state.dockApps.map { dockApp ->
                if (dockApp?.packageName == app.packageName) dockApp.copy(isFavorite = willBeFav) else dockApp
            }
            state.copy(allApps = updatedAll, favoriteApps = updatedFav, dockApps = updatedDock)
        }
    }

    fun toggleIncludeSideload() {
        val newValue = !_uiState.value.includeSideload
        repository.setIncludeSideload(newValue)
        _uiState.update { it.copy(includeSideload = newValue) }
        loadApps()
    }

    fun launchApp(app: AppInfo) {
        if (_uiState.value.isLocked) return
        repository.launchApp(app)
    }

    fun showAppMenu(app: AppInfo, slotIndex: Int? = null) {
        _uiState.update { it.copy(selectedAppForMenu = app, selectedSlotForMenu = slotIndex) }
    }

    fun hideAppMenu() {
        _uiState.update { it.copy(selectedAppForMenu = null, selectedSlotForMenu = null) }
    }

    fun openAppSettings(app: AppInfo) {
        repository.openAppSettings(app.packageName)
        hideAppMenu()
    }

    fun uninstallApp(app: AppInfo) {
        repository.uninstallApp(app.packageName)
        hideAppMenu()
    }

    fun openSystemSettings() {
        repository.openSystemSettings()
    }

    fun openWifiSettings() {
        repository.openWifiSettings()
    }

    // --- Viewing timer -------------------------------------------------------------------------

    /** Re-arms the alarm after a restart and keeps checking whether time is up. */
    private fun resumeTimer() {
        val endAt = timerStore.endAt
        if (endAt != 0L) {
            val app = getApplication<Application>()
            TimerScheduler.schedule(
                app,
                if (timerStore.isExpired()) System.currentTimeMillis() + TimerScheduler.RELOCK_INTERVAL_MS else endAt
            )
        }
        viewModelScope.launch {
            while (true) {
                checkTimer()
                delay(1000)
            }
        }
    }

    /** Re-reads the "display over other apps" permission (it is granted on a separate system screen). */
    fun refreshOverlayPermission() {
        val granted = Settings.canDrawOverlays(getApplication())
        _uiState.update { it.copy(overlayGranted = granted) }
    }

    fun openOverlayPermissionSettings() {
        val app = getApplication<Application>()
        try {
            app.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            // Many TV builds ship without this settings page.
            Toast.makeText(
                app,
                "Thiết bị không có trang cấp quyền. Cấp bằng ADB: adb shell appops set ${app.packageName} SYSTEM_ALERT_WINDOW allow",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /** Stops whatever another app is playing, so time-up really means time-up. */
    private fun pauseMedia() {
        val audio = getApplication<Application>().getSystemService(AudioManager::class.java) ?: return
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
    }

    /** Locks the launcher as soon as the end time has passed. Also called from onResume. */
    fun checkTimer() {
        if (!timerStore.isExpired() || _uiState.value.isLocked) return
        pauseMedia()
        TimerScheduler.schedule(
            getApplication(),
            System.currentTimeMillis() + TimerScheduler.RELOCK_INTERVAL_MS
        )
        _uiState.update {
            it.copy(
                isLocked = true,
                timerFlow = null,
                isSettingsOpen = false,
                isDockCustomizerOpen = false,
                isWallpaperPickerOpen = false,
                isAllAppsDialogOpen = false,
                slotPickingFor = null,
                selectedAppForMenu = null,
                selectedSlotForMenu = null
            )
        }
    }

    /** Called when the timer alarm brings us to the front: lock now and schedule the next check. */
    fun reassertLock() {
        if (!timerStore.isExpired()) return
        checkTimer()
        pauseMedia()
        TimerScheduler.schedule(
            getApplication(),
            System.currentTimeMillis() + TimerScheduler.RELOCK_INTERVAL_MS
        )
    }

    /** Entry point from Settings: set a passcode first if there is none, otherwise ask for it. */
    fun openTimerSettings() {
        _uiState.update {
            it.copy(
                isSettingsOpen = false,
                timerFlow = when {
                    !timerStore.hasPasscode() -> TimerFlow.CREATE_PIN
                    it.isUnlockedForSettings -> TimerFlow.SETTINGS
                    else -> TimerFlow.VERIFY
                }
            )
        }
    }

    fun closeTimerFlow() {
        // If we are locked this drops back to the lock screen.
        _uiState.update { it.copy(timerFlow = null, isUnlockedForSettings = false) }
    }

    fun verifyPasscode(pin: String): PinResult {
        val now = System.currentTimeMillis()
        if (now < _uiState.value.pinLockoutUntil) return PinResult.LOCKED_OUT
        if (timerStore.verifyPasscode(pin)) {
            failedPinAttempts = 0
            _uiState.update { it.copy(timerFlow = TimerFlow.SETTINGS, isUnlockedForSettings = it.isLocked) }
            return PinResult.OK
        }
        failedPinAttempts++
        if (failedPinAttempts >= MAX_PIN_ATTEMPTS) {
            failedPinAttempts = 0
            _uiState.update { it.copy(pinLockoutUntil = now + PIN_LOCKOUT_MS) }
            return PinResult.LOCKED_OUT
        }
        return PinResult.WRONG
    }

    fun showTimerSettings() {
        _uiState.update { it.copy(timerFlow = TimerFlow.SETTINGS) }
    }

    fun startChangePin() {
        _uiState.update { it.copy(timerFlow = TimerFlow.CHANGE_PIN) }
    }

    /** Saves a new passcode (first-time setup or change) and continues to the timer panel. */
    fun savePasscode(pin: String) {
        timerStore.setPasscode(pin)
        _uiState.update { it.copy(hasPasscode = true, timerFlow = TimerFlow.SETTINGS) }
    }

    fun startTimer(minutes: Int) {
        val m = minutes.coerceIn(ViewingTimerStore.MIN_MINUTES, ViewingTimerStore.MAX_MINUTES)
        val endAt = System.currentTimeMillis() + m * 60_000L
        timerStore.minutes = m
        timerStore.endAt = endAt
        TimerScheduler.schedule(getApplication(), endAt)
        _uiState.update {
            it.copy(
                timerMinutes = m,
                timerEndAt = endAt,
                isLocked = false,
                isUnlockedForSettings = false,
                timerFlow = null
            )
        }
    }

    fun cancelTimer() {
        timerStore.endAt = 0L
        TimerScheduler.cancel(getApplication())
        _uiState.update {
            it.copy(timerEndAt = 0L, isLocked = false, isUnlockedForSettings = false, timerFlow = null)
        }
    }

    private companion object {
        const val MAX_PIN_ATTEMPTS = 5
        const val PIN_LOCKOUT_MS = 30_000L
    }
}

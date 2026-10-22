package com.meomeo.tvlauncher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meomeo.tvlauncher.data.AppRepository
import com.meomeo.tvlauncher.data.GooglePhotosWallpaperManager
import com.meomeo.tvlauncher.model.AppInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

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
    val isAllAppsDialogOpen: Boolean = false,
    val slotPickingFor: Int? = null // When picker is open to choose app for this slot
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val googlePhotosManager = GooglePhotosWallpaperManager.getInstance(application)

    private val _uiState = MutableStateFlow(
        MainUiState(
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
            _uiState.update { it.copy(isLoading = true) }
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
        _uiState.update { it.copy(slotPickingFor = null, isDockCustomizerOpen = true) }
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

    fun openWallpaperPicker() {
        _uiState.update { it.copy(isSettingsOpen = false, isDockCustomizerOpen = false, isWallpaperPickerOpen = true, slotPickingFor = null) }
    }

    fun closeWallpaperPicker() {
        _uiState.update { it.copy(isWallpaperPickerOpen = false, isSettingsOpen = true) }
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
            state.copy(allApps = updatedAll, favoriteApps = updatedFav)
        }
    }

    fun toggleIncludeSideload() {
        val newValue = !_uiState.value.includeSideload
        repository.setIncludeSideload(newValue)
        _uiState.update { it.copy(includeSideload = newValue) }
        loadApps()
    }

    fun launchApp(app: AppInfo) {
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
}

package com.meomeo.tvlauncher.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.meomeo.tvlauncher.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("meomeo_tv_launcher_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "AppRepository"
        private const val KEY_FAVORITES = "favorite_apps"
        private const val KEY_INCLUDE_SIDELOAD = "include_sideload_apps"
        private const val KEY_DOCK_APPS = "dock_apps_v1"
        private const val KEY_WALLPAPER = "selected_wallpaper"
        const val DOCK_SLOTS_COUNT = 7
    }

    fun getSelectedWallpaper(): String {
        return prefs.getString(KEY_WALLPAPER, "wallpaper_google_photos") ?: "wallpaper_google_photos"
    }

    fun setSelectedWallpaper(wallpaperId: String) {
        prefs.edit().putString(KEY_WALLPAPER, wallpaperId).apply()
    }

    fun getDockPackageNames(): List<String> {
        val raw = prefs.getString(KEY_DOCK_APPS, null)
        return if (raw.isNullOrEmpty()) {
            emptyList()
        } else {
            raw.split(",").filter { it.isNotBlank() }.take(DOCK_SLOTS_COUNT)
        }
    }

    fun saveDockPackageNames(packages: List<String>) {
        val truncated = packages.take(DOCK_SLOTS_COUNT)
        prefs.edit().putString(KEY_DOCK_APPS, truncated.joinToString(",")).apply()
    }

    fun setDockSlot(slotIndex: Int, packageName: String, fallbackApps: List<AppInfo>) {
        val current = getDockPackageNames().toMutableList()
        if (current.isEmpty()) {
            // Initialize with first 8 apps
            current.addAll(fallbackApps.take(DOCK_SLOTS_COUNT).map { it.packageName })
        }
        while (current.size < DOCK_SLOTS_COUNT) {
            current.add("")
        }
        if (slotIndex in 0 until DOCK_SLOTS_COUNT) {
            current[slotIndex] = packageName
            saveDockPackageNames(current)
        }
    }

    fun getFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun isFavorite(packageName: String): Boolean {
        return getFavorites().contains(packageName)
    }

    fun toggleFavorite(packageName: String): Boolean {
        val currentFavorites = getFavorites().toMutableSet()
        val willBeFavorite = !currentFavorites.contains(packageName)
        if (willBeFavorite) {
            currentFavorites.add(packageName)
        } else {
            currentFavorites.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_FAVORITES, currentFavorites).apply()
        return willBeFavorite
    }

    fun isIncludeSideload(): Boolean {
        return prefs.getBoolean(KEY_INCLUDE_SIDELOAD, false)
    }

    fun setIncludeSideload(include: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_SIDELOAD, include).apply()
    }

    suspend fun getInstalledApps(includeSideload: Boolean = isIncludeSideload()): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val myPackage = context.packageName
        val favorites = getFavorites()

        // 1. Get Leanback / TV apps
        val tvIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val tvResolveInfos = pm.queryIntentActivities(tvIntent, 0)
        val tvPackageMap = mutableMapOf<String, AppInfo>()

        for (info in tvResolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == myPackage) continue

            val label = info.loadLabel(pm).toString()
            val banner = try {
                info.activityInfo.loadBanner(pm)
            } catch (e: Exception) {
                null
            }
            val icon = info.loadIcon(pm)

            tvPackageMap[pkg] = AppInfo(
                id = pkg,
                label = label,
                packageName = pkg,
                activityName = info.activityInfo.name,
                icon = icon,
                banner = banner,
                isTvApp = true,
                isFavorite = favorites.contains(pkg)
            )
        }

        val allApps = mutableListOf<AppInfo>()
        allApps.addAll(tvPackageMap.values)

        // 2. Query standard non-TV apps if sideload is enabled
        if (includeSideload) {
            val standardIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val standardResolveInfos = pm.queryIntentActivities(standardIntent, 0)

            for (info in standardResolveInfos) {
                val pkg = info.activityInfo.packageName
                if (pkg == myPackage || tvPackageMap.containsKey(pkg)) continue

                val label = info.loadLabel(pm).toString()
                val icon = info.loadIcon(pm)

                allApps.add(
                    AppInfo(
                        id = pkg,
                        label = label,
                        packageName = pkg,
                        activityName = info.activityInfo.name,
                        icon = icon,
                        banner = null,
                        isTvApp = false,
                        isFavorite = favorites.contains(pkg)
                    )
                )
            }
        }

        // Sort alphabetically by label
        allApps.sortedBy { it.label.lowercase() }
    }

    fun launchApp(app: AppInfo) {
        val pm = context.packageManager
        try {
            var launchIntent: Intent? = null
            if (app.isTvApp) {
                launchIntent = pm.getLeanbackLaunchIntentForPackage(app.packageName)
            }
            if (launchIntent == null) {
                launchIntent = pm.getLaunchIntentForPackage(app.packageName)
            }
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "Không thể mở ứng dụng: ${app.label}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi mở app ${app.packageName}", e)
            Toast.makeText(context, "Lỗi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAppSettings(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi mở chi tiết ứng dụng $packageName", e)
        }
    }

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gỡ cài đặt ứng dụng $packageName", e)
        }
    }

    fun openSystemSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Không tìm thấy Cài đặt hệ thống", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWifiSettings() {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            openSystemSettings()
        }
    }
}


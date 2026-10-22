package com.meomeo.tvlauncher.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class GooglePhotosWallpaperManager(private val context: Context) {

    companion object {
        private const val TAG = "GooglePhotosWallpaper"
        const val DEFAULT_ALBUM_URL = "https://photos.app.goo.gl/snYDHT7Cc74EvYj4A"
        private const val PREFS_NAME = "google_photos_wallpaper_prefs"
        private const val KEY_ALBUM_URL = "album_url"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_PHOTO_COUNT = "photo_count"

        @Volatile
        private var instance: GooglePhotosWallpaperManager? = null

        fun getInstance(context: Context): GooglePhotosWallpaperManager {
            return instance ?: synchronized(this) {
                instance ?: GooglePhotosWallpaperManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val wallpapersDir = File(context.filesDir, "google_photos_wallpapers").apply { mkdirs() }

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun getAlbumUrl(): String {
        return prefs.getString(KEY_ALBUM_URL, DEFAULT_ALBUM_URL) ?: DEFAULT_ALBUM_URL
    }

    fun setAlbumUrl(url: String) {
        val cleanUrl = url.trim()
        prefs.edit().putString(KEY_ALBUM_URL, cleanUrl.ifEmpty { DEFAULT_ALBUM_URL }).apply()
    }

    fun getLastSyncTime(): Long {
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    fun getCachedPhotoCount(): Int {
        return getLocalPhotos().size
    }

    fun getLocalPhotos(): List<File> {
        val files = wallpapersDir.listFiles { file ->
            file.isFile && file.name.startsWith("gphoto_") && file.name.endsWith(".jpg") && file.length() > 1024
        } ?: emptyArray()

        return files.sortedBy { file ->
            file.name.removePrefix("gphoto_").removeSuffix(".jpg").toIntOrNull() ?: 999
        }
    }

    suspend fun syncAlbum(): Result<List<File>> = withContext(Dispatchers.IO) {
        val currentUrl = getAlbumUrl()
        Log.d(TAG, "Starting sync for album: $currentUrl")

        try {
            val request = Request.Builder()
                .url(currentUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Failed to load album page: HTTP ${response.code}")
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val html = response.body?.string() ?: ""
            val regex = Regex("https://lh3\\.googleusercontent\\.com/pw/[a-zA-Z0-9_\\-]+")
            val matches = regex.findAll(html)
                .map { it.value }
                .distinct()
                .toList()

            Log.d(TAG, "Discovered ${matches.size} photos in Google Photos album")

            if (matches.isEmpty()) {
                val fallback = getLocalPhotos()
                if (fallback.isNotEmpty()) {
                    return@withContext Result.success(fallback)
                }
                return@withContext Result.failure(Exception("No photos found in shared album"))
            }

            val downloadedFiles = mutableListOf<File>()

            matches.forEachIndexed { index, photoUrl ->
                val targetFile = File(wallpapersDir, "gphoto_${index}.jpg")
                // Use =d for direct original uncompressed photo, avoiding lossy downscaling
                val highResUrl = if (photoUrl.contains("=")) photoUrl else "$photoUrl=d"

                try {
                    val imgReq = Request.Builder()
                        .url(highResUrl)
                        .header(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        )
                        .build()

                    httpClient.newCall(imgReq).execute().use { imgRes ->
                        if (imgRes.isSuccessful) {
                            imgRes.body?.byteStream()?.use { input ->
                                val tempFile = File(wallpapersDir, "temp_${index}.tmp")
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                                if (tempFile.length() > 1024) {
                                    if (targetFile.exists()) targetFile.delete()
                                    tempFile.renameTo(targetFile)
                                    downloadedFiles.add(targetFile)
                                } else {
                                    tempFile.delete()
                                }
                            }
                        } else {
                            if (targetFile.exists()) downloadedFiles.add(targetFile)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error downloading photo $index: ${e.message}")
                    if (targetFile.exists()) downloadedFiles.add(targetFile)
                }
            }

            // Clean up obsolete old photos beyond current count
            wallpapersDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("gphoto_") && file.name.endsWith(".jpg")) {
                    val idx = file.name.removePrefix("gphoto_").removeSuffix(".jpg").toIntOrNull()
                    if (idx != null && idx >= matches.size) {
                        file.delete()
                    }
                }
            }

            prefs.edit()
                .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
                .putInt(KEY_PHOTO_COUNT, downloadedFiles.size)
                .apply()

            Log.d(TAG, "Album sync completed successfully. Total photos cached: ${downloadedFiles.size}")
            Result.success(downloadedFiles)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing Google Photos album", e)
            val fallback = getLocalPhotos()
            if (fallback.isNotEmpty()) {
                Result.success(fallback)
            } else {
                Result.failure(e)
            }
        }
    }
}


package com.meomeo.tvlauncher.update

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** A published build that is newer than the one installed. */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sizeBytes: Long,
    /** Lower-case hex SHA-256 of the APK as reported by GitHub, if it reported one. */
    val sha256: String?
)

/**
 * Looks for a newer build on GitHub Releases.
 *
 * CI publishes every build as release `v1.0.N` with `N` also being the app's versionCode, so "newer"
 * simply means a larger N than the installed versionCode. The repository must be public: the TV
 * calls the API without a token (a token baked into an APK could be read by anyone).
 */
class UpdateChecker(private val context: Context) {

    companion object {
        const val REPOSITORY = "quangkhuongduy0195/meomeo_launcher"
        private const val LATEST = "https://api.github.com/repos/$REPOSITORY/releases/latest"
        private const val DEBUG_PREFS = "update_debug"
        private const val KEY_API_OVERRIDE = "api_override"
        private val tagRegex = Regex("""v?(\d+)\.(\d+)\.(\d+)""")

        fun installedVersionCode(context: Context): Int = PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(context.packageName, 0)
        ).toInt()

        fun installedVersionName(context: Context): String =
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    /**
     * Debug builds can point the checker at a local server (`api_override` in the `update_debug`
     * prefs) to test the whole flow without publishing a release. Release builds ignore it.
     */
    private fun endpoint(): String =
        (if (isDebuggable) context.getSharedPreferences(DEBUG_PREFS, Context.MODE_PRIVATE).getString(KEY_API_OVERRIDE, null) else null)
            ?: LATEST

    /** Only GitHub (or, in debug builds, the override host) may serve the APK. */
    fun isTrustedDownload(url: String): Boolean {
        if (url.startsWith("https://github.com/$REPOSITORY/releases/download/")) return true
        val override = if (isDebuggable) context.getSharedPreferences(DEBUG_PREFS, Context.MODE_PRIVATE).getString(KEY_API_OVERRIDE, null) else null
        return override != null && url.substringAfter("://").substringBefore('/') == override.substringAfter("://").substringBefore('/')
    }

    /**
     * @return the newer release, or null when the installed build is current. Fails when GitHub
     * cannot be reached or has no usable release (404 also means the repository is private).
     */
    suspend fun checkForUpdate(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(endpoint())
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "MeoMeoLauncher/${installedVersionName(context)} (Android ${Build.VERSION.RELEASE})")
                .build()
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 404 -> return@withContext Result.failure(
                        Exception("Chưa có bản phát hành nào (hoặc kho mã nguồn chưa công khai).")
                    )
                    response.code == 403 || response.code == 429 -> return@withContext Result.failure(
                        Exception("GitHub tạm thời từ chối truy cập. Hãy thử lại sau ít phút.")
                    )
                    !response.isSuccessful -> return@withContext Result.failure(Exception("GitHub trả về lỗi HTTP ${response.code}."))
                }
                Result.success(parse(JSONObject(response.body?.string().orEmpty())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không kết nối được tới GitHub. Hãy kiểm tra mạng."))
        }
    }

    private fun parse(release: JSONObject): UpdateInfo? {
        val tag = release.optString("tag_name")
        val match = tagRegex.matchEntire(tag) ?: return null
        val remoteCode = match.groupValues[3].toIntOrNull() ?: return null
        if (remoteCode <= installedVersionCode(context)) return null

        val assets = release.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (!asset.optString("name").endsWith(".apk", ignoreCase = true)) continue
            val digest = asset.optString("digest", "").takeIf { it.startsWith("sha256:") }?.removePrefix("sha256:")?.lowercase()
            return UpdateInfo(
                versionCode = remoteCode,
                versionName = tag.removePrefix("v"),
                apkUrl = asset.getString("browser_download_url"),
                sizeBytes = asset.optLong("size", -1L),
                sha256 = digest
            )
        }
        return null
    }
}

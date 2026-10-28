package com.meomeo.tvlauncher.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Result of the system installer, reported by [InstallResultReceiver]. */
sealed interface InstallOutcome {
    data object Success : InstallOutcome
    data class Failed(val message: String) : InstallOutcome
}

/** Bridges the system installer's broadcast back to the ViewModel. */
object InstallEvents {
    private val _outcome = MutableStateFlow<InstallOutcome?>(null)
    val outcome: StateFlow<InstallOutcome?> = _outcome.asStateFlow()

    fun report(outcome: InstallOutcome) {
        _outcome.value = outcome
    }

    fun consume() {
        _outcome.value = null
    }
}

/** Downloads an APK and hands it to Android's package installer. */
class ApkInstaller(private val context: Context, private val checker: UpdateChecker) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val updateDir = File(context.cacheDir, "updates")

    /** An app may only install packages once the user has allowed it ("Install unknown apps"). */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /**
     * Opens the system page where the user allows this app to install updates. Many TVs ship
     * without that page; returns false then, and the user has to allow it over ADB instead.
     */
    fun openInstallPermissionSettings(): Boolean = try {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: Exception) {
        false
    }

    /**
     * Downloads [info] into the app's cache, reporting progress in 0..1. The SHA-256 GitHub publishes
     * is checked when there is one (Android itself also refuses an APK signed with a different key).
     */
    suspend fun download(info: UpdateInfo, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!checker.isTrustedDownload(info.apkUrl)) {
                return@withContext Result.failure(Exception("Địa chỉ tải không hợp lệ."))
            }
            updateDir.mkdirs()
            updateDir.listFiles()?.forEach { it.delete() }
            val target = File(updateDir, "update.apk")
            val part = File(updateDir, "update.apk.part")

            val request = Request.Builder().url(info.apkUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Tải thất bại (HTTP ${response.code})."))
                val body = response.body ?: return@withContext Result.failure(Exception("Phản hồi rỗng."))
                val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
                val digest = MessageDigest.getInstance("SHA-256")
                var received = 0L
                var lastReported = -1
                body.byteStream().use { input ->
                    part.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            digest.update(buffer, 0, n)
                            received += n
                            if (total > 0) {
                                val percent = (received * 100 / total).toInt()
                                if (percent != lastReported) {
                                    lastReported = percent
                                    onProgress(received.toFloat() / total)
                                }
                            }
                        }
                    }
                }
                if (info.sizeBytes > 0 && received != info.sizeBytes) {
                    part.delete()
                    return@withContext Result.failure(Exception("Tệp tải về bị thiếu. Hãy thử lại."))
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (info.sha256 != null && !actual.equals(info.sha256, ignoreCase = true)) {
                    part.delete()
                    return@withContext Result.failure(Exception("Tệp tải về không khớp mã kiểm tra (SHA-256)."))
                }
            }
            part.renameTo(target)
            Result.success(target)
        } catch (e: Exception) {
            Log.w(TAG, "Download failed", e)
            Result.failure(Exception("Tải thất bại. Hãy kiểm tra mạng và thử lại."))
        }
    }

    /**
     * Streams [apk] into a PackageInstaller session and commits it. Android then shows its own
     * confirmation (see [InstallResultReceiver]); on success the system restarts the app.
     */
    suspend fun install(apk: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val installer = context.packageManager.packageInstaller
            // A confirmation the user never answered (or that crashed) leaves a session behind.
            installer.mySessions.forEach { runCatching { installer.abandonSession(it.sessionId) } }
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setSize(apk.length())
                // Best effort: lets Android 12+ skip the confirmation for later updates when it allows.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }
            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite("update.apk", 0, apk.length()).use { out ->
                        input.copyTo(out)
                        session.fsync(out)
                    }
                }
                val intent = Intent(context, InstallResultReceiver::class.java).setAction(InstallResultReceiver.ACTION)
                // Mutable: the installer adds the status extras to this intent.
                val pending = PendingIntent.getBroadcast(context, sessionId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                session.commit(pending.intentSender)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Install failed", e)
            Result.failure(Exception("Không thể bắt đầu cài đặt: ${e.message}"))
        }
    }

    private companion object {
        const val TAG = "ApkInstaller"
    }
}

/**
 * Receives the installer's status. The first status is usually "needs user action" and carries the
 * system confirmation screen, which has to be launched from here.
 */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirm != null) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirm)
                    } catch (e: Exception) {
                        InstallEvents.report(InstallOutcome.Failed("Không mở được màn hình xác nhận cài đặt."))
                    }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> InstallEvents.report(InstallOutcome.Success)
            PackageInstaller.STATUS_FAILURE_ABORTED -> InstallEvents.report(InstallOutcome.Failed("Đã hủy cài đặt."))
            PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> InstallEvents.report(
                InstallOutcome.Failed(
                    "Không cài đè được: bản này được ký bằng khóa khác với bản đang cài. " +
                        "Hãy gỡ launcher rồi cài lại một lần bằng bản từ GitHub Releases."
                )
            )
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
                InstallEvents.report(InstallOutcome.Failed("Cài đặt thất bại ($status). $message".trim()))
            }
        }
    }

    companion object {
        const val ACTION = "com.meomeo.tvlauncher.INSTALL_RESULT"
    }
}

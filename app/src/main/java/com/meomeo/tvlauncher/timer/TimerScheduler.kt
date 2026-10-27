package com.meomeo.tvlauncher.timer

import android.app.ActivityOptions
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.meomeo.tvlauncher.MainActivity

/**
 * Brings the launcher (and its lock screen) back to the front when viewing time runs out, even if
 * another app is on screen.
 *
 * A background service/receiver is not allowed to start activities on modern Android, so the alarm
 * carries an Activity PendingIntent instead. The system fires it, and on Android 14+ the creator
 * explicitly opts in to background activity starts.
 */
object TimerScheduler {

    /** While locked, re-assert the lock this often in case the user opened another app. */
    const val RELOCK_INTERVAL_MS = 10_000L

    private const val REQUEST_CODE = 4101

    fun schedule(context: Context, triggerAtMillis: Long) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val operation = lockIntent(app)
        try {
            val showIntent = PendingIntent.getActivity(
                app, REQUEST_CODE + 1, Intent(app, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), operation)
        } catch (_: SecurityException) {
            // Exact alarms not permitted on this device: fall back to a best-effort alarm.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        (app.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(lockIntent(app))
    }

    private fun lockIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        val options = ActivityOptions.makeBasic().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setPendingIntentCreatorBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            }
        }
        return PendingIntent.getActivity(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            options.toBundle()
        )
    }
}

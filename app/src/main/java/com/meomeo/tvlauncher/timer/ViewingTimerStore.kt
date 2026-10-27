package com.meomeo.tvlauncher.timer

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Persistent state for the viewing timer and its parental passcode.
 *
 * The timer is stored as an absolute wall-clock end time, so it survives process death and reboots:
 * once `now >= endAt` the launcher is locked until the passcode is entered, even after a restart.
 */
class ViewingTimerStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("viewing_timer_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PIN_LENGTH = 4
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 60
        const val DEFAULT_MINUTES = 30

        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_MINUTES = "minutes"
        private const val KEY_END_AT = "end_at"
        private const val PBKDF2_ITERATIONS = 20_000
    }

    fun hasPasscode(): Boolean = prefs.getString(KEY_PIN_HASH, null) != null

    fun setPasscode(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash(pin, salt), Base64.NO_WRAP))
            .apply()
    }

    fun verifyPasscode(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val expected = prefs.getString(KEY_PIN_HASH, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        return MessageDigest.isEqual(expected, hash(pin, salt))
    }

    var minutes: Int
        get() = prefs.getInt(KEY_MINUTES, DEFAULT_MINUTES).coerceIn(MIN_MINUTES, MAX_MINUTES)
        set(value) = prefs.edit().putInt(KEY_MINUTES, value.coerceIn(MIN_MINUTES, MAX_MINUTES)).apply()

    /** Wall-clock millis when the viewing time runs out, or 0 when no timer is set. */
    var endAt: Long
        get() = prefs.getLong(KEY_END_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_END_AT, value).apply()

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = endAt != 0L && now >= endAt

    private fun hash(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, 256))
            .encoded
}

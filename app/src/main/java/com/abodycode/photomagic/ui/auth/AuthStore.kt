package com.abodycode.photomagic.ui.auth

import android.content.Context
import android.content.SharedPreferences

/// Local persistence for the auth gate: persisted session, brute-force
/// lockout counter, chosen locale and theme. One SharedPreferences file so
/// every piece of the gate state is trivially inspectable.
internal object AuthStore {

    private const val PREFS: String = "photomagic.auth.v1"

    const val DEFAULT_LOCALE: String = "ar"      // AR is the default (matches p2p dish)
    const val DEFAULT_THEME: String = "light"    // Light is the default (matches p2p dish)

    private const val KEY_SESSION: String = "session_username"
    private const val KEY_LANG: String = "lang"
    private const val KEY_THEME: String = "theme"

    // Online brute-force throttling (exponential backoff like p2p dish).
    private const val KEY_ATTEMPTS: String = "lock_attempts"
    private const val KEY_LOCK_UNTIL: String = "lock_until"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ------------------------------------------------------------ session
    fun restoreSession(context: Context): String? =
        prefs(context).getString(KEY_SESSION, null)?.takeIf { it.isNotBlank() }

    fun saveSession(context: Context, username: String) {
        prefs(context).edit().putString(KEY_SESSION, username.trim().lowercase()).apply()
    }

    fun clearSession(context: Context) {
        prefs(context).edit().remove(KEY_SESSION).apply()
    }

    // ------------------------------------------------------ lockout state
    fun failedAttempts(context: Context): Int = prefs(context).getInt(KEY_ATTEMPTS, 0)

    fun lockUntilMs(context: Context): Long = prefs(context).getLong(KEY_LOCK_UNTIL, 0L)

    fun registerFailure(context: Context): Int {
        val attempts = failedAttempts(context) + 1
        val editor = prefs(context).edit()
        editor.putInt(KEY_ATTEMPTS, attempts)
        if (attempts >= authMaxAttempts) {
            // 30 s, then 60 s, 120 s... reset only by a successful login.
            val seconds = 30 shl (attempts - authMaxAttempts)
            editor.putLong(KEY_LOCK_UNTIL, System.currentTimeMillis() + seconds * 1000L)
        }
        editor.apply()
        return attempts
    }

    fun resetLockout(context: Context) {
        prefs(context).edit().putInt(KEY_ATTEMPTS, 0).putLong(KEY_LOCK_UNTIL, 0L).apply()
    }

    // -------------------------------------------------------- preferences
    fun loadLocale(context: Context): String =
        prefs(context).getString(KEY_LANG, null)?.takeIf { it == "en" || it == "ar" }
            ?: DEFAULT_LOCALE

    fun saveLocale(context: Context, locale: String) {
        val value = if (locale == "en") "en" else "ar"
        prefs(context).edit().putString(KEY_LANG, value).apply()
    }

    fun loadTheme(context: Context): String =
        prefs(context).getString(KEY_THEME, null)?.takeIf { it == "dark" || it == "light" }
            ?: DEFAULT_THEME

    fun saveTheme(context: Context, theme: String) {
        val value = if (theme == "dark") "dark" else "light"
        prefs(context).edit().putString(KEY_THEME, value).apply()
    }

    companion object {
        /// Same maximum as p2p dish: 5 failed attempts before backoff.
        const val authMaxAttempts: Int = 5
    }
}

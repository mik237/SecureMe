package me.secure.vault.secureme.core.utils

import android.util.Log
import me.secure.vault.secureme.BuildConfig

/**
 * SecureLogger provides a way to log messages that are automatically
 * stripped or disabled in release builds to prevent sensitive data leakage.
 */
object SecureLogger {
    private const val TAG = "SecureMe_Log"

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, message, throwable)
        }
    }

    fun i(message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message)
        }
    }

    /**
     * Use this for logging that might contain potentially sensitive but non-secret
     * information during development. This is strictly a no-op in release.
     */
    fun sensitive(message: String) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "[SENSITIVE] $message")
        }
    }
}

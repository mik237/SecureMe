package me.secure.vault.secureme.core.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardClearManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var clearJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun copyToClipboard(label: String, text: String) {
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        
        // Schedule clearing after 60 seconds
        scheduleClear()
    }

    private fun scheduleClear() {
        clearJob?.cancel()
        clearJob = scope.launch {
            delay(60_000) // 60 seconds
            clearClipboard()
        }
    }

    fun clearClipboard() {
        try {
            if (clipboard.hasPrimaryClip()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    clipboard.clearPrimaryClip()
                } else {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
                SecureLogger.d("Clipboard cleared for security.")
            }
        } catch (e: Exception) {
            SecureLogger.e("Failed to clear clipboard", e)
        }
    }
}

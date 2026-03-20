package me.secure.vault.secureme.core.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TempFileCleaner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Deletes all files in the temp_view directory for a specific user.
     * Path: getCacheDir()/temp_view/{userId}/
     */
    fun clearTempFiles(userId: String) {
        val tempDir = File(context.cacheDir, "temp_view/$userId")
        if (tempDir.exists() && tempDir.isDirectory) {
            tempDir.listFiles()?.forEach { file ->
                try {
                    if (file.delete()) {
                        SecureLogger.d("Deleted temp file: ${file.name}")
                    }
                } catch (e: Exception) {
                    SecureLogger.e("Failed to delete temp file: ${file.name}", e)
                }
            }
        }
    }

    /**
     * Deletes a specific temp file.
     */
    fun deleteFile(filePath: String) {
        val file = File(filePath)
        if (file.exists() && file.absolutePath.contains("temp_view")) {
            try {
                if (file.delete()) {
                    SecureLogger.d("Deleted specific temp file: ${file.name}")
                }
            } catch (e: Exception) {
                SecureLogger.e("Failed to delete temp file: ${file.name}", e)
            }
        }
    }
}

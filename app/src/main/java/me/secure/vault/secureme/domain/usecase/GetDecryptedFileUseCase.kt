package me.secure.vault.secureme.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.secure.vault.secureme.core.security.SessionManager
import me.secure.vault.secureme.crypto.AesGcmCipher
import me.secure.vault.secureme.domain.repository.VaultRepository
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

class GetDecryptedFileUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultRepository: VaultRepository,
    private val aesGcmCipher: AesGcmCipher,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(fileId: String): Result<File> = withContext(Dispatchers.IO) {
        var masterKey: ByteArray? = null
        var fileKey: ByteArray? = null
        var tempFile: File? = null

        try {
            val masterKeyRef = sessionManager.getMasterKey()
                ?: throw IllegalStateException("Vault is locked")
            masterKey = masterKeyRef

            // 1. Load metadata and find entry
            val metadata = vaultRepository.loadMetadata().getOrThrow()
            val entry = metadata.entries.find { it.id == fileId }
                ?: throw Exception("File not found in vault")

            // 2. Decrypt the file key
            val decryptedFileKey = aesGcmCipher.decrypt(entry.encryptedFileKey, masterKeyRef)
            fileKey = decryptedFileKey

            // 3. Prepare temp file in UID-scoped cache
            // Rule 7: Every user-specific local path is scoped by the authenticated Firebase UID
            val tempDir = File(context.cacheDir, "temp_view/${entry.ownerId}")
            if (!tempDir.exists()) tempDir.mkdirs()
            
            val localTempFile = File(tempDir, "${entry.id}.tmp")
            tempFile = localTempFile
            
            // 4. Decrypt stream to temp file
            val encryptedFile = File(entry.storagePath)
            if (!encryptedFile.exists()) throw Exception("Encrypted file not found on disk")

            FileInputStream(encryptedFile).use { input ->
                FileOutputStream(localTempFile).use { output ->
                    aesGcmCipher.decryptStream(input, output, decryptedFileKey)
                }
            }

            Result.success(localTempFile)
        } catch (e: Exception) {
            // Cleanup partial file on failure
            tempFile?.let { if (it.exists()) it.delete() }
            Result.failure(e)
        } finally {
            // Rule 14.1: Wipe sensitive keys from memory
            masterKey?.fill(0)
            fileKey?.fill(0)
        }
    }
}

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
        runCatching {
            val masterKey = sessionManager.getMasterKey()
                ?: throw IllegalStateException("Vault is locked")

            // 1. Load metadata and find entry
            val metadata = vaultRepository.loadMetadata().getOrThrow()
            val entry = metadata.entries.find { it.id == fileId }
                ?: throw Exception("File not found in vault")

            // 2. Decrypt the file key
            val fileKey = aesGcmCipher.decrypt(entry.encryptedFileKey, masterKey)

            // 3. Prepare temp file in cache
            val tempDir = File(context.cacheDir, "temp_view")
            if (!tempDir.exists()) tempDir.mkdirs()
            
            val tempFile = File(tempDir, "${entry.id}.tmp")
            
            // 4. Decrypt stream to temp file
            val encryptedFile = File(entry.storagePath)
            FileInputStream(encryptedFile).use { input ->
                FileOutputStream(tempFile).use { output ->
                    aesGcmCipher.decryptStream(input, output, fileKey)
                }
            }

            // Wipe file key from memory
            fileKey.fill(0)
            masterKey.fill(0)

            tempFile
        }
    }
}

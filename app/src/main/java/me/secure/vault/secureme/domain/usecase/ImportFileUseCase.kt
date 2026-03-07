package me.secure.vault.secureme.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.secure.vault.secureme.core.security.SessionManager
import me.secure.vault.secureme.crypto.AesGcmCipher
import me.secure.vault.secureme.domain.model.VaultFileEntry
import me.secure.vault.secureme.domain.model.VaultMetadata
import me.secure.vault.secureme.domain.repository.AuthRepository
import me.secure.vault.secureme.domain.repository.VaultRepository
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject

class ImportFileUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultRepository: VaultRepository,
    private val authRepository: AuthRepository,
    private val aesGcmCipher: AesGcmCipher,
    private val sessionManager: SessionManager
) {
    private val secureRandom = SecureRandom()

    suspend operator fun invoke(uri: Uri): Result<VaultFileEntry> = withContext(Dispatchers.IO) {
        runCatching {
            val metadata = getUriMetadata(uri)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Failed to open input stream from URI")
            
            importInternal(inputStream, metadata.name, metadata.size, metadata.mimeType).getOrThrow()
        }
    }

    suspend operator fun invoke(file: File): Result<VaultFileEntry> = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream = FileInputStream(file)
            val mimeType = context.contentResolver.getType(Uri.fromFile(file)) ?: "application/octet-stream"
            importInternal(inputStream, file.name, file.length(), mimeType).getOrThrow()
        }
    }

    private suspend fun importInternal(
        inputStream: InputStream,
        fileName: String,
        fileSize: Long,
        mimeType: String
    ): Result<VaultFileEntry> = withContext(Dispatchers.IO) {
        val masterKey = sessionManager.getMasterKey()
            ?: return@withContext Result.failure(IllegalStateException("Vault is locked"))

        val fileKey = ByteArray(32)
        secureRandom.nextBytes(fileKey)

        val fileId = UUID.randomUUID().toString()
        val targetPath = vaultRepository.getNewVaultFilePath(fileId)
        val targetFile = File(targetPath)

        try {
            inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    aesGcmCipher.encryptStream(input, output, fileKey)
                }
            }

            val encryptedFileKey = aesGcmCipher.encrypt(fileKey, masterKey)
            val ownerId = authRepository.getCurrentUserId() ?: throw Exception("User not logged in")

            val entry = VaultFileEntry(
                id = fileId,
                fileName = fileName,
                mimeType = mimeType,
                fileSize = fileSize,
                createdAt = System.currentTimeMillis(),
                storagePath = targetPath,
                encryptedFileKey = encryptedFileKey,
                ownerId = ownerId
            )

            // Update metadata
            // Load current metadata, or create new if it doesn't exist
            val currentMetadata = vaultRepository.loadMetadata().getOrElse {
                VaultMetadata(ownerId = ownerId, entries = emptyList())
            }
            val updatedMetadata = currentMetadata.copy(
                entries = currentMetadata.entries + entry
            )
            vaultRepository.saveMetadata(updatedMetadata).getOrThrow()

            Result.success(entry)
        } catch (e: Exception) {
            // Delete partially written file if it exists
            if (targetFile.exists()) {
                targetFile.delete()
            }
            Result.failure(e)
        } finally {
            // Rule 14.1 & 5.3.3: Wipe sensitive keys from memory immediately after use
            masterKey.fill(0)
            fileKey.fill(0)
        }
    }

    private fun getUriMetadata(uri: Uri): UriMetadata {
        var name = "unknown"
        var size = 0L
        var mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) {
                    val stringValue = cursor.getString(nameIndex)
                    if (stringValue != null) name = stringValue
                }
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
        return UriMetadata(name, size, mimeType)
    }

    private data class UriMetadata(val name: String, val size: Long, val mimeType: String)
}

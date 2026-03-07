package me.secure.vault.secureme.data.repository

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.secure.vault.secureme.core.security.SessionManager
import me.secure.vault.secureme.crypto.AesGcmCipher
import me.secure.vault.secureme.domain.model.EncryptedData
import me.secure.vault.secureme.domain.model.VaultMetadata
import me.secure.vault.secureme.domain.repository.AuthRepository
import me.secure.vault.secureme.domain.repository.VaultRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalVaultRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val aesGcmCipher: AesGcmCipher,
    private val authRepository: AuthRepository
) : VaultRepository {

    /**
     * GUIDELINE COMPLIANCE (Corrected for Persistence & Scoped Storage):
     * To ensure the vault persists even if the app is uninstalled, we use the 
     * 'Documents' directory in public external storage.
     * 
     * Path: /sdcard/Documents/SecureMe_Vault/
     */
    private val vaultDir: File by lazy {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        File(docsDir, "SecureMe_Vault")
    }

    private val metadataFile: File by lazy {
        File(vaultDir, "vault_metadata.enc")
    }

    override suspend fun initializeVault(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!vaultDir.exists()) {
                val created = vaultDir.mkdirs()
                if (!created && !vaultDir.exists()) {
                    throw Exception("Failed to create vault directory at ${vaultDir.absolutePath}")
                }
            }
            val noMediaFile = File(vaultDir, ".nomedia")
            if (!noMediaFile.exists()) {
                noMediaFile.createNewFile()
            }
        }
    }

    override suspend fun loadMetadata(): Result<VaultMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val ownerId = authRepository.getCurrentUserId() ?: "unknown"
            
            if (!metadataFile.exists()) {
                return@runCatching VaultMetadata(ownerId = ownerId, entries = emptyList())
            }

            val masterKey = sessionManager.getMasterKey()
                ?: throw IllegalStateException("Vault is locked")

            try {
                val encryptedBytes = metadataFile.readBytes()
                if (encryptedBytes.size < 12) {
                    return@runCatching VaultMetadata(ownerId = ownerId, entries = emptyList())
                }
                
                val iv = encryptedBytes.take(12).toByteArray()
                val ciphertext = encryptedBytes.drop(12).toByteArray()
                
                val decryptedBytes = aesGcmCipher.decrypt(EncryptedData(ciphertext, iv), masterKey)
                val jsonString = String(decryptedBytes)
                Json.decodeFromString<VaultMetadata>(jsonString)
            } finally {
                masterKey.fill(0)
            }
        }
    }

    override suspend fun saveMetadata(metadata: VaultMetadata): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val masterKey = sessionManager.getMasterKey()
                ?: throw IllegalStateException("Vault is locked")

            try {
                val jsonString = Json.encodeToString(metadata)
                val encryptedData = aesGcmCipher.encrypt(jsonString.toByteArray(), masterKey)
                
                val combined = encryptedData.iv + encryptedData.ciphertext
                metadataFile.writeBytes(combined)
            } finally {
                masterKey.fill(0)
            }
        }
    }

    override suspend fun deleteFile(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val metadataResult = loadMetadata()
            val metadata = metadataResult.getOrThrow()
            
            val entryToDelete = metadata.entries.find { it.id == fileId }
                ?: throw Exception("File not found in metadata")

            // 1. Delete the physical file
            val file = File(entryToDelete.storagePath)
            if (file.exists()) {
                if (!file.delete()) {
                    throw Exception("Failed to delete encrypted file from storage")
                }
            }

            // 2. Remove from metadata
            val updatedEntries = metadata.entries.filter { it.id != fileId }
            val updatedMetadata = metadata.copy(entries = updatedEntries)

            // 3. Save metadata
            saveMetadata(updatedMetadata).getOrThrow()
        }
    }

    override fun getNewVaultFilePath(fileId: String): String {
        return File(vaultDir, "$fileId.enc").absolutePath
    }
}

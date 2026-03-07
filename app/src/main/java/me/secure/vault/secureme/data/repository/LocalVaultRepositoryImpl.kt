package me.secure.vault.secureme.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.secure.vault.secureme.core.security.SessionManager
import me.secure.vault.secureme.crypto.AesGcmCipher
import me.secure.vault.secureme.domain.model.EncryptedData
import me.secure.vault.secureme.domain.model.VaultMetadata
import me.secure.vault.secureme.domain.repository.VaultRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalVaultRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val aesGcmCipher: AesGcmCipher
) : VaultRepository {

    private val vaultDir: File by lazy {
        File(context.getExternalFilesDir(null), "SecureMe_Vault")
    }

    private val metadataFile: File by lazy {
        File(vaultDir, "vault_metadata.enc")
    }

    override suspend fun initializeVault(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!vaultDir.exists()) {
                vaultDir.mkdirs()
            }
            val noMediaFile = File(vaultDir, ".nomedia")
            if (!noMediaFile.exists()) {
                noMediaFile.createNewFile()
            }
        }
    }

    override suspend fun loadMetadata(): Result<VaultMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            if (!metadataFile.exists()) {
                throw IllegalStateException("Metadata file not found")
            }

            val masterKey = sessionManager.getMasterKey()
                ?: throw IllegalStateException("Vault is locked")

            try {
                val encryptedBytes = metadataFile.readBytes()
                // The first 12 bytes are IV (based on guidelines 5.4, but AesGcmCipher handles it differently via EncryptedData)
                // Actually, AesGcmCipher.encrypt returns EncryptedData which has ciphertext and iv separately.
                // We need a way to store them both in the file.
                // Guidelines 5.4 says: [12 bytes: IV][N bytes: AES-256-GCM encrypted content]
                
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
                
                // Save as [IV][Ciphertext] as per guidelines 5.4
                val combined = encryptedData.iv + encryptedData.ciphertext
                metadataFile.writeBytes(combined)
            } finally {
                masterKey.fill(0)
            }
        }
    }

    override fun getNewVaultFilePath(fileId: String): String {
        return File(vaultDir, "$fileId.enc").absolutePath
    }
}

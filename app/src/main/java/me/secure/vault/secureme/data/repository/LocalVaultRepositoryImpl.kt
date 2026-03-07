package me.secure.vault.secureme.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.secure.vault.secureme.core.security.SessionManager
import me.secure.vault.secureme.crypto.AesGcmCipher
import me.secure.vault.secureme.crypto.SharingCryptoManager
import me.secure.vault.secureme.domain.model.EncryptedData
import me.secure.vault.secureme.domain.model.ShareRecord
import me.secure.vault.secureme.domain.model.ShareStatus
import me.secure.vault.secureme.domain.model.VaultMetadata
import me.secure.vault.secureme.domain.repository.AuthRepository
import me.secure.vault.secureme.domain.repository.VaultRepository
import java.io.File
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalVaultRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val aesGcmCipher: AesGcmCipher,
    private val sharingCryptoManager: SharingCryptoManager,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : VaultRepository {

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

            val file = File(entryToDelete.storagePath)
            if (file.exists()) {
                if (!file.delete()) {
                    throw Exception("Failed to delete encrypted file from storage")
                }
            }

            val updatedEntries = metadata.entries.filter { it.id != fileId }
            val updatedMetadata = metadata.copy(entries = updatedEntries)

            saveMetadata(updatedMetadata).getOrThrow()
        }
    }

    override suspend fun shareFile(fileId: String, recipientEmail: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val senderId = authRepository.getCurrentUserId() ?: throw Exception("Sender not logged in")
            val cleanEmail = recipientEmail.trim().lowercase()
            
            // 1. Find recipient by email (with limit to match security rules)
            val recipientQuery = firestore.collection("users")
                .whereEqualTo("email", cleanEmail)
                .limit(1)
                .get()
                .await()
            
            if (recipientQuery.isEmpty) throw Exception("User with email $cleanEmail not found")
            val recipientDoc = recipientQuery.documents.first()
            val recipientId = recipientDoc.id
            
            // 2. Retrieve recipient's public keys
            val publicKeysMap = recipientDoc.get("publicKeys") as? Map<*, *> ?: throw Exception("Recipient public keys not found")
            val x25519PubBase64 = publicKeysMap["x25519PublicKey"] as? String ?: throw Exception("X25519 Public Key missing")
            val recipientX25519Pub = Base64.getDecoder().decode(x25519PubBase64)
            
            // 3. Retrieve sender's keys from session
            val senderX25519Priv = sessionManager.getX25519PrivateKey() ?: throw Exception("Vault is locked (X25519 missing)")
            val senderEd25519Priv = sessionManager.getEd25519PrivateKey() ?: throw Exception("Vault is locked (Ed25519 missing)")
            val masterKey = sessionManager.getMasterKey() ?: throw Exception("Vault is locked (MasterKey missing)")
            
            try {
                // 4. Load metadata to get file key
                val metadata = loadMetadata().getOrThrow()
                val fileEntry = metadata.entries.find { it.id == fileId } ?: throw Exception("File not found in vault")
                
                // Decrypt file key with master key
                val fileKey = aesGcmCipher.decrypt(fileEntry.encryptedFileKey, masterKey)
                
                // 5. Perform X25519 Key Exchange
                val sharedSecret = sharingCryptoManager.calculateSharedSecret(senderX25519Priv, recipientX25519Pub)
                
                // 6. Use HKDF-SHA256 to derive AES-GCM key
                val derivedKey = sharingCryptoManager.deriveSharedKey(sharedSecret, null, "FileSharing".toByteArray())
                
                // 7. Encrypt the File's Unique Encryption Key for the recipient
                val encryptedFileKeyForRecipient = aesGcmCipher.encrypt(fileKey, derivedKey)
                
                // 8. Create Digital Signature over share metadata
                val shareId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                val signatureData = (senderId + recipientId + fileId + timestamp.toString()).toByteArray()
                val signature = sharingCryptoManager.signData(signatureData, senderEd25519Priv)
                
                // 9. Upload the already encrypted file to Firebase Storage
                val encryptedFile = File(fileEntry.storagePath)
                if (!encryptedFile.exists()) throw Exception("Encrypted file not found on disk")
                
                val storageRef = storage.reference.child("shares/$shareId/$fileId.enc")
                storageRef.putFile(Uri.fromFile(encryptedFile)).await()
                
                // 10. Create ShareRecord in Firestore
                val shareRecord = ShareRecord(
                    shareId = shareId,
                    senderId = senderId,
                    recipientId = recipientId,
                    fileId = fileId,
                    fileName = fileEntry.fileName,
                    fileSize = fileEntry.fileSize,
                    mimeType = fileEntry.mimeType,
                    encryptedFileKey = encryptedFileKeyForRecipient,
                    senderEphemeralPublicKey = sharingCryptoManager.getX25519PublicKey(senderX25519Priv),
                    senderSignature = signature,
                    timestamp = timestamp,
                    status = ShareStatus.PENDING
                )
                
                firestore.collection("shares").document(shareId).set(shareRecordToMap(shareRecord)).await()
                Unit
            } finally {
                // Security Rule 5.3: Wipe sensitive keys from memory
                senderX25519Priv.fill(0)
                senderEd25519Priv.fill(0)
                masterKey.fill(0)
            }
        }
    }

    private fun shareRecordToMap(record: ShareRecord): Map<String, Any> {
        return mapOf(
            "shareId" to record.shareId,
            "senderId" to record.senderId,
            "recipientId" to record.recipientId,
            "fileId" to record.fileId,
            "fileName" to record.fileName,
            "fileSize" to record.fileSize,
            "mimeType" to record.mimeType,
            "encryptedFileKey" to mapOf(
                "ciphertext" to Base64.getEncoder().encodeToString(record.encryptedFileKey.ciphertext),
                "iv" to Base64.getEncoder().encodeToString(record.encryptedFileKey.iv)
            ),
            "senderEphemeralPublicKey" to Base64.getEncoder().encodeToString(record.senderEphemeralPublicKey),
            "senderSignature" to Base64.getEncoder().encodeToString(record.senderSignature),
            "timestamp" to record.timestamp,
            "status" to record.status.name
        )
    }

    override fun getNewVaultFilePath(fileId: String): String {
        return File(vaultDir, "$fileId.enc").absolutePath
    }
}

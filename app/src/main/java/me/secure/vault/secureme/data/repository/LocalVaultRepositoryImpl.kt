package me.secure.vault.secureme.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.storageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.secure.vault.secureme.core.security.SessionManager
import me.secure.vault.secureme.crypto.AesGcmCipher
import me.secure.vault.secureme.crypto.SharingCryptoManager
import me.secure.vault.secureme.domain.model.EncryptedData
import me.secure.vault.secureme.domain.model.ShareRecord
import me.secure.vault.secureme.domain.model.ShareStatus
import me.secure.vault.secureme.domain.model.UserKeyBundle
import me.secure.vault.secureme.domain.model.VaultFileEntry
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

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var cleanupJob: Job? = null
    private val processingShares = mutableSetOf<String>()

    /**
     * Storage Strategy (Updated per Senior Dev requirements):
     * - Vault files (Encrypted): Public Shared Storage (Documents/SecureMe_Vault/{userId}/)
     *   Ensures data survives app uninstall.
     * - Metadata (Encrypted Index): Internal App Storage (filesDir/{userId}/vault_metadata.enc)
     *   Ensures isolation and security of the file index/keys.
     */

    private fun getVaultDir(userId: String): File {
        @Suppress("DEPRECATION")
        val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        return File(publicDocs, "SecureMe_Vault/$userId")
    }

    private fun getVaultDir(): File {
        val userId = authRepository.getCurrentUserIdSync() ?: "default_user"
        return getVaultDir(userId)
    }

    private fun getMetadataFile(userId: String): File {
        val userInternalDir = File(context.filesDir, userId)
        if (!userInternalDir.exists()) userInternalDir.mkdirs()
        return File(userInternalDir, "vault_metadata.enc")
    }

    private fun getMetadataFile(): File {
        val userId = authRepository.getCurrentUserIdSync() ?: "default_user"
        return getMetadataFile(userId)
    }

    override suspend fun initializeVault(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val vaultDir = getVaultDir()
            if (!vaultDir.exists()) {
                val created = vaultDir.mkdirs()
                if (!created && !vaultDir.exists()) {
                    throw Exception("Failed to create vault directory at ${vaultDir.absolutePath}. Please check storage permissions.")
                }
            }
            
            val noMediaFile = File(vaultDir, ".nomedia")
            if (!noMediaFile.exists()) {
                noMediaFile.createNewFile()
            }
            
            // Ensure internal metadata directory exists
            getMetadataFile().parentFile?.mkdirs()
            Unit
        }
    }

    override suspend fun isLocalMetadataAvailable(): Boolean {
        val metadataFile = getMetadataFile()
        return metadataFile.exists() && metadataFile.length() > 0
    }

    override suspend fun loadMetadata(): Result<VaultMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val ownerId = authRepository.getCurrentUserId() ?: "unknown"
            val metadataFile = getMetadataFile()
            
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
                // 1. Save Locally to Internal Scoped Storage
                val jsonString = Json.encodeToString(metadata)
                val encryptedData = aesGcmCipher.encrypt(jsonString.toByteArray(), masterKey)
                
                val combined = encryptedData.iv + encryptedData.ciphertext
                getMetadataFile().writeBytes(combined)

                // 2. Backup to Cloud
                backupMetadataInternal(metadata, masterKey).getOrThrow()
            } finally {
                masterKey.fill(0)
            }
        }
    }

    override suspend fun backupMetadata(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val masterKey = sessionManager.getMasterKey()
                ?: throw IllegalStateException("Vault is locked")
            try {
                val metadata = loadMetadata().getOrThrow()
                backupMetadataInternal(metadata, masterKey).getOrThrow()
            } finally {
                masterKey.fill(0)
            }
        }
    }

    private suspend fun backupMetadataInternal(metadata: VaultMetadata, masterKey: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = authRepository.getCurrentUserId() ?: throw Exception("Not logged in")
            val jsonString = Json.encodeToString(metadata)
            val encryptedData = aesGcmCipher.encrypt(jsonString.toByteArray(), masterKey)
            
            val backupMap = mapOf(
                "ciphertext" to Base64.getEncoder().encodeToString(encryptedData.ciphertext),
                "iv" to Base64.getEncoder().encodeToString(encryptedData.iv),
                "timestamp" to System.currentTimeMillis()
            )
            
            firestore.collection("users").document(userId)
                .collection("metadataBackup").document("latest")
                .set(backupMap)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreMetadata(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = authRepository.getCurrentUserId() ?: throw Exception("Not logged in")
            val masterKey = sessionManager.getMasterKey()
                ?: throw IllegalStateException("Vault is locked")
            
            try {
                val doc = firestore.collection("users").document(userId)
                    .collection("metadataBackup").document("latest")
                    .get()
                    .await()
                
                if (!doc.exists()) {
                    throw Exception("No cloud backup found")
                }
                
                val ciphertextBase64 = doc.getString("ciphertext") ?: throw Exception("Invalid backup data")
                val ivBase64 = doc.getString("iv") ?: throw Exception("Invalid backup data")
                
                val encryptedData = EncryptedData(
                    ciphertext = Base64.getDecoder().decode(ciphertextBase64),
                    iv = Base64.getDecoder().decode(ivBase64)
                )
                
                val decryptedBytes = aesGcmCipher.decrypt(encryptedData, masterKey)
                val metadata = Json.decodeFromString<VaultMetadata>(String(decryptedBytes))
                
                // Save locally (encrypting it with master key for local storage)
                val localEncryptedData = aesGcmCipher.encrypt(Json.encodeToString(metadata).toByteArray(), masterKey)
                val combined = localEncryptedData.iv + localEncryptedData.ciphertext
                
                val metadataFile = getMetadataFile()
                metadataFile.parentFile?.mkdirs()
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
            val cleanEmail = recipientEmail.trim().lowercase()
            
            val recipientQuery = firestore.collection("users")
                .whereEqualTo("email", cleanEmail)
                .limit(1)
                .get()
                .await()
            
            if (recipientQuery.isEmpty) throw Exception("User with email $cleanEmail not found")
            val recipientDoc = recipientQuery.documents.first()
            val recipientId = recipientDoc.id
            
            val publicKeysMap = recipientDoc.get("publicKeys") as? Map<*, *> ?: throw Exception("Recipient public keys not found")
            
            val bundle = UserKeyBundle(
                userId = recipientId,
                x25519PublicKey = Base64.getDecoder().decode(publicKeysMap["x25519PublicKey"] as String),
                ed25519PublicKey = Base64.getDecoder().decode(publicKeysMap["ed25519PublicKey"] as String)
            )
            
            shareFileWithBundle(fileId, bundle).getOrThrow()
        }
    }

    override suspend fun shareFileWithBundle(fileId: String, bundle: UserKeyBundle): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val senderId = authRepository.getCurrentUserId() ?: throw Exception("Sender not logged in")
            val recipientId = bundle.userId
            val recipientX25519Pub = bundle.x25519PublicKey
            
            val senderX25519Priv = sessionManager.getX25519PrivateKey() ?: throw Exception("Vault is locked")
            val senderEd25519Priv = sessionManager.getEd25519PrivateKey() ?: throw Exception("Vault is locked")
            val masterKey = sessionManager.getMasterKey() ?: throw Exception("Vault is locked")
            
            try {
                val metadataResult = loadMetadata()
                val metadata = metadataResult.getOrThrow()
                val fileEntry = metadata.entries.find { it.id == fileId } ?: throw Exception("File not found")
                
                val fileKey = aesGcmCipher.decrypt(fileEntry.encryptedFileKey, masterKey)
                
                val sharedSecret = sharingCryptoManager.calculateSharedSecret(senderX25519Priv, recipientX25519Pub)
                val derivedKey = sharingCryptoManager.deriveSharedKey(sharedSecret, null, "FileSharing".toByteArray())
                val encryptedFileKeyForRecipient = aesGcmCipher.encrypt(fileKey, derivedKey)
                
                val shareId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                val signatureData = (senderId + recipientId + fileId + timestamp.toString()).toByteArray()
                val signature = sharingCryptoManager.signData(signatureData, senderEd25519Priv)
                
                val encryptedFile = File(fileEntry.storagePath)
                if (!encryptedFile.exists()) throw Exception("Encrypted file not found on disk")

                val storageRef = storage.reference.child("shares/$shareId/$fileId.enc")
                
                val storageMeta = storageMetadata {
                    setCustomMetadata("senderId", senderId)
                    setCustomMetadata("recipientId", recipientId)
                }
                storageRef.putFile(Uri.fromFile(encryptedFile), storageMeta).await()
                
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
                
                sharedSecret.fill(0)
                derivedKey.fill(0)
                fileKey.fill(0)
                Unit
            } finally {
                senderX25519Priv.fill(0)
                senderEd25519Priv.fill(0)
                masterKey.fill(0)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getIncomingShares(): Flow<List<ShareRecord>> {
        return flow {
            emit(authRepository.getCurrentUserId() ?: "")
        }.flatMapLatest { currentUserId ->
            firestore.collection("shares")
                .whereEqualTo("recipientId", currentUserId)
                .whereEqualTo("status", ShareStatus.PENDING.name)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        mapToShareRecord(doc.data ?: return@mapNotNull null)
                    }
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getSentShares(): Flow<List<ShareRecord>> {
        return flow {
            emit(authRepository.getCurrentUserId() ?: "")
        }.flatMapLatest { currentUserId ->
            firestore.collection("shares")
                .whereEqualTo("senderId", currentUserId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        mapToShareRecord(doc.data ?: return@mapNotNull null)
                    }
                }
        }
    }

    override suspend fun acceptShare(share: ShareRecord): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val recipientId = authRepository.getCurrentUserId() ?: throw Exception("Not logged in")
            val recipientX25519Priv = sessionManager.getX25519PrivateKey() ?: throw Exception("Vault locked")
            val masterKey = sessionManager.getMasterKey() ?: throw Exception("Vault locked")

            try {
                val sharedSecret = sharingCryptoManager.calculateSharedSecret(
                    recipientX25519Priv,
                    share.senderEphemeralPublicKey
                )

                val derivedKey = sharingCryptoManager.deriveSharedKey(sharedSecret, null, "FileSharing".toByteArray())

                val rawFileKey = aesGcmCipher.decrypt(share.encryptedFileKey, derivedKey)

                val localFile = File(getVaultDir(), "${share.fileId}.enc")
                val storageRef = storage.reference.child("shares/${share.shareId}/${share.fileId}.enc")
                storageRef.getFile(localFile).await()

                val encryptedFileKeyForVault = aesGcmCipher.encrypt(rawFileKey, masterKey)

                val metadata = loadMetadata().getOrThrow()
                val newEntry = VaultFileEntry(
                    id = share.fileId,
                    fileName = share.fileName,
                    mimeType = share.mimeType,
                    fileSize = share.fileSize,
                    createdAt = System.currentTimeMillis(),
                    storagePath = localFile.absolutePath,
                    encryptedFileKey = encryptedFileKeyForVault,
                    ownerId = recipientId,
                    isShared = true
                )
                val updatedMetadata = metadata.copy(entries = metadata.entries + newEntry)
                saveMetadata(updatedMetadata).getOrThrow()

                sharedSecret.fill(0)
                derivedKey.fill(0)
                rawFileKey.fill(0)

                firestore.collection("shares").document(share.shareId)
                    .update("status", ShareStatus.ACCEPTED.name)
                    .await()

                Unit
            } finally {
                recipientX25519Priv.fill(0)
                masterKey.fill(0)
            }
        }
    }

    override suspend fun rejectShare(shareId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            firestore.collection("shares").document(shareId)
                .update("status", ShareStatus.REJECTED.name).await()
            Unit
        }
    }

    override suspend fun deleteShareRecord(shareId: String, fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val path = "shares/$shareId/$fileId.enc"
            
            try {
                storage.reference.child(path).delete().await()
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val storageError = e as? StorageException
                
                val isPermissionError = msg.contains("403") || msg.contains("Permission denied") ||
                        storageError?.errorCode == StorageException.ERROR_NOT_AUTHORIZED
                
                val isNotFoundError = msg.contains("Object does not exist") || msg.contains("404") ||
                        storageError?.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND
                
                if (isNotFoundError || isPermissionError) {
                } else {
                    throw e 
                }
            }
            
            try {
                firestore.collection("shares").document(shareId).delete().await()
            } catch (e: Exception) {
                val msg = e.message ?: ""
                val isFirestorePermissionError = msg.contains("PERMISSION_DENIED") || 
                        (e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                
                if (!isFirestorePermissionError) {
                    throw e
                }
            }
            Unit
        }
    }

    override suspend fun startAutoCleanup() {
        val currentUserId = authRepository.getCurrentUserId() ?: return
        cleanupJob?.cancel()
        cleanupJob = repositoryScope.launch {
            firestore.collection("shares")
                .whereEqualTo("senderId", currentUserId)
                .snapshots()
                .collect { snapshot ->
                    snapshot.documents.forEachIndexed { index, doc ->
                        val status = doc.getString("status")
                        val shareId = doc.id
                        val fileId = doc.getString("fileId")

                        if ((status == ShareStatus.ACCEPTED.name || status == ShareStatus.REJECTED.name) &&
                            fileId != null && !processingShares.contains(shareId)) {

                            processingShares.add(shareId)

                            repositoryScope.launch {
                                delay(index * 200L)
                                deleteShareRecord(shareId, fileId).onFailure { error ->
                                    processingShares.remove(shareId)
                                }
                            }
                        }
                    }
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

    @Suppress("UNCHECKED_CAST")
    private fun mapToShareRecord(data: Map<String, Any>): ShareRecord {
        val encryptedFileKeyMap = data["encryptedFileKey"] as Map<String, String>
        return ShareRecord(
            shareId = data["shareId"] as String,
            senderId = data["senderId"] as String,
            recipientId = data["recipientId"] as String,
            fileId = data["fileId"] as String,
            fileName = data["fileName"] as String,
            fileSize = (data["fileSize"] as Number).toLong(),
            mimeType = data["mimeType"] as String,
            encryptedFileKey = EncryptedData(
                ciphertext = Base64.getDecoder().decode(encryptedFileKeyMap["ciphertext"]),
                iv = Base64.getDecoder().decode(encryptedFileKeyMap["iv"])
            ),
            senderEphemeralPublicKey = Base64.getDecoder().decode(data["senderEphemeralPublicKey"] as String),
            senderSignature = Base64.getDecoder().decode(data["senderSignature"] as String),
            timestamp = (data["timestamp"] as Number).toLong(),
            status = ShareStatus.valueOf(data["status"] as String)
        )
    }

    override fun getNewVaultFilePath(fileId: String): String {
        return File(getVaultDir(), "$fileId.enc").absolutePath
    }

    override suspend fun deleteVaultData(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete Local Vault Directory
            val vaultDir = getVaultDir(userId)
            if (vaultDir.exists()) {
                vaultDir.deleteRecursively()
            }
            
            // Delete Local Metadata Directory
            val internalDir = File(context.filesDir, userId)
            if (internalDir.exists()) {
                internalDir.deleteRecursively()
            }
            
            // Delete Metadata Backup in Cloud
            firestore.collection("users").document(userId)
                .collection("metadataBackup").document("latest")
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

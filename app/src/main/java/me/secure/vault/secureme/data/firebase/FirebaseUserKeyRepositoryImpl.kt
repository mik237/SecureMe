package me.secure.vault.secureme.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import me.secure.vault.secureme.domain.model.EncryptedData
import me.secure.vault.secureme.domain.model.UserKeyBundle
import me.secure.vault.secureme.domain.repository.EncryptedKeyBundle
import me.secure.vault.secureme.domain.repository.UserKeyRepository
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseUserKeyRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserKeyRepository {

    override suspend fun saveEncryptedKeys(
        userId: String,
        email: String,
        encryptedMasterKey: EncryptedData,
        encryptedX25519PrivateKey: EncryptedData,
        encryptedEd25519PrivateKey: EncryptedData,
        publicKeys: UserKeyBundle,
        salt: ByteArray
    ): Result<Unit> = runCatching {
        val userKeysRef = firestore.collection("users").document(userId)
        
        val data = hashMapOf(
            "email" to email,
            "keys" to hashMapOf(
                "masterKey" to encryptedDataToMap(encryptedMasterKey),
                "x25519PrivateKey" to encryptedDataToMap(encryptedX25519PrivateKey),
                "ed25519PrivateKey" to encryptedDataToMap(encryptedEd25519PrivateKey),
                "salt" to Base64.getEncoder().encodeToString(salt)
            ),
            "publicKeys" to hashMapOf(
                "x25519PublicKey" to Base64.getEncoder().encodeToString(publicKeys.x25519PublicKey),
                "ed25519PublicKey" to Base64.getEncoder().encodeToString(publicKeys.ed25519PublicKey)
            )
        )
        
        userKeysRef.set(data).await()
        Unit
    }

    override suspend fun getEncryptedKeys(userId: String): Result<EncryptedKeyBundle> = runCatching {
        val doc = firestore.collection("users").document(userId).get().await()
        if (!doc.exists()) throw Exception("User keys not found")
        
        val keys = doc.get("keys") as Map<*, *>
        
        EncryptedKeyBundle(
            encryptedMasterKey = mapToEncryptedData(keys["masterKey"] as Map<*, *>),
            encryptedX25519PrivateKey = mapToEncryptedData(keys["x25519PrivateKey"] as Map<*, *>),
            encryptedEd25519PrivateKey = mapToEncryptedData(keys["ed25519PrivateKey"] as Map<*, *>),
            salt = Base64.getDecoder().decode(keys["salt"] as String)
        )
    }

    override suspend fun getPublicKeys(userId: String): Result<UserKeyBundle?> = runCatching {
        val doc = firestore.collection("users").document(userId).get().await()
        if (!doc.exists()) return@runCatching null
        
        val publicKeysMap = doc.get("publicKeys") as? Map<*, *> ?: return@runCatching null
        
        UserKeyBundle(
            userId = userId,
            x25519PublicKey = Base64.getDecoder().decode(publicKeysMap["x25519PublicKey"] as String),
            ed25519PublicKey = Base64.getDecoder().decode(publicKeysMap["ed25519PublicKey"] as String)
        )
    }

    override suspend fun deleteUserKeys(userId: String): Result<Unit> = runCatching {
        firestore.collection("users").document(userId).delete().await()
        Unit
    }

    private fun encryptedDataToMap(data: EncryptedData): Map<String, String> {
        return mapOf(
            "ciphertext" to Base64.getEncoder().encodeToString(data.ciphertext),
            "iv" to Base64.getEncoder().encodeToString(data.iv)
        )
    }

    private fun mapToEncryptedData(map: Map<*, *>): EncryptedData {
        return EncryptedData(
            ciphertext = Base64.getDecoder().decode(map["ciphertext"] as String),
            iv = Base64.getDecoder().decode(map["iv"] as String)
        )
    }
}

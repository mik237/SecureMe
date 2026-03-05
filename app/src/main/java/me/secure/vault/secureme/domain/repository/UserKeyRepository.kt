package me.secure.vault.secureme.domain.repository

import me.secure.vault.secureme.domain.model.EncryptedData
import me.secure.vault.secureme.domain.model.UserKeyBundle

interface UserKeyRepository {
    suspend fun saveEncryptedKeys(
        userId: String,
        encryptedMasterKey: EncryptedData,
        encryptedX25519PrivateKey: EncryptedData,
        encryptedEd25519PrivateKey: EncryptedData,
        publicKeys: UserKeyBundle,
        salt: ByteArray
    ): Result<Unit>

    suspend fun getEncryptedKeys(userId: String): Result<EncryptedKeyBundle>
}

data class EncryptedKeyBundle(
    val encryptedMasterKey: EncryptedData,
    val encryptedX25519PrivateKey: EncryptedData,
    val encryptedEd25519PrivateKey: EncryptedData,
    val salt: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedKeyBundle

        if (encryptedMasterKey != other.encryptedMasterKey) return false
        if (encryptedX25519PrivateKey != other.encryptedX25519PrivateKey) return false
        if (encryptedEd25519PrivateKey != other.encryptedEd25519PrivateKey) return false
        if (!salt.contentEquals(other.salt)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptedMasterKey.hashCode()
        result = 31 * result + encryptedX25519PrivateKey.hashCode()
        result = 31 * result + encryptedEd25519PrivateKey.hashCode()
        result = 31 * result + salt.contentHashCode()
        return result
    }
}

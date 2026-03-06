package me.secure.vault.secureme.crypto

import me.secure.vault.secureme.domain.model.EncryptedData
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterKeyManager @Inject constructor(
    private val aesGcmCipher: AesGcmCipher
) {
    private val masterKeyLength = 32 // 256 bits

    fun generateMasterKey(): ByteArray {
        val key = ByteArray(masterKeyLength)
        SecureRandom().nextBytes(key)
        return key
    }

    fun wrapMasterKey(masterKey: ByteArray, wrappingKey: ByteArray): EncryptedData {
        return aesGcmCipher.encrypt(masterKey, wrappingKey)
    }

    fun unwrapMasterKey(encryptedMasterKey: EncryptedData, wrappingKey: ByteArray): ByteArray {
        return aesGcmCipher.decrypt(encryptedMasterKey, wrappingKey)
    }
}

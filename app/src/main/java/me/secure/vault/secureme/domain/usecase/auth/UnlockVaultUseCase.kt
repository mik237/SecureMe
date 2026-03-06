package me.secure.vault.secureme.domain.usecase.auth

import me.secure.vault.secureme.core.security.SessionManager
import me.secure.vault.secureme.crypto.KeyDerivationManager
import me.secure.vault.secureme.crypto.MasterKeyManager
import me.secure.vault.secureme.domain.repository.UserKeyRepository
import javax.inject.Inject

class UnlockVaultUseCase @Inject constructor(
    private val keyDerivationManager: KeyDerivationManager,
    private val masterKeyManager: MasterKeyManager,
    private val userKeyRepository: UserKeyRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(userId: String, password: String): Result<Unit> = runCatching {
        var derivedKey: ByteArray? = null
        var masterKey: ByteArray? = null
        var x25519Priv: ByteArray? = null
        var ed25519Priv: ByteArray? = null

        try {
            // 1. Fetch encrypted keys and salt from Firebase
            val keyBundle = userKeyRepository.getEncryptedKeys(userId).getOrThrow()

            // 2. Derive key from password and fetched salt
            derivedKey = keyDerivationManager.deriveKey(password, keyBundle.salt)

            // 3. Unwrap Master Key
            masterKey = masterKeyManager.unwrapMasterKey(keyBundle.encryptedMasterKey, derivedKey)

            // 4. Unwrap Private Keys
            x25519Priv = masterKeyManager.unwrapMasterKey(keyBundle.encryptedX25519PrivateKey, masterKey)
            ed25519Priv = masterKeyManager.unwrapMasterKey(keyBundle.encryptedEd25519PrivateKey, masterKey)

            // 5. Initialize SessionManager
            sessionManager.setKeys(
                masterKey = masterKey,
                x25519Priv = x25519Priv,
                ed25519Priv = ed25519Priv
            )
        } finally {
            // Rule 14.1 & 5.3.2: Securely wipe sensitive keys from memory immediately after use
            derivedKey?.fill(0)
            masterKey?.fill(0)
            x25519Priv?.fill(0)
            ed25519Priv?.fill(0)
        }
    }
}

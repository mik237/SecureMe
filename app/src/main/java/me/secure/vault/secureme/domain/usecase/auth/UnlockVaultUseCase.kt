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
        // 1. Fetch encrypted keys and salt from Firebase
        val keyBundle = userKeyRepository.getEncryptedKeys(userId).getOrThrow()

        // 2. Derive key from password and fetched salt
        val derivedKey = keyDerivationManager.deriveKey(password, keyBundle.salt)

        // 3. Unwrap Master Key
        val masterKey = masterKeyManager.unwrapMasterKey(keyBundle.encryptedMasterKey, derivedKey)

        // 4. Unwrap Private Keys
        val x25519Priv = masterKeyManager.unwrapMasterKey(keyBundle.encryptedX25519PrivateKey, masterKey)
        val ed25519Priv = masterKeyManager.unwrapMasterKey(keyBundle.encryptedEd25519PrivateKey, masterKey)

        // 5. Initialize SessionManager
        sessionManager.setKeys(
            masterKey = masterKey,
            x25519Priv = x25519Priv,
            ed25519Priv = ed25519Priv
        )
    }
}

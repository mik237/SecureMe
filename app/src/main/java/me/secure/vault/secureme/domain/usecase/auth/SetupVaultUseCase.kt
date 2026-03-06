package me.secure.vault.secureme.domain.usecase.auth

import me.secure.vault.secureme.core.security.SessionManager
import me.secure.vault.secureme.crypto.AsymmetricKeyManager
import me.secure.vault.secureme.crypto.KeyDerivationManager
import me.secure.vault.secureme.crypto.MasterKeyManager
import me.secure.vault.secureme.domain.model.UserKeyBundle
import me.secure.vault.secureme.domain.repository.UserKeyRepository
import javax.inject.Inject

class SetupVaultUseCase @Inject constructor(
    private val keyDerivationManager: KeyDerivationManager,
    private val masterKeyManager: MasterKeyManager,
    private val asymmetricKeyManager: AsymmetricKeyManager,
    private val userKeyRepository: UserKeyRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(userId: String, password: String): Result<Unit> = runCatching {
        var salt: ByteArray?
        var derivedKey: ByteArray? = null
        var masterKey: ByteArray? = null
        var x25519Priv: ByteArray? = null
        var ed25519Priv: ByteArray? = null

        try {
            // 1. Generate salt and derive key from password
            salt = keyDerivationManager.generateSalt()
            derivedKey = keyDerivationManager.deriveKey(password, salt)

            // 2. Generate Master Key
            masterKey = masterKeyManager.generateMasterKey()

            // 3. Generate Asymmetric Keys
            val x25519KeyPair = asymmetricKeyManager.generateX25519KeyPair()
            val ed25519KeyPair = asymmetricKeyManager.generateEd25519KeyPair()
            
            x25519Priv = x25519KeyPair.privateKey
            ed25519Priv = ed25519KeyPair.privateKey

            // 4. Encrypt Master Key with Derived Key
            val encryptedMasterKey = masterKeyManager.wrapMasterKey(masterKey, derivedKey)

            // 5. Encrypt Private Keys with Master Key
            val encryptedX25519Priv = masterKeyManager.wrapMasterKey(x25519Priv, masterKey)
            val encryptedEd25519Priv = masterKeyManager.wrapMasterKey(ed25519Priv, masterKey)

            // 6. Save to Repository
            val publicKeys = UserKeyBundle(
                userId = userId,
                x25519PublicKey = x25519KeyPair.publicKey,
                ed25519PublicKey = ed25519KeyPair.publicKey
            )

            userKeyRepository.saveEncryptedKeys(
                userId = userId,
                encryptedMasterKey = encryptedMasterKey,
                encryptedX25519PrivateKey = encryptedX25519Priv,
                encryptedEd25519PrivateKey = encryptedEd25519Priv,
                publicKeys = publicKeys,
                salt = salt
            ).getOrThrow()

            // 7. Initialize SessionManager
            // SessionManager takes copies of these keys internally
            sessionManager.setKeys(
                masterKey = masterKey,
                x25519Priv = x25519Priv,
                ed25519Priv = ed25519Priv
            )
        } finally {
            // Rule 14.1 & 5.3.2: Clear sensitive keys from memory immediately after use
            derivedKey?.fill(0)
            masterKey?.fill(0)
            x25519Priv?.fill(0)
            ed25519Priv?.fill(0)
        }
    }
}

package me.secure.vault.secureme.domain.usecase

import me.secure.vault.secureme.crypto.FingerprintGenerator
import me.secure.vault.secureme.domain.model.TrustedContact
import me.secure.vault.secureme.domain.repository.ContactRepository
import me.secure.vault.secureme.domain.repository.VaultRepository
import javax.inject.Inject

class ShareFileUseCase @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val contactRepository: ContactRepository,
    private val fingerprintGenerator: FingerprintGenerator
) {
    /**
     * Shares a file with a recipient. 
     * Strictly enforces that the recipient must be a verified and trusted contact.
     */
    suspend operator fun invoke(fileId: String, recipientEmail: String): Result<Unit> {
        val cleanEmail = recipientEmail.trim().lowercase()
        
        // 1. Fetch remote user bundle to get their current ID and keys
        val remoteUserResult = contactRepository.searchRemoteUser(cleanEmail)
        val remoteBundle = remoteUserResult.getOrElse { return Result.failure(it) }
            ?: return Result.failure(Exception("User not found with email $cleanEmail"))
            
        // 2. Check if user is in local contacts
        val localContact = contactRepository.getContactSync(remoteBundle.userId)
        
        // 3. Trust Enforcement
        if (localContact == null) {
            return Result.failure(
                SecurityException("Recipient must be added to your contacts before sharing sensitive files.")
            )
        }

        if (!localContact.isTrusted) {
            return Result.failure(
                SecurityException("Identity for ${localContact.displayName} is not verified. Please verify their fingerprint in Contacts before sharing.")
            )
        }
        
        // 4. Fingerprint Mismatch Detection (Key Continuity Check)
        val currentFingerprint = fingerprintGenerator.generateFingerprint(
            remoteBundle.x25519PublicKey,
            remoteBundle.ed25519PublicKey
        )
        
        if (currentFingerprint != localContact.trustedFingerprint) {
            return Result.failure(
                SecurityException("SECURITY WARNING: Identity fingerprint mismatch for ${localContact.displayName}. " +
                    "Their keys have changed on the server. Sharing blocked until identity is re-verified.")
            )
        }
        
        // 5. Proceed with sharing only if all trust checks passed
        return vaultRepository.shareFileWithBundle(fileId, remoteBundle)
    }
}

package me.secure.vault.secureme.domain.usecase.auth

import me.secure.vault.secureme.core.security.SessionManager
import me.secure.vault.secureme.domain.repository.AuthRepository
import me.secure.vault.secureme.domain.repository.UserKeyRepository
import me.secure.vault.secureme.domain.repository.VaultRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userKeyRepository: UserKeyRepository,
    private val vaultRepository: VaultRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(): Result<Unit> {
        return runCatching {
            val userId = authRepository.getCurrentUserId() ?: throw Exception("User not logged in")
            
            // 1. Delete Remote Keys from Firestore
            userKeyRepository.deleteUserKeys(userId).getOrThrow()
            
            // 2. Delete Vault Data (Local files, Internal Metadata, and Cloud Metadata Backup)
            vaultRepository.deleteVaultData(userId).getOrThrow()
            
            // 3. Clear Local Session
            sessionManager.lockVault()
            
            // 4. Delete Firebase Auth Account
            authRepository.deleteAccount().getOrThrow()
        }
    }
}

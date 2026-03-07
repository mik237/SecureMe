package me.secure.vault.secureme.domain.usecase

import me.secure.vault.secureme.core.security.SessionManager
import javax.inject.Inject

class LockVaultUseCase @Inject constructor(
    private val sessionManager: SessionManager
) {
    operator fun invoke() {
        sessionManager.lockVault()
    }
}

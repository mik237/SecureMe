package me.secure.vault.secureme.domain.usecase.auth

import me.secure.vault.secureme.core.security.SessionManager
import me.secure.vault.secureme.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(): Result<Unit> {
        sessionManager.lockVault()
        return authRepository.logout()
    }
}

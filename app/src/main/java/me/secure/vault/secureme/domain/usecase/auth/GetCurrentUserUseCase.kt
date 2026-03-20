package me.secure.vault.secureme.domain.usecase.auth

import me.secure.vault.secureme.domain.repository.AuthRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend fun getUserId(): String? = authRepository.getCurrentUserId()
    suspend fun getEmail(): String? = authRepository.getCurrentUserEmail()
}

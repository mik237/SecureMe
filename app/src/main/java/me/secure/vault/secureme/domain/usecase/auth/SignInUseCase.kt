package me.secure.vault.secureme.domain.usecase.auth

import me.secure.vault.secureme.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        return repository.signIn(email, password)
    }
}

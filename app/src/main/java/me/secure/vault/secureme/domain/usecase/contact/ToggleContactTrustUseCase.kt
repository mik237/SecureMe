package me.secure.vault.secureme.domain.usecase.contact

import me.secure.vault.secureme.domain.repository.ContactRepository
import javax.inject.Inject

class ToggleContactTrustUseCase @Inject constructor(
    private val repository: ContactRepository
) {
    suspend operator fun invoke(userId: String, isTrusted: Boolean): Result<Unit> {
        return repository.updateTrustStatus(userId, isTrusted)
    }
}

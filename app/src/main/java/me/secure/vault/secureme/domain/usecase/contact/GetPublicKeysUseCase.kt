package me.secure.vault.secureme.domain.usecase.contact

import me.secure.vault.secureme.domain.model.UserKeyBundle
import me.secure.vault.secureme.domain.repository.UserKeyRepository
import javax.inject.Inject

class GetPublicKeysUseCase @Inject constructor(
    private val userKeyRepository: UserKeyRepository
) {
    suspend operator fun invoke(userId: String): Result<UserKeyBundle?> {
        return userKeyRepository.getPublicKeys(userId)
    }
}

package me.secure.vault.secureme.domain.usecase.contact

import me.secure.vault.secureme.domain.model.UserKeyBundle
import me.secure.vault.secureme.domain.repository.ContactRepository
import javax.inject.Inject

class SearchUserUseCase @Inject constructor(
    private val repository: ContactRepository
) {
    suspend operator fun invoke(email: String): Result<UserKeyBundle?> {
        return repository.searchRemoteUser(email)
    }
}

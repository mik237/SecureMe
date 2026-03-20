package me.secure.vault.secureme.domain.usecase.contact

import kotlinx.coroutines.flow.Flow
import me.secure.vault.secureme.domain.model.TrustedContact
import me.secure.vault.secureme.domain.repository.ContactRepository
import javax.inject.Inject

class GetContactsUseCase @Inject constructor(
    private val repository: ContactRepository
) {
    operator fun invoke(): Flow<List<TrustedContact>> {
        return repository.getTrustedContacts()
    }
}

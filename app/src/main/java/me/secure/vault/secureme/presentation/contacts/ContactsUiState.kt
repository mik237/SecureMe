package me.secure.vault.secureme.presentation.contacts

import me.secure.vault.secureme.domain.model.TrustedContact

data class ContactsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val contacts: List<TrustedContact> = emptyList(),
    val error: String? = null
)

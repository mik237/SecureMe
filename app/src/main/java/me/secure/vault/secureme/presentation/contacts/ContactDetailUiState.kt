package me.secure.vault.secureme.presentation.contacts

import me.secure.vault.secureme.domain.model.TrustedContact

data class ContactDetailUiState(
    val isLoading: Boolean = false,
    val contact: TrustedContact? = null,
    val error: String? = null
)

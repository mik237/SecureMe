package me.secure.vault.secureme.presentation.contacts

sealed class ContactsUiEffect {
    data class NavigateTo(val route: String) : ContactsUiEffect()
    data class ShowSnackbar(val message: String) : ContactsUiEffect()
}

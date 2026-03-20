package me.secure.vault.secureme.presentation.contacts

sealed class ContactsUiIntent {
    object LoadContacts : ContactsUiIntent()
    data class DeleteContact(val userId: String) : ContactsUiIntent()
    data class SearchUser(val email: String) : ContactsUiIntent()
    data class SaveContact(
        val userId: String,
        val displayName: String,
        val email: String,
        val fingerprint: String
    ) : ContactsUiIntent()
    data class ToggleTrust(val userId: String, val isTrusted: Boolean) : ContactsUiIntent()
}

package me.secure.vault.secureme.presentation.contacts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.secure.vault.secureme.crypto.FingerprintGenerator
import me.secure.vault.secureme.domain.repository.ContactRepository
import me.secure.vault.secureme.domain.usecase.contact.GetContactUseCase
import me.secure.vault.secureme.domain.usecase.contact.ToggleContactTrustUseCase
import javax.inject.Inject

@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    private val getContactUseCase: GetContactUseCase,
    private val toggleContactTrustUseCase: ToggleContactTrustUseCase,
    private val contactRepository: ContactRepository,
    private val fingerprintGenerator: FingerprintGenerator,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: String = checkNotNull(savedStateHandle["userId"])

    private val _uiState = MutableStateFlow(ContactDetailUiState())
    val uiState: StateFlow<ContactDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<ContactsUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        loadContact()
    }

    private fun loadContact() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getContactUseCase(userId).collect { contact ->
                _uiState.update { it.copy(isLoading = false, contact = contact) }
                if (contact != null) {
                    checkIfKeysChanged(contact.email, contact.trustedFingerprint)
                }
            }
        }
    }

    private fun checkIfKeysChanged(email: String, localFingerprint: String) {
        viewModelScope.launch {
            contactRepository.searchRemoteUser(email).onSuccess { remoteBundle ->
                if (remoteBundle != null) {
                    val remoteFingerprint = fingerprintGenerator.generateFingerprint(
                        remoteBundle.x25519PublicKey,
                        remoteBundle.ed25519PublicKey
                    )
                    if (remoteFingerprint != localFingerprint) {
                        _uiState.update { it.copy(
                            hasFingerprintMismatch = true,
                            remoteFingerprint = remoteFingerprint
                        ) }
                    }
                }
            }
        }
    }

    fun toggleTrust(isTrusted: Boolean) {
        viewModelScope.launch {
            toggleContactTrustUseCase(userId, isTrusted).onFailure { error ->
                _uiEffect.send(ContactsUiEffect.ShowSnackbar(error.message ?: "Update failed"))
            }
        }
    }

    fun updateToRemoteFingerprint() {
        val contact = uiState.value.contact ?: return
        val newFingerprint = uiState.value.remoteFingerprint ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val updatedContact = contact.copy(
                trustedFingerprint = newFingerprint,
                verifiedAt = System.currentTimeMillis(),
                isTrusted = true
            )
            contactRepository.saveContact(updatedContact).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, hasFingerprintMismatch = false) }
                    _uiEffect.send(ContactsUiEffect.ShowSnackbar("Identity updated and trusted"))
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.send(ContactsUiEffect.ShowSnackbar(error.message ?: "Update failed"))
                }
            )
        }
    }
}

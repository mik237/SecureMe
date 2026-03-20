package me.secure.vault.secureme.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.secure.vault.secureme.crypto.FingerprintGenerator
import me.secure.vault.secureme.domain.model.TrustedContact
import me.secure.vault.secureme.domain.model.UserKeyBundle
import me.secure.vault.secureme.domain.repository.AuthRepository
import me.secure.vault.secureme.domain.repository.ContactRepository
import me.secure.vault.secureme.domain.usecase.contact.GetContactsUseCase
import me.secure.vault.secureme.domain.usecase.contact.SearchUserUseCase
import me.secure.vault.secureme.domain.usecase.contact.ToggleContactTrustUseCase
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val searchUserUseCase: SearchUserUseCase,
    private val toggleContactTrustUseCase: ToggleContactTrustUseCase,
    private val contactRepository: ContactRepository,
    private val authRepository: AuthRepository,
    private val fingerprintGenerator: FingerprintGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<ContactsUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private val _searchResult = MutableStateFlow<UserKeyBundle?>(null)
    val searchResult: StateFlow<UserKeyBundle?> = _searchResult.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadContacts()
    }

    fun onIntent(intent: ContactsUiIntent) {
        when (intent) {
            is ContactsUiIntent.LoadContacts -> loadContacts()
            is ContactsUiIntent.DeleteContact -> deleteContact(intent.userId)
            is ContactsUiIntent.SearchUser -> searchUser(intent.email)
            is ContactsUiIntent.SaveContact -> saveContact(intent)
            is ContactsUiIntent.ToggleTrust -> toggleTrust(intent.userId, intent.isTrusted)
        }
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getContactsUseCase().collect { contacts ->
                _uiState.update { it.copy(isLoading = false, contacts = contacts, error = null) }
            }
        }
    }

    private fun searchUser(email: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // Debounce period
            _uiState.update { it.copy(isLoading = true) }
            searchUserUseCase(email).fold(
                onSuccess = { userBundle ->
                    _uiState.update { it.copy(isLoading = false) }
                    // Filter out self
                    val currentUserId = authRepository.getCurrentUserIdSync()
                    if (userBundle?.userId == currentUserId) {
                        _searchResult.value = null
                    } else {
                        _searchResult.value = userBundle
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.send(ContactsUiEffect.ShowSnackbar(error.message ?: "Search failed"))
                }
            )
        }
    }

    private fun saveContact(intent: ContactsUiIntent.SaveContact) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val contact = TrustedContact(
                userId = intent.userId,
                displayName = intent.displayName,
                email = intent.email,
                trustedFingerprint = intent.fingerprint,
                verifiedAt = System.currentTimeMillis(),
                isTrusted = false // Default to false until manually verified
            )
            contactRepository.saveContact(contact).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.send(ContactsUiEffect.NavigateTo("contacts"))
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false) }
                    _uiEffect.send(ContactsUiEffect.ShowSnackbar(error.message ?: "Failed to save contact"))
                }
            )
        }
    }

    private fun deleteContact(userId: String) {
        viewModelScope.launch {
            contactRepository.deleteContact(userId).onFailure { error ->
                _uiEffect.send(ContactsUiEffect.ShowSnackbar(error.message ?: "Delete failed"))
            }
        }
    }

    private fun toggleTrust(userId: String, isTrusted: Boolean) {
        viewModelScope.launch {
            toggleContactTrustUseCase(userId, isTrusted).onFailure { error ->
                _uiEffect.send(ContactsUiEffect.ShowSnackbar(error.message ?: "Update failed"))
            }
        }
    }

    fun getFingerprint(userBundle: UserKeyBundle): String {
        return fingerprintGenerator.generateFingerprint(
            userBundle.x25519PublicKey,
            userBundle.ed25519PublicKey
        )
    }

}

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
import me.secure.vault.secureme.domain.usecase.contact.GetContactUseCase
import me.secure.vault.secureme.domain.usecase.contact.ToggleContactTrustUseCase
import javax.inject.Inject

@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    private val getContactUseCase: GetContactUseCase,
    private val toggleContactTrustUseCase: ToggleContactTrustUseCase,
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
}

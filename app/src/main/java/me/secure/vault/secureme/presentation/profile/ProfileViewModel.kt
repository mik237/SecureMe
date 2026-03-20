package me.secure.vault.secureme.presentation.profile

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
import me.secure.vault.secureme.core.utils.ClipboardClearManager
import me.secure.vault.secureme.crypto.FingerprintGenerator
import me.secure.vault.secureme.domain.usecase.auth.GetCurrentUserUseCase
import me.secure.vault.secureme.domain.usecase.auth.LogoutUseCase
import me.secure.vault.secureme.domain.usecase.contact.GetPublicKeysUseCase
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getPublicKeysUseCase: GetPublicKeysUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val fingerprintGenerator: FingerprintGenerator,
    private val clipboardClearManager: ClipboardClearManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<ProfileUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        loadProfile()
    }

    fun onIntent(intent: ProfileUiIntent) {
        when (intent) {
            ProfileUiIntent.LoadProfile -> loadProfile()
            ProfileUiIntent.OnLogoutClick -> logout()
            is ProfileUiIntent.OnCopyFingerprint -> copyFingerprint(intent.fingerprint)
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val userId = getCurrentUserUseCase.getUserId()
            val email = getCurrentUserUseCase.getEmail()
            
            if (userId != null && email != null) {
                getPublicKeysUseCase(userId).fold(
                    onSuccess = { bundle ->
                        if (bundle != null) {
                            val fingerprint = fingerprintGenerator.generateFingerprint(
                                bundle.x25519PublicKey,
                                bundle.ed25519PublicKey
                            )
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    email = email,
                                    fingerprint = fingerprint
                                )
                            }
                        } else {
                            _uiState.update { it.copy(isLoading = false, error = "Public keys not found") }
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                        _uiEffect.send(ProfileUiEffect.ShowError(error.message ?: "Failed to load keys"))
                    }
                )
            } else {
                _uiState.update { it.copy(isLoading = false, error = "User not authenticated") }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase().fold(
                onSuccess = {
                    _uiEffect.send(ProfileUiEffect.NavigateToOnBoarding)
                },
                onFailure = { error ->
                    _uiEffect.send(ProfileUiEffect.ShowError(error.message ?: "Logout failed"))
                }
            )
        }
    }

    private fun copyFingerprint(fingerprint: String) {
        clipboardClearManager.copyToClipboard("SecureMe Fingerprint", fingerprint)
        viewModelScope.launch {
            _uiEffect.send(ProfileUiEffect.ShowMessage("Fingerprint copied to clipboard. It will be cleared in 60 seconds."))
        }
    }
}

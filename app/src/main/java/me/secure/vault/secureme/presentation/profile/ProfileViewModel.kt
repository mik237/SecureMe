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
import me.secure.vault.secureme.crypto.FingerprintGenerator
import me.secure.vault.secureme.domain.repository.AuthRepository
import me.secure.vault.secureme.domain.repository.UserKeyRepository
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userKeyRepository: UserKeyRepository,
    private val fingerprintGenerator: FingerprintGenerator
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
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val userId = authRepository.getCurrentUserId()
            val email = authRepository.getCurrentUserEmail()
            
            if (userId != null && email != null) {
                userKeyRepository.getPublicKeys(userId).fold(
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
}

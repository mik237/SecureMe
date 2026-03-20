package me.secure.vault.secureme.presentation.lock

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
import me.secure.vault.secureme.domain.usecase.auth.GetCurrentUserUseCase
import me.secure.vault.secureme.domain.usecase.auth.UnlockVaultUseCase
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val unlockVaultUseCase: UnlockVaultUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<LockUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onIntent(intent: LockUiIntent) {
        when (intent) {
            is LockUiIntent.OnPasswordChange -> {
                _uiState.update { it.copy(password = intent.password, error = null) }
            }
            LockUiIntent.OnUnlockClick -> unlock()
        }
    }

    private fun unlock() {
        val password = _uiState.value.password
        if (password.isBlank()) {
            _uiState.update { it.copy(error = "Password cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val userId = getCurrentUserUseCase.getUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not found. Please login again.") }
                return@launch
            }

            unlockVaultUseCase(userId, password).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.send(LockUiEffect.UnlockSuccess)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Failed to unlock vault") }
                }
            )
        }
    }
}

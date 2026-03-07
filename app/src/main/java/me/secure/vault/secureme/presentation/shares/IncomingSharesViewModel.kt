package me.secure.vault.secureme.presentation.shares

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.secure.vault.secureme.domain.model.ShareRecord
import me.secure.vault.secureme.domain.repository.VaultRepository
import javax.inject.Inject

@HiltViewModel
class IncomingSharesViewModel @Inject constructor(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomingSharesUiState())
    val uiState: StateFlow<IncomingSharesUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<IncomingSharesUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        onIntent(IncomingSharesUiIntent.LoadShares)
    }

    fun onIntent(intent: IncomingSharesUiIntent) {
        when (intent) {
            IncomingSharesUiIntent.LoadShares -> loadShares()
            is IncomingSharesUiIntent.AcceptShare -> acceptShare(intent.share)
            is IncomingSharesUiIntent.RejectShare -> rejectShare(intent.shareId)
        }
    }

    private fun loadShares() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            vaultRepository.getIncomingShares()
                .onEach { shares ->
                    // Ensure unique shares by shareId to prevent LazyColumn crashes if data has duplicates
                    val uniqueShares = shares.distinctBy { it.shareId }
                    _uiState.update { it.copy(isLoading = false, shares = uniqueShares) }
                }
                .catch { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
                .collect()
        }
    }

    private fun acceptShare(share: ShareRecord) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            vaultRepository.acceptShare(share)
                .onSuccess {
                    _uiState.update { it.copy(isProcessing = false) }
                    _uiEffect.send(IncomingSharesUiEffect.ShareAccepted)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isProcessing = false) }
                    _uiEffect.send(IncomingSharesUiEffect.ShowError(error.message ?: "Failed to accept share"))
                }
        }
    }

    private fun rejectShare(shareId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            vaultRepository.rejectShare(shareId)
                .onSuccess {
                    _uiState.update { it.copy(isProcessing = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isProcessing = false) }
                    _uiEffect.send(IncomingSharesUiEffect.ShowError(error.message ?: "Failed to reject share"))
                }
        }
    }
}

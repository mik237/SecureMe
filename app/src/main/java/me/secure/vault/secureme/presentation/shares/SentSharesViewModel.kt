package me.secure.vault.secureme.presentation.shares

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.secure.vault.secureme.domain.repository.VaultRepository
import javax.inject.Inject

@HiltViewModel
class SentSharesViewModel @Inject constructor(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SentSharesUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<SentSharesUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        onIntent(SentSharesUiIntent.LoadShares)
    }

    fun onIntent(intent: SentSharesUiIntent) {
        when (intent) {
            SentSharesUiIntent.LoadShares -> loadShares()
            is SentSharesUiIntent.DeleteShare -> deleteShare(intent.shareId, intent.fileId)
        }
    }

    private fun loadShares() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            vaultRepository.getSentShares()
                .onEach { shares ->
                    // Ensure unique shares by shareId to prevent LazyColumn crashes
                    val uniqueShares = shares.distinctBy { it.shareId }
                    _uiState.update { it.copy(isLoading = false, shares = uniqueShares) }
                }
                .catch { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
                .collect()
        }
    }

    private fun deleteShare(shareId: String, fileId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            vaultRepository.deleteShareRecord(shareId, fileId)
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false) }
                    _uiEffect.send(SentSharesUiEffect.ShareDeleted)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isDeleting = false) }
                    _uiEffect.send(SentSharesUiEffect.ShowError(error.message ?: "Failed to delete share"))
                }
        }
    }
}

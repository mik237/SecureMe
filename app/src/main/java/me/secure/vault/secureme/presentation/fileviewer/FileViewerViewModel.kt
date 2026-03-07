package me.secure.vault.secureme.presentation.fileviewer

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
import me.secure.vault.secureme.domain.usecase.GetDecryptedFileUseCase
import me.secure.vault.secureme.domain.usecase.GetVaultFileEntryUseCase
import javax.inject.Inject

@HiltViewModel
class FileViewerViewModel @Inject constructor(
    private val getVaultFileEntryUseCase: GetVaultFileEntryUseCase,
    private val getDecryptedFileUseCase: GetDecryptedFileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileViewerUiState())
    val uiState: StateFlow<FileViewerUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<FileViewerUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onIntent(intent: FileViewerUiIntent) {
        when (intent) {
            is FileViewerUiIntent.LoadFile -> loadFile(intent.fileId)
        }
    }

    private fun loadFile(fileId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            getVaultFileEntryUseCase(fileId).onSuccess { entry ->
                _uiState.update { it.copy(fileEntry = entry) }

                getDecryptedFileUseCase(fileId).onSuccess { decryptedFile ->
                    _uiState.update { it.copy(isLoading = false, decryptedFile = decryptedFile) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                    _uiEffect.send(FileViewerUiEffect.ShowError(error.message ?: "Decryption failed"))
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                _uiEffect.send(FileViewerUiEffect.ShowError(error.message ?: "Failed to find file"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _uiState.value.decryptedFile?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }
}

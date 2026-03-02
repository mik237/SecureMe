package me.secure.vault.secureme.presentation.fileviewer

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class FileViewerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(FileViewerUiState())
    val uiState: StateFlow<FileViewerUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<FileViewerUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onIntent(intent: FileViewerUiIntent) {
        when (intent) {
            is FileViewerUiIntent.LoadFile -> {
                _uiState.update { it.copy(fileName = "File ID: ${intent.fileId}") }
            }
        }
    }
}

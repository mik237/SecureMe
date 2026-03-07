package me.secure.vault.secureme.presentation.home

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
import me.secure.vault.secureme.domain.usecase.GetVaultFilesUseCase
import me.secure.vault.secureme.domain.usecase.ImportFileUseCase
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getVaultFilesUseCase: GetVaultFilesUseCase,
    private val importFileUseCase: ImportFileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<HomeUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        onIntent(HomeUiIntent.LoadFiles)
    }

    fun onIntent(intent: HomeUiIntent) {
        when (intent) {
            is HomeUiIntent.LoadFiles -> loadFiles()
            is HomeUiIntent.OnTabSelected -> {
                _uiState.update { it.copy(selectedTab = intent.tab) }
                loadFiles()
            }
            is HomeUiIntent.ImportFile -> importFile(intent.uri)
            is HomeUiIntent.OpenFile -> {
                viewModelScope.launch {
                    _uiEffect.send(HomeUiEffect.OpenFileViewer(intent.file.id))
                }
            }
        }
    }

    private fun loadFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            getVaultFilesUseCase(uiState.value.selectedTab)
                .onSuccess { files ->
                    _uiState.update { it.copy(isLoading = false, files = files) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                    _uiEffect.send(HomeUiEffect.ShowError(error.message ?: "Failed to load files"))
                }
        }
    }

    private fun importFile(uri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            importFileUseCase(uri)
                .onSuccess {
                    loadFiles() // Refresh list after import
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEffect.send(HomeUiEffect.ShowError(error.message ?: "Import failed"))
                }
        }
    }
}

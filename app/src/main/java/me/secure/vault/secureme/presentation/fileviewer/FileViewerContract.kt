package me.secure.vault.secureme.presentation.fileviewer

data class FileViewerUiState(
    val isLoading: Boolean = false,
    val fileName: String = ""
)

sealed class FileViewerUiIntent {
    data class LoadFile(val fileId: String) : FileViewerUiIntent()
}

sealed class FileViewerUiEffect {
    object NavigateBack : FileViewerUiEffect()
}

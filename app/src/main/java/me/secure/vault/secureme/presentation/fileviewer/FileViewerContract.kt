package me.secure.vault.secureme.presentation.fileviewer

import me.secure.vault.secureme.domain.model.VaultFileEntry
import java.io.File

data class FileViewerUiState(
    val isLoading: Boolean = false,
    val fileEntry: VaultFileEntry? = null,
    val decryptedFile: File? = null,
    val errorMessage: String? = null
)

sealed class FileViewerUiIntent {
    data class LoadFile(val fileId: String) : FileViewerUiIntent()
}

sealed class FileViewerUiEffect {
    data class ShowError(val message: String) : FileViewerUiEffect()
}

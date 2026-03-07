package me.secure.vault.secureme.presentation.home

import android.net.Uri
import me.secure.vault.secureme.domain.model.HomeTab
import me.secure.vault.secureme.domain.model.VaultFileEntry

data class HomeUiState(
    val isLoading: Boolean = false,
    val files: List<VaultFileEntry> = emptyList(),
    val selectedTab: HomeTab = HomeTab.IMAGES,
    val errorMessage: String? = null,
    val fileToDelete: VaultFileEntry? = null,
    val fileToShare: VaultFileEntry? = null,
    val shareRecipientEmail: String = "",
    val isSharing: Boolean = false
)

sealed class HomeUiIntent {
    object LoadFiles : HomeUiIntent()
    data class OnTabSelected(val tab: HomeTab) : HomeUiIntent()
    data class ImportFile(val uri: Uri) : HomeUiIntent()
    data class OpenFile(val file: VaultFileEntry) : HomeUiIntent()
    data class ConfirmDeleteFile(val file: VaultFileEntry) : HomeUiIntent()
    object DismissDeleteDialog : HomeUiIntent()
    object DeleteFile : HomeUiIntent()
    
    data class OnShareFileClick(val file: VaultFileEntry) : HomeUiIntent()
    data class OnShareRecipientChange(val email: String) : HomeUiIntent()
    object DismissShareDialog : HomeUiIntent()
    object ShareFile : HomeUiIntent()
    
    object LockVault : HomeUiIntent()
}

sealed class HomeUiEffect {
    data class ShowError(val message: String) : HomeUiEffect()
    data class OpenFileViewer(val fileId: String) : HomeUiEffect()
    object FileSharedSuccessfully : HomeUiEffect()
    object NavigateToUnlock : HomeUiEffect()
}

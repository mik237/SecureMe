package me.secure.vault.secureme.presentation.home

import me.secure.vault.secureme.domain.model.HomeTab
import me.secure.vault.secureme.domain.model.VaultFileEntry
import me.secure.vault.secureme.domain.model.TrustedContact

data class HomeUiState(
    val isLoading: Boolean = false,
    val files: List<VaultFileEntry> = emptyList(),
    val selectedTab: HomeTab = HomeTab.IMAGES,
    val fileToDelete: VaultFileEntry? = null,
    val fileToShare: VaultFileEntry? = null,
    val isSharing: Boolean = false,
    val shareRecipientEmail: String = "",
    val shareError: String? = null,
    val trustedContacts: List<TrustedContact> = emptyList(),
    val showContactPicker: Boolean = false
)

sealed class HomeUiIntent {
    data class OnTabSelected(val tab: HomeTab) : HomeUiIntent()
    data class OpenFile(val file: VaultFileEntry) : HomeUiIntent()
    data class ConfirmDeleteFile(val file: VaultFileEntry) : HomeUiIntent()
    object DeleteFile : HomeUiIntent()
    object DismissDeleteDialog : HomeUiIntent()
    data class ImportFile(val uri: android.net.Uri) : HomeUiIntent()
    data class OnShareFileClick(val file: VaultFileEntry) : HomeUiIntent()
    data class OnShareRecipientChange(val email: String) : HomeUiIntent()
    object ShareFile : HomeUiIntent()
    object DismissShareDialog : HomeUiIntent()
    object LockVault : HomeUiIntent()
    object Logout : HomeUiIntent()
    object DeleteAccount : HomeUiIntent()
    object LoadTrustedContacts : HomeUiIntent()
    data class SelectContactForSharing(val email: String) : HomeUiIntent()
}

sealed class HomeUiEffect {
    data class ShowError(val message: String) : HomeUiEffect()
    data class OpenFileViewer(val fileId: String) : HomeUiEffect()
    object FileSharedSuccessfully : HomeUiEffect()
    object NavigateToUnlock : HomeUiEffect()
    object NavigateToLogin : HomeUiEffect()
}

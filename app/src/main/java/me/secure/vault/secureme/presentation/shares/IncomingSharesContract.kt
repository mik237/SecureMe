package me.secure.vault.secureme.presentation.shares

import me.secure.vault.secureme.domain.model.ShareRecord

data class IncomingSharesUiState(
    val isLoading: Boolean = false,
    val shares: List<ShareRecord> = emptyList(),
    val errorMessage: String? = null,
    val isProcessing: Boolean = false
)

sealed class IncomingSharesUiIntent {
    object LoadShares : IncomingSharesUiIntent()
    data class AcceptShare(val share: ShareRecord) : IncomingSharesUiIntent()
    data class RejectShare(val shareId: String) : IncomingSharesUiIntent()
}

sealed class IncomingSharesUiEffect {
    data class ShowError(val message: String) : IncomingSharesUiEffect()
    object ShareAccepted : IncomingSharesUiEffect()
}

package me.secure.vault.secureme.presentation.shares

import me.secure.vault.secureme.domain.model.ShareRecord

data class SentSharesUiState(
    val isLoading: Boolean = false,
    val shares: List<ShareRecord> = emptyList(),
    val isDeleting: Boolean = false,
    val error: String? = null
)

sealed class SentSharesUiIntent {
    object LoadShares : SentSharesUiIntent()
    data class DeleteShare(val shareId: String, val fileId: String) : SentSharesUiIntent()
}

sealed class SentSharesUiEffect {
    data class ShowError(val message: String) : SentSharesUiEffect()
    object ShareDeleted : SentSharesUiEffect()
}

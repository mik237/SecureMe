package me.secure.vault.secureme.presentation.lock

data class LockUiState(
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class LockUiIntent {
    data class OnPasswordChange(val password: String) : LockUiIntent()
    object OnUnlockClick : LockUiIntent()
}

sealed class LockUiEffect {
    object UnlockSuccess : LockUiEffect()
    data class ShowError(val message: String) : LockUiEffect()
}

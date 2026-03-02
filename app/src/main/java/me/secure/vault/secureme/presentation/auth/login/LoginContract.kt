package me.secure.vault.secureme.presentation.auth.login

data class LoginUiState(
    val isLoading: Boolean = false
)

sealed class LoginUiIntent {
    object OnLoginSuccess : LoginUiIntent()
}

sealed class LoginUiEffect {
    object NavigateToHome : LoginUiEffect()
}

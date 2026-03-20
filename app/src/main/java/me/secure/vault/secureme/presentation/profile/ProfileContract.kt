package me.secure.vault.secureme.presentation.profile

data class ProfileUiState(
    val isLoading: Boolean = false,
    val email: String = "",
    val fingerprint: String = "",
    val error: String? = null
)

sealed class ProfileUiIntent {
    object LoadProfile : ProfileUiIntent()
}

sealed class ProfileUiEffect {
    data class ShowError(val message: String) : ProfileUiEffect()
}

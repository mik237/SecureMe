package me.secure.vault.secureme.presentation.profile

data class ProfileUiState(
    val isLoading: Boolean = false,
    val email: String = "",
    val fingerprint: String = "",
    val error: String? = null
)

sealed class ProfileUiIntent {
    object LoadProfile : ProfileUiIntent()
    object OnLogoutClick : ProfileUiIntent()
    data class OnCopyFingerprint(val fingerprint: String) : ProfileUiIntent()
}

sealed class ProfileUiEffect {
    data class ShowError(val message: String) : ProfileUiEffect()
    data class ShowMessage(val message: String) : ProfileUiEffect()
    object NavigateToOnBoarding : ProfileUiEffect()
}

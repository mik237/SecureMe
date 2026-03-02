package me.secure.vault.secureme.presentation.onboarding

data class OnboardingUiState(
    val isLoading: Boolean = false
)

sealed class OnboardingUiIntent {
    object OnFinishOnboarding : OnboardingUiIntent()
}

sealed class OnboardingUiEffect {
    object NavigateToLogin : OnboardingUiEffect()
}

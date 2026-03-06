package me.secure.vault.secureme.presentation.onboarding

import androidx.compose.runtime.Immutable

@Immutable
data class OnboardingUiState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val passwordStrength: PasswordStrength = PasswordStrength.WEAK,
    val isLoading: Boolean = false,
    val isRegisterMode: Boolean = false,
    val isFormValid: Boolean = false
)

enum class PasswordStrength {
    WEAK, FAIR, STRONG
}

sealed class OnboardingUiIntent {
    data class OnEmailChange(val email: String) : OnboardingUiIntent()
    data class OnPasswordChange(val password: String) : OnboardingUiIntent()
    data class OnConfirmPasswordChange(val confirmPassword: String) : OnboardingUiIntent()
    object OnSubmit : OnboardingUiIntent()
    object OnToggleMode : OnboardingUiIntent()
}

sealed class OnboardingUiEffect {
    object NavigateToHome : OnboardingUiEffect()
    data class ShowError(val message: String) : OnboardingUiEffect()
    object NavigateToLogin : OnboardingUiEffect()
    data class ShowToast(val message: String) : OnboardingUiEffect()
}

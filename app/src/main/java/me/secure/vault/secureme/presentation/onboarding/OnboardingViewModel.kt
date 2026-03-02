package me.secure.vault.secureme.presentation.onboarding

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<OnboardingUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onIntent(intent: OnboardingUiIntent) {
        when (intent) {
            is OnboardingUiIntent.OnEmailChange -> {
                val filteredEmail = intent.email.filter { !it.isWhitespace() }
                _uiState.update { 
                    val newState = it.copy(email = filteredEmail, emailError = null)
                    newState.copy(isFormValid = validateForm(newState))
                }
            }
            is OnboardingUiIntent.OnPasswordChange -> {
                val filteredPassword = intent.password.filter { !it.isWhitespace() }
                _uiState.update { 
                    val newState = it.copy(
                        password = filteredPassword, 
                        passwordError = null,
                        passwordStrength = calculatePasswordStrength(filteredPassword)
                    )
                    newState.copy(isFormValid = validateForm(newState))
                }
            }
            is OnboardingUiIntent.OnConfirmPasswordChange -> {
                val filteredConfirmPassword = intent.confirmPassword.filter { !it.isWhitespace() }
                _uiState.update { 
                    val newState = it.copy(confirmPassword = filteredConfirmPassword, confirmPasswordError = null)
                    newState.copy(isFormValid = validateForm(newState))
                }
            }
            OnboardingUiIntent.OnToggleMode -> {
                _uiState.update { 
                    val newState = it.copy(isRegisterMode = !it.isRegisterMode)
                    newState.copy(isFormValid = validateForm(newState))
                }
            }
            OnboardingUiIntent.OnSubmit -> {
                submit()
            }
        }
    }

    private fun validateForm(state: OnboardingUiState): Boolean {
        val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(state.email).matches()
        val isPasswordValid = validatePassword(state.password) == null
        
        return if (state.isRegisterMode) {
            val isConfirmPasswordValid = state.password == state.confirmPassword && state.confirmPassword.isNotEmpty()
            isEmailValid && isPasswordValid && isConfirmPasswordValid
        } else {
            isEmailValid && state.password.isNotEmpty()
        }
    }

    private fun submit() {
        val currentState = _uiState.value
        
        // Final sanity check before submission to show errors if any (though button should be disabled)
        val emailError = if (!Patterns.EMAIL_ADDRESS.matcher(currentState.email).matches()) {
            "Invalid email address"
        } else null

        val passwordError = validatePassword(currentState.password)
        
        val confirmPasswordError = if (currentState.isRegisterMode && currentState.password != currentState.confirmPassword) {
            "Passwords do not match"
        } else null

        if (emailError != null || passwordError != null || confirmPasswordError != null) {
            _uiState.update { 
                it.copy(
                    emailError = emailError, 
                    passwordError = passwordError, 
                    confirmPasswordError = confirmPasswordError
                ) 
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // TODO: Firebase Authentication Integration (Stage 5)
            _uiEffect.send(OnboardingUiEffect.ShowToast("TODO: Firebase implementation coming in Stage 5"))
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun validatePassword(password: String): String? {
        if (password.length < 12) return "Password must be at least 12 characters"
        if (!password.any { it.isUpperCase() }) return "Must contain at least one uppercase letter"
        if (!password.any { it.isLowerCase() }) return "Must contain at least one lowercase letter"
        if (!password.any { it.isDigit() }) return "Must contain at least one number"
        if (!password.any { !it.isLetterOrDigit() }) return "Must contain at least one special character"
        return null
    }

    private fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.length < 8) return PasswordStrength.WEAK
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        
        val criteriaMet = listOf(hasUpper, hasLower, hasDigit, hasSpecial).count { it }
        
        return when {
            password.length >= 12 && criteriaMet >= 4 -> PasswordStrength.STRONG
            password.length >= 8 && criteriaMet >= 2 -> PasswordStrength.FAIR
            else -> PasswordStrength.WEAK
        }
    }
}

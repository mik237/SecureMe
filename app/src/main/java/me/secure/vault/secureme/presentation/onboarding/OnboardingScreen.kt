package me.secure.vault.secureme.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import me.secure.vault.secureme.presentation.composables.SecureMeSnackbar
import me.secure.vault.secureme.presentation.composables.SecureMeSnackbarVisuals
import me.secure.vault.secureme.presentation.composables.secureMeTextFieldColors
import me.secure.vault.secureme.presentation.navigation.NavigationRoutes

@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is OnboardingUiEffect.NavigateToHome -> {
                    keyboardController?.hide()
                    navController.navigate(NavigationRoutes.HOME) {
                        popUpTo(NavigationRoutes.ONBOARDING) { inclusive = true }
                    }
                }
                is OnboardingUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        SecureMeSnackbarVisuals(
                            message = effect.message,
                            isError = true
                        )
                    )
                }
                is OnboardingUiEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(
                        SecureMeSnackbarVisuals(
                            message = effect.message,
                            isError = false
                        )
                    )
                }
                OnboardingUiEffect.NavigateToLogin -> {
                    // Toggled via state
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                SecureMeSnackbar(snackbarData = data)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (uiState.isRegisterMode) "Create Account" else "Secure Access",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = if (uiState.isRegisterMode) {
                    "Experience the ultimate standard in digital privacy.\nSecure your world with zero-knowledge encryption."
                } else {
                    "Unlock your private sanctuary. Sign in to safely\naccess your encrypted data and communications."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 32.dp)
            )

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.onIntent(OnboardingUiIntent.OnEmailChange(it)) },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.emailError != null,
                supportingText = { uiState.emailError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                colors = secureMeTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            var passwordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.onIntent(OnboardingUiIntent.OnPasswordChange(it)) },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.passwordError != null,
                supportingText = { uiState.passwordError?.let { Text(it) } },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (uiState.isRegisterMode) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = {
                        keyboardController?.hide()
                        viewModel.onIntent(OnboardingUiIntent.OnSubmit)
                    }
                ),
                colors = secureMeTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (uiState.isRegisterMode) {
                AnimatedVisibility(visible = uiState.passwordStrength != PasswordStrength.STRONG) {
                    Text(
                        text = "Use 12+ characters with upper, lower, numbers & symbols",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                PasswordStrengthIndicator(uiState.passwordStrength)

                Spacer(modifier = Modifier.height(16.dp))

                var confirmPasswordVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = uiState.confirmPassword,
                    onValueChange = { viewModel.onIntent(OnboardingUiIntent.OnConfirmPasswordChange(it)) },
                    label = { Text("Confirm Password") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.confirmPasswordError != null,
                    supportingText = { uiState.confirmPasswordError?.let { Text(it) } },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            viewModel.onIntent(OnboardingUiIntent.OnSubmit)
                        }
                    ),
                    colors = secureMeTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.onIntent(OnboardingUiIntent.OnSubmit)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (uiState.isRegisterMode) "Create Account" else "Login",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { viewModel.onIntent(OnboardingUiIntent.OnToggleMode) }) {
                Text(
                    text = if (uiState.isRegisterMode) 
                        "Already have an account? Login" 
                    else "Don't have an account? Create one",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PasswordStrengthIndicator(strength: PasswordStrength) {
    val color by animateColorAsState(
        targetValue = when (strength) {
            PasswordStrength.WEAK -> MaterialTheme.colorScheme.error
            PasswordStrength.FAIR -> Color(0xFFFFA726) // Orange
            PasswordStrength.STRONG -> Color(0xFF66BB6A) // Success color
        },
        label = "strength_color"
    )

    val label = when (strength) {
        PasswordStrength.WEAK -> "Weak"
        PasswordStrength.FAIR -> "Fair"
        PasswordStrength.STRONG -> "Strong"
    }

    Column(modifier = Modifier.padding(top = 8.dp, start = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Password Strength: $label",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val segments = 3
            val filledSegments = when (strength) {
                PasswordStrength.WEAK -> 1
                PasswordStrength.FAIR -> 2
                PasswordStrength.STRONG -> 3
            }
            
            repeat(segments) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = if (index < filledSegments) color else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

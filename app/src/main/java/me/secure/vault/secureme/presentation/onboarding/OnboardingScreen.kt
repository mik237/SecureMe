package me.secure.vault.secureme.presentation.onboarding

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import me.secure.vault.secureme.presentation.navigation.NavigationRoutes
import me.secure.vault.secureme.ui.theme.SecureMeColors

@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is OnboardingUiEffect.NavigateToHome -> {
                    navController.navigate(NavigationRoutes.HOME) {
                        popUpTo(NavigationRoutes.ONBOARDING) { inclusive = true }
                    }
                }
                is OnboardingUiEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
                is OnboardingUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                OnboardingUiEffect.NavigateToLogin -> {
                    // Toggled via state
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SecureMeColors.Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (uiState.isRegisterMode) "Create Account" else "Welcome Back",
                style = MaterialTheme.typography.headlineLarge,
                color = SecureMeColors.OnBackground,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = if (uiState.isRegisterMode) "Secure your digital life" else "Sign in to access your vault",
                style = MaterialTheme.typography.bodyMedium,
                color = SecureMeColors.OnSurface,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.onIntent(OnboardingUiIntent.OnEmailChange(it)) },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.emailError != null,
                supportingText = { uiState.emailError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = textFieldColors(),
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = textFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (uiState.isRegisterMode) {
                AnimatedVisibility(visible = uiState.passwordStrength != PasswordStrength.STRONG) {
                    Text(
                        text = "Use 12+ characters with upper, lower, numbers & symbols",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecureMeColors.OnSurface,
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.onIntent(OnboardingUiIntent.OnSubmit) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecureMeColors.Primary,
                    contentColor = Color.Black
                ),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
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
                    color = SecureMeColors.Primary
                )
            }
        }
    }
}

@Composable
fun PasswordStrengthIndicator(strength: PasswordStrength) {
    val color by animateColorAsState(
        targetValue = when (strength) {
            PasswordStrength.WEAK -> SecureMeColors.Error
            PasswordStrength.FAIR -> Color(0xFFFFA726) // Orange
            PasswordStrength.STRONG -> SecureMeColors.Success
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
                            color = if (index < filledSegments) color else SecureMeColors.SurfaceVariant,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SecureMeColors.Primary,
    unfocusedBorderColor = SecureMeColors.SurfaceVariant,
    focusedLabelColor = SecureMeColors.Primary,
    unfocusedLabelColor = SecureMeColors.OnSurface,
    cursorColor = SecureMeColors.Primary,
    errorBorderColor = SecureMeColors.Error,
    errorLabelColor = SecureMeColors.Error,
    focusedTextColor = SecureMeColors.OnBackground,
    unfocusedTextColor = SecureMeColors.OnBackground
)

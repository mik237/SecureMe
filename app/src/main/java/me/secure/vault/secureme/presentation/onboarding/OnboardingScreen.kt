package me.secure.vault.secureme.presentation.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import me.secure.vault.secureme.presentation.navigation.NavigationRoutes

@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is OnboardingUiEffect.NavigateToLogin -> {
                    navController.navigate(NavigationRoutes.LOGIN) {
                        popUpTo(NavigationRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            }
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { viewModel.onIntent(OnboardingUiIntent.OnFinishOnboarding) }) {
                Text(text = "Onboarding Screen (Finish)")
            }
        }
    }
}

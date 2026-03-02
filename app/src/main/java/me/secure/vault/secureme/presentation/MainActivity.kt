package me.secure.vault.secureme.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import me.secure.vault.secureme.presentation.navigation.NavGraph
import me.secure.vault.secureme.presentation.splash.SplashViewModel
import me.secure.vault.secureme.ui.theme.SecureMeTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            viewModel.isLoading.value
        }

        lifecycleScope.launch {
            viewModel.effect.collect {
                // You can handle other effects here if needed
            }
        }

        setContent {
            SecureMeTheme {
                NavGraph()
            }
        }
    }
}

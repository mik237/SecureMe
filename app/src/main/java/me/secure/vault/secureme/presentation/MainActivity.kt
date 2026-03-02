package me.secure.vault.secureme.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import me.secure.vault.secureme.presentation.navigation.AppNavGraph
import me.secure.vault.secureme.ui.theme.SecureMeTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // The splash screen stays until the app content is ready.
        // We can control this with splashScreen.setKeepOnScreenCondition if needed,
        // but for now, we'll let the SplashScreen composable handle the navigation.
        
        setContent {
            SecureMeTheme {
                AppNavGraph()
            }
        }
    }
}

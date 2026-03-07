package me.secure.vault.secureme.presentation

import android.os.Bundle
import android.view.WindowManager
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

        // Rule 9: FLAG_SECURE set on all windows — no screenshots, no app switcher previews
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        setContent {
            // Support both Light and Dark theme based on system selection
            SecureMeTheme {
                AppNavGraph()
            }
        }
    }
}

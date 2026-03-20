package me.secure.vault.secureme.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import me.secure.vault.secureme.core.security.RootDetectionUtil
import me.secure.vault.secureme.core.security.SessionManager
import me.secure.vault.secureme.core.security.VaultLockManager
import me.secure.vault.secureme.presentation.navigation.AppNavGraph
import me.secure.vault.secureme.presentation.navigation.NavigationRoutes
import me.secure.vault.secureme.ui.theme.SecureMeTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var vaultLockManager: VaultLockManager

    @Inject
    lateinit var sessionManager: SessionManager

    private var navController: NavHostController? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "Storage permissions are required for the vault to function.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // UI Protection: Prevent screenshots and screen recordings
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        checkAndRequestPermissions()
        
        setContent {
            SecureMeTheme {
                val controller = rememberNavController()
                navController = controller

                var showRootWarning by remember { mutableStateOf(RootDetectionUtil.isDeviceRooted()) }

                if (showRootWarning) {
                    AlertDialog(
                        onDismissRequest = { showRootWarning = false },
                        title = { Text("Security Warning") },
                        text = { Text("This device appears to be rooted. Rooting compromises the security of SecureMe. Use at your own risk.") },
                        confirmButton = {
                            TextButton(onClick = { showRootWarning = false }) {
                                Text("I Understand")
                            }
                        }
                    )
                }

                AppNavGraph(navController = controller)
            }
        }

        handleIntent(intent)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        vaultLockManager.onUserInteraction()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "OPEN_SHARES" || intent?.hasExtra("shareId") == true) {
            navController?.navigate(NavigationRoutes.SHARED_WITH_ME)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Explicitly clear session on activity destruction
        sessionManager.clearKeys()
    }
}

package me.secure.vault.secureme.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SecureMeColors.Primary,
    secondary = SecureMeColors.Secondary,
    tertiary = SecureMeColors.Accent,
    background = SecureMeColors.Background,
    surface = SecureMeColors.Surface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = SecureMeColors.OnBackground,
    onSurface = SecureMeColors.OnSurface,
    error = SecureMeColors.Error,
    surfaceVariant = SecureMeColors.SurfaceVariant
)

@Composable
fun SecureMeTheme(
    darkTheme: Boolean = true, // SecureMe is Dark Theme only as per guidelines
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        dynamicDarkColorScheme(context)
    } else {
        DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

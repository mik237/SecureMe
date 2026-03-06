package me.secure.vault.secureme.presentation.composables

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.secure.vault.secureme.ui.theme.SecureMeColors

/**
 * Custom Snackbar visuals to differentiate between error and success states globally.
 */
class SecureMeSnackbarVisuals(
    override val message: String,
    val isError: Boolean,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = true,
    override val duration: SnackbarDuration = SnackbarDuration.Short
) : SnackbarVisuals

/**
 * A global themed Snackbar for SecureMe that handles error and success states.
 */
@Composable
fun SecureMeSnackbar(snackbarData: SnackbarData) {
    val isError = (snackbarData.visuals as? SecureMeSnackbarVisuals)?.isError ?: false

    Snackbar(
        containerColor = if (isError) SecureMeColors.Error else SecureMeColors.Success,
        contentColor = Color.White,
        actionColor = Color.White,
        snackbarData = snackbarData,
        shape = RoundedCornerShape(12.dp)
    )
}

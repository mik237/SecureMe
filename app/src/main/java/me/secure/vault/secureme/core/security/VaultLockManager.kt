package me.secure.vault.secureme.core.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import me.secure.vault.secureme.core.utils.SecureLogger
import me.secure.vault.secureme.domain.repository.AppPreferencesRepository
import me.secure.vault.secureme.domain.usecase.LockVaultUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultLockManager @Inject constructor(
    private val lockVaultUseCase: LockVaultUseCase,
    private val preferencesRepository: AppPreferencesRepository,
    private val sessionManager: SessionManager
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var inactivityJob: Job? = null
    private var backgroundLockJob: Job? = null

    init {
        // Register to observe app lifecycle
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /**
     * Resets the inactivity timer. Should be called on every user interaction.
     */
    fun onUserInteraction() {
        if (!sessionManager.isVaultUnlocked()) return

        inactivityJob?.cancel()
        inactivityJob = scope.launch {
            val timeoutMinutes = preferencesRepository.getAutoLockTimeoutMinutes().first()
            delay(timeoutMinutes * 60 * 1000L)
            SecureLogger.d("Vault auto-locked due to inactivity ($timeoutMinutes minutes)")
            lockVaultUseCase()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        // App came to foreground
        backgroundLockJob?.cancel()
        SecureLogger.d("App in foreground, background lock timer cancelled.")
    }

    override fun onStop(owner: LifecycleOwner) {
        // App went to background
        if (!sessionManager.isVaultUnlocked()) return

        backgroundLockJob = scope.launch {
            SecureLogger.d("App in background, starting 30s lock timer.")
            delay(30_000) // 30 seconds
            SecureLogger.d("Vault auto-locked because app was in background for > 30s")
            lockVaultUseCase()
        }
    }

    fun stopMonitoring() {
        inactivityJob?.cancel()
        backgroundLockJob?.cancel()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }
}

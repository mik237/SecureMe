package me.secure.vault.secureme.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    fun getAutoLockTimeoutMinutes(): Flow<Int>
    suspend fun setAutoLockTimeoutMinutes(minutes: Int)
    
    fun isBiometricEnabled(): Flow<Boolean>
    suspend fun setBiometricEnabled(enabled: Boolean)

    fun isVaultInitialized(userId: String): Flow<Boolean>
    suspend fun setVaultInitialized(userId: String, initialized: Boolean)
}

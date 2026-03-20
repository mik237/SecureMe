package me.secure.vault.secureme.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.secure.vault.secureme.domain.repository.AppPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "secureme_prefs")

@Singleton
class AppPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppPreferencesRepository {

    private object Keys {
        fun vaultInitialized(userId: String) = booleanPreferencesKey("vault_initialized_$userId")
        val autoLockTimeout = intPreferencesKey("auto_lock_timeout_minutes")
        val biometricEnabled = booleanPreferencesKey("biometric_enabled")
    }

    override fun getAutoLockTimeoutMinutes(): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.autoLockTimeout] ?: 5 // Default 5 minutes
    }

    override suspend fun setAutoLockTimeoutMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.autoLockTimeout] = minutes
        }
    }

    override fun isBiometricEnabled(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.biometricEnabled] ?: false
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.biometricEnabled] = enabled
        }
    }

    override fun isVaultInitialized(userId: String): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.vaultInitialized(userId)] ?: false
    }

    override suspend fun setVaultInitialized(userId: String, initialized: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.vaultInitialized(userId)] = initialized
        }
    }
}

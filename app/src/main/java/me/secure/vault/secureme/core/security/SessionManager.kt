package me.secure.vault.secureme.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private var masterKey: ByteArray? = null
    private var x25519PrivateKey: ByteArray? = null
    private var ed25519PrivateKey: ByteArray? = null

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    /**
     * Stores the keys in memory. 
     * Note: We take a copy to ensure the caller's reference isn't the only one,
     * but the caller is still responsible for clearing their own temporary buffers.
     */
    fun setKeys(masterKey: ByteArray, x25519Priv: ByteArray, ed25519Priv: ByteArray) {
        this.masterKey = masterKey.copyOf()
        this.x25519PrivateKey = x25519Priv.copyOf()
        this.ed25519PrivateKey = ed25519Priv.copyOf()
        _isUnlocked.value = true
    }

    /**
     * Returns a copy of the master key.
     * WARNING: The caller MUST wipe this copy using .fill(0) after use.
     */
    fun getMasterKey(): ByteArray? = masterKey?.copyOf()
    
    fun getX25519PrivateKey(): ByteArray? = x25519PrivateKey?.copyOf()
    
    fun getEd25519PrivateKey(): ByteArray? = ed25519PrivateKey?.copyOf()

    fun isVaultUnlocked(): Boolean = masterKey != null

    /**
     * Wipes all sensitive keys from memory and locks the vault.
     */
    fun clearKeys() {
        masterKey?.fill(0)
        x25519PrivateKey?.fill(0)
        ed25519PrivateKey?.fill(0)
        
        masterKey = null
        x25519PrivateKey = null
        ed25519PrivateKey = null
        _isUnlocked.value = false
    }

    fun lockVault() {
        clearKeys()
    }
}

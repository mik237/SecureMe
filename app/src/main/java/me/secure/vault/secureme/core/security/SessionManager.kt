package me.secure.vault.secureme.core.security

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private var masterKey: ByteArray? = null
    private var x25519PrivateKey: ByteArray? = null
    private var ed25519PrivateKey: ByteArray? = null

    fun setKeys(masterKey: ByteArray, x25519Priv: ByteArray, ed25519Priv: ByteArray) {
        this.masterKey = masterKey.copyOf()
        this.x25519PrivateKey = x25519Priv.copyOf()
        this.ed25519PrivateKey = ed25519Priv.copyOf()
    }

    fun getMasterKey(): ByteArray? = masterKey?.copyOf()
    fun getX25519PrivateKey(): ByteArray? = x25519PrivateKey?.copyOf()
    fun getEd25519PrivateKey(): ByteArray? = ed25519PrivateKey?.copyOf()

    fun isVaultUnlocked(): Boolean = masterKey != null

    fun lockVault() {
        masterKey?.fill(0)
        x25519PrivateKey?.fill(0)
        ed25519PrivateKey?.fill(0)
        
        masterKey = null
        x25519PrivateKey = null
        ed25519PrivateKey = null
    }
}

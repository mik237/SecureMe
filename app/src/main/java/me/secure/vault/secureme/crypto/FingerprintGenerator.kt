package me.secure.vault.secureme.crypto

import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FingerprintGenerator @Inject constructor() {

    /**
     * Generates a SHA-256 fingerprint from concatenated X25519 and Ed25519 public keys.
     * Returns a human-readable hex string grouped by 4 characters (e.g., XXXX-XXXX-XXXX...).
     */
    fun generateFingerprint(x25519PublicKey: ByteArray, ed25519PublicKey: ByteArray): String {
        val concatenated = x25519PublicKey + ed25519PublicKey
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(concatenated)
        
        return hash.toHexString().uppercase().chunked(4).joinToString("-")
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }
}

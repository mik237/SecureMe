package me.secure.vault.secureme.domain.model

data class TrustedContact(
    val userId: String,
    val displayName: String,
    val email: String,
    val trustedFingerprint: String,       // SHA-256(x25519Public + ed25519Public), hex grouped
    val verifiedAt: Long,
    val isTrusted: Boolean
)

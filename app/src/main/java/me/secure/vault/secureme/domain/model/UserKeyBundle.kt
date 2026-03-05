package me.secure.vault.secureme.domain.model

data class UserKeyBundle(
    val userId: String,
    val x25519PublicKey: ByteArray,
    val ed25519PublicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserKeyBundle

        if (userId != other.userId) return false
        if (!x25519PublicKey.contentEquals(other.x25519PublicKey)) return false
        if (!ed25519PublicKey.contentEquals(other.ed25519PublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + x25519PublicKey.contentHashCode()
        result = 31 * result + ed25519PublicKey.contentHashCode()
        return result
    }
}

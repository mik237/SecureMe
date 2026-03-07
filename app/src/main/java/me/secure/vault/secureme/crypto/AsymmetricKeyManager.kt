package me.secure.vault.secureme.crypto

import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AsymmetricKeyManager @Inject constructor() {

    private val secureRandom = SecureRandom()

    fun generateX25519KeyPair(): KeyPairData {
        val generator = X25519KeyPairGenerator()
        generator.init(X25519KeyGenerationParameters(secureRandom))
        val keyPair = generator.generateKeyPair()
        val publicKey = (keyPair.public as X25519PublicKeyParameters).encoded
        val privateKey = (keyPair.private as X25519PrivateKeyParameters).encoded
        return KeyPairData(publicKey, privateKey)
    }

    fun generateEd25519KeyPair(): KeyPairData {
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(secureRandom))
        val keyPair = generator.generateKeyPair()
        val publicKey = (keyPair.public as Ed25519PublicKeyParameters).encoded
        val privateKey = (keyPair.private as Ed25519PrivateKeyParameters).encoded
        return KeyPairData(publicKey, privateKey)
    }

    data class KeyPairData(
        val publicKey: ByteArray,
        val privateKey: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as KeyPairData

            if (!publicKey.contentEquals(other.publicKey)) return false
            if (!privateKey.contentEquals(other.privateKey)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = publicKey.contentHashCode()
            result = 31 * result + privateKey.contentHashCode()
            return result
        }
    }
}

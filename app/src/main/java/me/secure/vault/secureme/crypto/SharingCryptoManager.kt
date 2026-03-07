package me.secure.vault.secureme.crypto

import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.crypto.digests.SHA256Digest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharingCryptoManager @Inject constructor() {

    /**
     * Performs X25519 Key Agreement to derive a shared secret.
     */
    fun calculateSharedSecret(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        val agreement = X25519Agreement()
        val privKeyParams = X25519PrivateKeyParameters(privateKey, 0)
        val pubKeyParams = X25519PublicKeyParameters(publicKey, 0)
        agreement.init(privKeyParams)
        val secret = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(pubKeyParams, secret, 0)
        return secret
    }

    /**
     * Derives a 256-bit key from a shared secret using HKDF-SHA256.
     */
    fun deriveSharedKey(sharedSecret: ByteArray, salt: ByteArray?, info: ByteArray?): ByteArray {
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(sharedSecret, salt, info))
        val derivedKey = ByteArray(32)
        hkdf.generateBytes(derivedKey, 0, 32)
        return derivedKey
    }

    /**
     * Signs data using Ed25519 private key.
     */
    fun signData(data: ByteArray, privateKey: ByteArray): ByteArray {
        val signer = Ed25519Signer()
        signer.init(true, Ed25519PrivateKeyParameters(privateKey, 0))
        signer.update(data, 0, data.size)
        return signer.generateSignature()
    }

    /**
     * Derives X25519 public key from private key.
     */
    fun getX25519PublicKey(privateKey: ByteArray): ByteArray {
        val privKeyParams = X25519PrivateKeyParameters(privateKey, 0)
        return privKeyParams.generatePublicKey().encoded
    }
}

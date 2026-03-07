package me.secure.vault.secureme.crypto

import me.secure.vault.secureme.domain.model.EncryptedData
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AesGcmCipher @Inject constructor() {

    private val secureRandom = SecureRandom()
    private val algorithm = "AES/GCM/NoPadding"
    private val provider = "BC"
    private val tagLength = 128
    private val ivLength = 12
    private val bufferSize = 8192

    fun encrypt(data: ByteArray, key: ByteArray): EncryptedData {
        val iv = ByteArray(ivLength)
        secureRandom.nextBytes(iv)
        
        val cipher = Cipher.getInstance(algorithm, provider)
        val spec = GCMParameterSpec(tagLength, iv)
        val secretKey = SecretKeySpec(key, "AES")
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val ciphertext = cipher.doFinal(data)
        
        return EncryptedData(ciphertext, iv)
    }

    fun decrypt(encryptedData: EncryptedData, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(algorithm, provider)
        val spec = GCMParameterSpec(tagLength, encryptedData.iv)
        val secretKey = SecretKeySpec(key, "AES")
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(encryptedData.ciphertext)
    }

    fun encryptStream(inputStream: InputStream, outputStream: OutputStream, key: ByteArray) {
        val iv = ByteArray(ivLength)
        secureRandom.nextBytes(iv)
        
        // Write IV at the beginning of the output stream
        outputStream.write(iv)

        val cipher = Cipher.getInstance(algorithm, provider)
        val spec = GCMParameterSpec(tagLength, iv)
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val buffer = ByteArray(bufferSize)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val output = cipher.update(buffer, 0, bytesRead)
            if (output != null) {
                outputStream.write(output)
            }
        }
        val finalOutput = cipher.doFinal()
        if (finalOutput != null) {
            outputStream.write(finalOutput)
        }
    }

    fun decryptStream(inputStream: InputStream, outputStream: OutputStream, key: ByteArray) {
        val iv = ByteArray(ivLength)
        val ivRead = inputStream.read(iv)
        if (ivRead != ivLength) {
            throw Exception("Invalid encrypted file: IV missing or incomplete")
        }

        val cipher = Cipher.getInstance(algorithm, provider)
        val spec = GCMParameterSpec(tagLength, iv)
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val buffer = ByteArray(bufferSize)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val output = cipher.update(buffer, 0, bytesRead)
            if (output != null) {
                outputStream.write(output)
            }
        }
        val finalOutput = cipher.doFinal()
        if (finalOutput != null) {
            outputStream.write(finalOutput)
        }
    }
}

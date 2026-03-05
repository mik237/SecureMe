package me.secure.vault.secureme.crypto

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyDerivationManager @Inject constructor() {

    private val argon2Kt = Argon2Kt()
    private val saltLength = 32
    private val iterations = 3
    private val memory = 65536
    private val parallelism = 2
    private val hashLength = 32

    suspend fun deriveKey(password: String, salt: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        val result = argon2Kt.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = password.toByteArray(),
            salt = salt,
            tCostInIterations = iterations,
            mCostInKibibyte = memory,
            parallelism = parallelism,
            hashLengthInBytes = hashLength
        )
        val hash = ByteArray(result.rawHash.remaining())
        result.rawHash.get(hash)
        hash
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(saltLength)
        SecureRandom().nextBytes(salt)
        return salt
    }
}

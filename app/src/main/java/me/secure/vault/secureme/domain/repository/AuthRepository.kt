package me.secure.vault.secureme.domain.repository

interface AuthRepository {
    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun isUserLoggedIn(): Boolean
    suspend fun getCurrentUserId(): String?
    fun getCurrentUserIdSync(): String?
    suspend fun logout(): Result<Unit>
}

package me.secure.vault.secureme.domain.repository

interface AuthRepository {
    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun isUserLoggedIn(): Boolean
    suspend fun logout(): Result<Unit>
}

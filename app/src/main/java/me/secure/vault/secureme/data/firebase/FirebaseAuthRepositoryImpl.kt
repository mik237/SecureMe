package me.secure.vault.secureme.data.firebase

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import me.secure.vault.secureme.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun register(email: String, password: String): Result<Unit> = runCatching {
        firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        Unit
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        Unit
    }

    override suspend fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override suspend fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    override fun getCurrentUserIdSync(): String? {
        return firebaseAuth.currentUser?.uid
    }

    override suspend fun getCurrentUserEmail(): String? {
        return firebaseAuth.currentUser?.email
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        firebaseAuth.signOut()
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        firebaseAuth.currentUser?.delete()?.await()
        Unit
    }
}

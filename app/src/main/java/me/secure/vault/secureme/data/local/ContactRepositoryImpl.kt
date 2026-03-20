package me.secure.vault.secureme.data.local

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import me.secure.vault.secureme.domain.model.TrustedContact
import me.secure.vault.secureme.domain.model.UserKeyBundle
import me.secure.vault.secureme.domain.repository.AuthRepository
import me.secure.vault.secureme.domain.repository.ContactRepository
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : ContactRepository {

    override fun getTrustedContacts(): Flow<List<TrustedContact>> {
        val currentUserId = authRepository.getCurrentUserIdSync() ?: return flowOf(emptyList())
        return contactDao.getTrustedContacts(currentUserId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getContact(userId: String): Flow<TrustedContact?> {
        val currentUserId = authRepository.getCurrentUserIdSync() ?: return flowOf(null)
        return contactDao.getContactById(userId, currentUserId).map { it?.toDomain() }
    }

    override suspend fun searchRemoteUser(email: String): Result<UserKeyBundle?> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) return@runCatching null

            val doc = snapshot.documents.first()
            val userId = doc.id
            val publicKeys = doc.get("publicKeys") as Map<*, *>
            
            UserKeyBundle(
                userId = userId,
                x25519PublicKey = Base64.getDecoder().decode(publicKeys["x25519PublicKey"] as String),
                ed25519PublicKey = Base64.getDecoder().decode(publicKeys["ed25519PublicKey"] as String)
            )
        }
    }

    override suspend fun saveContact(contact: TrustedContact): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentUserId = authRepository.getCurrentUserIdSync() 
                ?: throw Exception("User not authenticated")
            contactDao.insertContact(contact.toEntity(currentUserId))
        }
    }

    override suspend fun deleteContact(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentUserId = authRepository.getCurrentUserIdSync() 
                ?: throw Exception("User not authenticated")
            contactDao.deleteContact(userId, currentUserId)
        }
    }

    override suspend fun updateTrustStatus(userId: String, isTrusted: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentUserId = authRepository.getCurrentUserIdSync() 
                ?: throw Exception("User not authenticated")
            contactDao.updateTrustStatus(userId, currentUserId, isTrusted)
        }
    }
}

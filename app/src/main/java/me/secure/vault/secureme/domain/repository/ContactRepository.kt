package me.secure.vault.secureme.domain.repository

import kotlinx.coroutines.flow.Flow
import me.secure.vault.secureme.domain.model.TrustedContact
import me.secure.vault.secureme.domain.model.UserKeyBundle

interface ContactRepository {
    /**
     * Returns a flow of trusted contacts for the current user.
     */
    fun getTrustedContacts(): Flow<List<TrustedContact>>

    /**
     * Returns a flow of a specific contact by userId.
     */
    fun getContact(userId: String): Flow<TrustedContact?>

    /**
     * Returns a specific contact by userId (synchronous suspend).
     */
    suspend fun getContactSync(userId: String): TrustedContact?

    /**
     * Searches for a user by email in the remote database (Firestore).
     */
    suspend fun searchRemoteUser(email: String): Result<UserKeyBundle?>

    /**
     * Saves or updates a contact in the local database.
     */
    suspend fun saveContact(contact: TrustedContact): Result<Unit>

    /**
     * Deletes a contact from the local database.
     */
    suspend fun deleteContact(userId: String): Result<Unit>

    /**
     * Updates the trust status of a contact.
     */
    suspend fun updateTrustStatus(userId: String, isTrusted: Boolean): Result<Unit>
}

package me.secure.vault.secureme.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM trusted_contacts WHERE ownerId = :ownerId")
    fun getTrustedContacts(ownerId: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM trusted_contacts WHERE userId = :userId AND ownerId = :ownerId LIMIT 1")
    fun getContactById(userId: String, ownerId: String): Flow<ContactEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Query("DELETE FROM trusted_contacts WHERE userId = :userId AND ownerId = :ownerId")
    suspend fun deleteContact(userId: String, ownerId: String)

    @Query("UPDATE trusted_contacts SET isTrusted = :isTrusted WHERE userId = :userId AND ownerId = :ownerId")
    suspend fun updateTrustStatus(userId: String, ownerId: String, isTrusted: Boolean)
}

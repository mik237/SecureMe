package me.secure.vault.secureme.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import me.secure.vault.secureme.domain.model.TrustedContact

@Entity(tableName = "trusted_contacts")
data class ContactEntity(
    @PrimaryKey val userId: String,
    val ownerId: String, // Firebase UID of the current user for isolation
    val displayName: String,
    val email: String,
    val trustedFingerprint: String,
    val verifiedAt: Long,
    val isTrusted: Boolean
)

fun ContactEntity.toDomain() = TrustedContact(
    userId = userId,
    displayName = displayName,
    email = email,
    trustedFingerprint = trustedFingerprint,
    verifiedAt = verifiedAt,
    isTrusted = isTrusted
)

fun TrustedContact.toEntity(ownerId: String) = ContactEntity(
    userId = userId,
    ownerId = ownerId,
    displayName = displayName,
    email = email,
    trustedFingerprint = trustedFingerprint,
    verifiedAt = verifiedAt,
    isTrusted = isTrusted
)

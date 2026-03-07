package me.secure.vault.secureme.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class VaultFileEntry(
    val id: String,                        // UUID
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val createdAt: Long,                   // epoch millis
    val storagePath: String,               // absolute path in vault folder
    val encryptedFileKey: EncryptedData,   // file key encrypted with master key
    val ownerId: String,                   // Firebase UID
    val isShared: Boolean = false          // true if received from another user
)

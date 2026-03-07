package me.secure.vault.secureme.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ShareRecord(
    val shareId: String,
    val senderId: String,
    val recipientId: String,
    val fileId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val encryptedFileKey: EncryptedData,
    val senderEphemeralPublicKey: ByteArray,  // sender's X25519 ephemeral public key
    val senderSignature: ByteArray,            // Ed25519 signature over share data
    val timestamp: Long,
    val status: ShareStatus
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShareRecord

        if (shareId != other.shareId) return false
        if (senderId != other.senderId) return false
        if (recipientId != other.recipientId) return false
        if (fileId != other.fileId) return false
        if (fileName != other.fileName) return false
        if (fileSize != other.fileSize) return false
        if (mimeType != other.mimeType) return false
        if (encryptedFileKey != other.encryptedFileKey) return false
        if (!senderEphemeralPublicKey.contentEquals(other.senderEphemeralPublicKey)) return false
        if (!senderSignature.contentEquals(other.senderSignature)) return false
        if (timestamp != other.timestamp) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shareId.hashCode()
        result = 31 * result + senderId.hashCode()
        result = 31 * result + recipientId.hashCode()
        result = 31 * result + fileId.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + encryptedFileKey.hashCode()
        result = 31 * result + senderEphemeralPublicKey.contentHashCode()
        result = 31 * result + senderSignature.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }
}

enum class ShareStatus { PENDING, ACCEPTED, REJECTED }

package me.secure.vault.secureme.domain.repository

import kotlinx.coroutines.flow.Flow
import me.secure.vault.secureme.domain.model.ShareRecord
import me.secure.vault.secureme.domain.model.UserKeyBundle
import me.secure.vault.secureme.domain.model.VaultMetadata

interface VaultRepository {
    suspend fun initializeVault(): Result<Unit>
    suspend fun loadMetadata(): Result<VaultMetadata>
    suspend fun saveMetadata(metadata: VaultMetadata): Result<Unit>
    suspend fun backupMetadata(): Result<Unit>
    suspend fun restoreMetadata(): Result<Unit>
    suspend fun isLocalMetadataAvailable(): Boolean
    suspend fun deleteFile(fileId: String): Result<Unit>
    
    /**
     * Shares a file by performing user lookup by email internally.
     */
    suspend fun shareFile(fileId: String, recipientEmail: String): Result<Unit>

    /**
     * Shares a file with a pre-fetched UserKeyBundle.
     */
    suspend fun shareFileWithBundle(fileId: String, bundle: UserKeyBundle): Result<Unit>

    fun getIncomingShares(): Flow<List<ShareRecord>>
    fun getSentShares(): Flow<List<ShareRecord>>
    suspend fun acceptShare(share: ShareRecord): Result<Unit>
    suspend fun rejectShare(shareId: String): Result<Unit>
    suspend fun deleteShareRecord(shareId: String, fileId: String): Result<Unit>
    suspend fun startAutoCleanup()
    fun getNewVaultFilePath(fileId: String): String
}

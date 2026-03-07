package me.secure.vault.secureme.domain.repository

import kotlinx.coroutines.flow.Flow
import me.secure.vault.secureme.domain.model.ShareRecord
import me.secure.vault.secureme.domain.model.VaultMetadata

interface VaultRepository {
    suspend fun initializeVault(): Result<Unit>
    suspend fun loadMetadata(): Result<VaultMetadata>
    suspend fun saveMetadata(metadata: VaultMetadata): Result<Unit>
    suspend fun deleteFile(fileId: String): Result<Unit>
    suspend fun shareFile(fileId: String, recipientEmail: String): Result<Unit>
    fun getIncomingShares(): Flow<List<ShareRecord>>
    fun getSentShares(): Flow<List<ShareRecord>>
    suspend fun acceptShare(share: ShareRecord): Result<Unit>
    suspend fun rejectShare(shareId: String): Result<Unit>
    suspend fun deleteShareRecord(shareId: String, fileId: String): Result<Unit>
    suspend fun startAutoCleanup()
    fun getNewVaultFilePath(fileId: String): String
}

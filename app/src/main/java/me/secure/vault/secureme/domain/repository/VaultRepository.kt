package me.secure.vault.secureme.domain.repository

import me.secure.vault.secureme.domain.model.VaultMetadata

interface VaultRepository {
    suspend fun initializeVault(): Result<Unit>
    suspend fun loadMetadata(): Result<VaultMetadata>
    suspend fun saveMetadata(metadata: VaultMetadata): Result<Unit>
    suspend fun deleteFile(fileId: String): Result<Unit>
    suspend fun shareFile(fileId: String, recipientEmail: String): Result<Unit>
    fun getNewVaultFilePath(fileId: String): String
}

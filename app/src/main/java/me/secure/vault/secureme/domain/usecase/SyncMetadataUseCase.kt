package me.secure.vault.secureme.domain.usecase

import me.secure.vault.secureme.domain.repository.VaultRepository
import javax.inject.Inject

class SyncMetadataUseCase @Inject constructor(
    private val vaultRepository: VaultRepository
) {
    /**
     * Checks if local metadata exists. If not, attempts to restore from cloud backup.
     * This should be called during vault initialization/unlocking.
     */
    suspend fun restoreIfMissing(): Result<Unit> {
        // Only attempt restore if local metadata is strictly missing or empty
        if (vaultRepository.isLocalMetadataAvailable()) {
            val metadataResult = vaultRepository.loadMetadata()
            if (metadataResult.isSuccess && metadataResult.getOrThrow().entries.isNotEmpty()) {
                // We have local data, no need to restore from cloud right now.
                return Result.success(Unit)
            }
        }

        // Local metadata is missing or empty, attempt cloud restore
        return vaultRepository.restoreMetadata()
    }

    /**
     * Manually triggers a backup of the current local metadata to the cloud.
     */
    suspend fun backup(): Result<Unit> {
        return vaultRepository.backupMetadata()
    }
}

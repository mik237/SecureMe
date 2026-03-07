package me.secure.vault.secureme.domain.usecase

import me.secure.vault.secureme.domain.model.VaultFileEntry
import me.secure.vault.secureme.domain.repository.VaultRepository
import javax.inject.Inject

class GetVaultFileEntryUseCase @Inject constructor(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(fileId: String): Result<VaultFileEntry> {
        return vaultRepository.loadMetadata().mapCatching { metadata ->
            metadata.entries.find { it.id == fileId }
                ?: throw Exception("File not found")
        }
    }
}

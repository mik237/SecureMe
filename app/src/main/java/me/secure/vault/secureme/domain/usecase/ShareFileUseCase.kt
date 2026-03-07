package me.secure.vault.secureme.domain.usecase

import me.secure.vault.secureme.domain.repository.VaultRepository
import javax.inject.Inject

class ShareFileUseCase @Inject constructor(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(fileId: String, recipientEmail: String): Result<Unit> {
        return vaultRepository.shareFile(fileId, recipientEmail)
    }
}

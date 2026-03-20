package me.secure.vault.secureme.domain.usecase.sharing

import me.secure.vault.secureme.domain.repository.VaultRepository
import javax.inject.Inject

class RejectShareUseCase @Inject constructor(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(shareId: String): Result<Unit> {
        return vaultRepository.rejectShare(shareId)
    }
}

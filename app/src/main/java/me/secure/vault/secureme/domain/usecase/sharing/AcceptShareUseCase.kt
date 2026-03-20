package me.secure.vault.secureme.domain.usecase.sharing

import me.secure.vault.secureme.domain.model.ShareRecord
import me.secure.vault.secureme.domain.repository.VaultRepository
import javax.inject.Inject

class AcceptShareUseCase @Inject constructor(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(share: ShareRecord): Result<Unit> {
        return vaultRepository.acceptShare(share)
    }
}

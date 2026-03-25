package me.secure.vault.secureme.domain.usecase

import me.secure.vault.secureme.domain.model.VaultFileEntry
import me.secure.vault.secureme.domain.repository.VaultRepository
import me.secure.vault.secureme.domain.model.HomeTab
import javax.inject.Inject

class GetVaultFilesUseCase @Inject constructor(
    private val vaultRepository: VaultRepository
) {
    suspend operator fun invoke(tab: HomeTab): Result<List<VaultFileEntry>> {
        if (tab == HomeTab.SETTINGS) return Result.success(emptyList())
        
        return vaultRepository.loadMetadata().map { metadata ->
            metadata.entries.filter { entry ->
                when (tab) {
                    HomeTab.IMAGES -> entry.mimeType.startsWith("image/")
                    HomeTab.VIDEOS -> entry.mimeType.startsWith("video/")
                    HomeTab.DOCUMENTS -> isDocument(entry.mimeType)
                    HomeTab.SETTINGS -> false // Should not happen due to check above
                }
            }.sortedByDescending { it.createdAt }
        }
    }

    private fun isDocument(mimeType: String): Boolean {
        return mimeType.startsWith("text/") ||
                mimeType == "application/pdf" ||
                mimeType == "application/msword" ||
                mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                mimeType == "application/vnd.ms-excel" ||
                mimeType == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
                mimeType == "application/vnd.ms-powerpoint" ||
                mimeType == "application/vnd.openxmlformats-officedocument.presentationml.presentation" ||
                mimeType == "application/zip" ||
                mimeType == "application/x-rar-compressed"
    }
}

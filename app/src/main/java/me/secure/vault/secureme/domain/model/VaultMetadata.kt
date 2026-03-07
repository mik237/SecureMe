package me.secure.vault.secureme.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class VaultMetadata(
    val version: Int = 1,
    val ownerId: String,
    val entries: List<VaultFileEntry>
)

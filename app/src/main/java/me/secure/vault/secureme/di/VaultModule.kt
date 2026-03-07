package me.secure.vault.secureme.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.secure.vault.secureme.data.repository.LocalVaultRepositoryImpl
import me.secure.vault.secureme.domain.repository.VaultRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VaultModule {

    @Binds
    @Singleton
    abstract fun bindVaultRepository(
        localVaultRepositoryImpl: LocalVaultRepositoryImpl
    ): VaultRepository
}

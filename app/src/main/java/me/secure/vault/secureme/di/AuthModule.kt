package me.secure.vault.secureme.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.secure.vault.secureme.data.firebase.FirebaseAuthRepositoryImpl
import me.secure.vault.secureme.data.firebase.FirebaseUserKeyRepositoryImpl
import me.secure.vault.secureme.domain.repository.AuthRepository
import me.secure.vault.secureme.domain.repository.UserKeyRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: FirebaseAuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserKeyRepository(
        userKeyRepositoryImpl: FirebaseUserKeyRepositoryImpl
    ): UserKeyRepository
}

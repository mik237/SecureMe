package me.secure.vault.secureme.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.secure.vault.secureme.data.local.ContactDao
import me.secure.vault.secureme.data.local.ContactDatabase
import me.secure.vault.secureme.data.local.ContactRepositoryImpl
import me.secure.vault.secureme.domain.repository.ContactRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideContactDatabase(@ApplicationContext context: Context): ContactDatabase {
        return Room.databaseBuilder(
            context,
            ContactDatabase::class.java,
            "secureme_contacts.db"
        ).build()
    }

    @Provides
    fun provideContactDao(database: ContactDatabase): ContactDao {
        return database.contactDao()
    }

    @Provides
    @Singleton
    fun provideContactRepository(repository: ContactRepositoryImpl): ContactRepository {
        return repository
    }
}

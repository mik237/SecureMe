package me.secure.vault.secureme.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.secure.vault.secureme.BuildConfig
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseApp(@ApplicationContext context: Context): FirebaseApp {
        return FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAppCheck(firebaseApp: FirebaseApp): FirebaseAppCheck {
        val appCheck = FirebaseAppCheck.getInstance(firebaseApp)
        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
        return appCheck
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(
        @Suppress("UNUSED_PARAMETER") appCheck: FirebaseAppCheck
    ): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(
        @Suppress("UNUSED_PARAMETER") appCheck: FirebaseAppCheck
    ): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(
        @Suppress("UNUSED_PARAMETER") appCheck: FirebaseAppCheck
    ): FirebaseStorage = FirebaseStorage.getInstance()
}

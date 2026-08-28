package com.example.di

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.network.BackendApiService
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.BillingRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.WallpaperRepositoryImpl
import com.example.domain.repository.UserPreferencesRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object AppContainer {

    // Set CLOUDFLARE_WORKER_URL in your .env, e.g. https://your-worker.your-subdomain.workers.dev/
    val CLOUDFLARE_WORKER_URL: String = BuildConfig.CLOUDFLARE_WORKER_URL.ifBlank { "https://example.workers.dev" }

    val firebaseAuth by lazy { Firebase.auth }
    val firestore by lazy { Firebase.firestore }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val backendApi: BackendApiService by lazy {
        val validUrl = if (CLOUDFLARE_WORKER_URL.startsWith("http")) CLOUDFLARE_WORKER_URL else "https://example.workers.dev"
        val formattedUrl = if (validUrl.endsWith("/")) validUrl else "$validUrl/"
        Retrofit.Builder()
            .baseUrl(formattedUrl)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BackendApiService::class.java)
    }

    val authRepositoryImpl: AuthRepositoryImpl by lazy {
        AuthRepositoryImpl(firebaseAuth)
    }

    val userRepository: UserRepositoryImpl by lazy {
        UserRepositoryImpl(firestore, authRepositoryImpl)
    }

    private var appDatabase: AppDatabase? = null
    fun getDatabase(context: Context): AppDatabase {
        if (appDatabase == null) {
            appDatabase = AppDatabase.getDatabase(context)
        }
        return appDatabase!!
    }

    private var wallpaperRepositoryImpl: WallpaperRepositoryImpl? = null
    fun getWallpaperRepository(context: Context): WallpaperRepositoryImpl {
        if (wallpaperRepositoryImpl == null) {
            wallpaperRepositoryImpl = WallpaperRepositoryImpl(
                getDatabase(context).wallpaperDao(),
                firestore,
                authRepositoryImpl
            )
        }
        return wallpaperRepositoryImpl!!
    }

    private var billingRepositoryImpl: BillingRepositoryImpl? = null
    fun getBillingRepository(context: Context): BillingRepositoryImpl {
        if (billingRepositoryImpl == null) {
            billingRepositoryImpl = BillingRepositoryImpl(context, userRepository, authRepositoryImpl, backendApi)
            billingRepositoryImpl?.startBillingConnection()
        }
        return billingRepositoryImpl!!
    }

    private var userPreferencesRepository: UserPreferencesRepository? = null
    fun getUserPreferencesRepository(context: Context): UserPreferencesRepository {
        if (userPreferencesRepository == null) {
            userPreferencesRepository = UserPreferencesRepository(context)
        }
        return userPreferencesRepository!!
    }
}
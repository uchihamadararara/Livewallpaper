package com.example.di

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.network.SupabaseApiService
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.BillingRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.WallpaperRepositoryImpl
import com.example.domain.repository.UserPreferencesRepository
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object AppContainer {
    val SUPABASE_URL = BuildConfig.SUPABASE_URL
    val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val supabaseApi: SupabaseApiService by lazy {
        Retrofit.Builder()
            .baseUrl(if (SUPABASE_URL.endsWith("/")) SUPABASE_URL else "$SUPABASE_URL/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApiService::class.java)
    }

    val authRepositoryImpl: AuthRepositoryImpl by lazy {
        AuthRepositoryImpl(supabaseApi)
    }

    val userRepository: UserRepositoryImpl by lazy {
        UserRepositoryImpl(supabaseApi, authRepositoryImpl)
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
                supabaseApi,
                authRepositoryImpl
            )
        }
        return wallpaperRepositoryImpl!!
    }

    private var billingRepositoryImpl: BillingRepositoryImpl? = null
    fun getBillingRepository(context: Context): BillingRepositoryImpl {
        if (billingRepositoryImpl == null) {
            billingRepositoryImpl = BillingRepositoryImpl(context, userRepository, authRepositoryImpl, supabaseApi)
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

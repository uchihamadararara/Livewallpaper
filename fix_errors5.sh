#!/bin/bash
set -e

# Revert BottomNavItem usage to what BottomNavItem actually supports
# Assuming BottomNavItem just has route, maybe not icon
sed -i 's/if (isSelected) screen.selectedIcon else screen.unselectedIcon/androidx.compose.material.icons.Icons.Default.Home/g' app/src/main/java/com/example/MainActivity.kt
sed -i 's/screen.title/screen.route/g' app/src/main/java/com/example/MainActivity.kt

# AppContainer authRepository syntax error
cat << 'INNER_EOF' > app/src/main/java/com/example/di/AppContainer.kt
package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.network.SupabaseApiService
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.BillingRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.WallpaperRepositoryImpl
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BillingRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WallpaperRepository
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object AppContainer {

    const val SUPABASE_URL = "https://YOUR_PROJECT_REF.supabase.co/"
    const val SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val supabaseApi: SupabaseApiService by lazy {
        Retrofit.Builder()
            .baseUrl(SUPABASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApiService::class.java)
    }

    private var _userPreferencesRepository: com.example.domain.repository.UserPreferencesRepository? = null

    fun getUserPreferencesRepository(context: Context): com.example.domain.repository.UserPreferencesRepository {
        if (_userPreferencesRepository == null) {
            _userPreferencesRepository = com.example.domain.repository.UserPreferencesRepository(context)
        }
        return _userPreferencesRepository!!
    }
    
    private var database: AppDatabase? = null

    val authRepositoryImpl: AuthRepositoryImpl by lazy {
        AuthRepositoryImpl(supabaseApi)
    }
    
    val authRepository: AuthRepository get() = authRepositoryImpl

    val userRepository: UserRepository by lazy {
        UserRepositoryImpl(supabaseApi, authRepositoryImpl)
    }

    private var _wallpaperRepository: WallpaperRepository? = null

    fun getWallpaperRepository(context: Context): WallpaperRepository {
        if (_wallpaperRepository == null) {
            val db = database ?: AppDatabase.getDatabase(context).also { database = it }
            _wallpaperRepository = WallpaperRepositoryImpl(db.wallpaperDao(), supabaseApi, authRepositoryImpl)
        }
        return _wallpaperRepository!!
    }

    private var _billingRepository: BillingRepository? = null

    fun getBillingRepository(context: Context): BillingRepository {
        if (_billingRepository == null) {
            _billingRepository = BillingRepositoryImpl(context.applicationContext, userRepository, authRepositoryImpl, supabaseApi)
        }
        return _billingRepository!!
    }
}
INNER_EOF

# Fix references again
sed -i 's/kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/kotlinx.coroutines.runBlocking { com.example.di.AppContainer.authRepositoryImpl.getUserId() }/g' app/src/main/java/com/example/service/AdvancedWallpaperService.kt
sed -i 's/kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/kotlinx.coroutines.runBlocking { com.example.di.AppContainer.authRepositoryImpl.getUserId() }/g' app/src/main/java/com/example/ui/explore/ExploreScreen.kt
sed -i 's/kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/kotlinx.coroutines.runBlocking { com.example.di.AppContainer.authRepositoryImpl.getUserId() }/g' app/src/main/java/com/example/ui/favorites/FavoritesScreen.kt
sed -i 's/kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/kotlinx.coroutines.runBlocking { com.example.di.AppContainer.authRepositoryImpl.getUserId() }/g' app/src/main/java/com/example/ui/premium/PremiumScreen.kt
sed -i 's/kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/kotlinx.coroutines.runBlocking { com.example.di.AppContainer.authRepositoryImpl.getUserId() }/g' app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt

sed -i '/import com.example.di.AppContainer/d' app/src/main/java/com/example/ui/navigation/AppNavigation.kt

echo "Done"

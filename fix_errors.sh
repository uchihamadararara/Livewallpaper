#!/bin/bash
set -e

echo "Fixing MainActivity.kt..."
sed -i '/import com.google.firebase.FirebaseApp/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/import com.google.firebase.FirebaseOptions/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/FirebaseApp.getApps(this).isEmpty()/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/val options = FirebaseOptions.Builder()/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/.setApplicationId("1:703447056926:android:e3e9d8923a1d95c4779fdf")/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/.setProjectId("ais-dev-o2gvyffa2dxj2prpuezyms")/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/.setApiKey(BuildConfig.FIREBASE_API_KEY)/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/.build()/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/FirebaseApp.initializeApp(this, options)/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/fun initializeFirebase(context: Context, apiKey: String) {/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/if (FirebaseApp.getApps(context).isEmpty()) {/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/FirebaseApp.initializeApp(context, options)/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/}/d' app/src/main/java/com/example/MainActivity.kt

# A more robust removal of initializeFirebase and its usages
sed -i '/fun initializeFirebase/,/^    }/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/initializeFirebase(/d' app/src/main/java/com/example/MainActivity.kt


echo "Fixing AuthRepository.kt interface..."
cat << 'INNER_EOF' > app/src/main/java/com/example/domain/repository/AuthRepository.kt
package com.example.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val isLoggedIn: StateFlow<Boolean>
    suspend fun signInAnonymously(): Result<Unit>
    suspend fun signOut()
}
INNER_EOF

echo "Fixing AuthRepositoryImpl.kt..."
cat << 'INNER_EOF' > app/src/main/java/com/example/data/repository/AuthRepositoryImpl.kt
package com.example.data.repository

import com.example.data.network.SupabaseApiService
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val supabaseApi: SupabaseApiService
) : AuthRepository {
    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn
    
    private var currentToken: String? = null
    private var currentUserId: String? = null

    suspend fun getAccessToken(): String? = currentToken
    suspend fun getUserId(): String? = currentUserId

    override suspend fun signInAnonymously(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = emptyMap<String, String>()
                val response = supabaseApi.signUpAnonymously(
                    com.example.di.AppContainer.SUPABASE_ANON_KEY,
                    request
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    currentToken = body?.get("access_token") as? String
                    val user = body?.get("user") as? Map<*, *>
                    currentUserId = user?.get("id") as? String
                    _isLoggedIn.value = true
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Auth failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun signOut() {
        currentToken = null
        currentUserId = null
        _isLoggedIn.value = false
    }
}
INNER_EOF

echo "Fixing AppContainer.kt AuthRepository usage..."
sed -i 's/val authRepository: AuthRepository by lazy {/val authRepositoryImpl: AuthRepositoryImpl by lazy {/g' app/src/main/java/com/example/di/AppContainer.kt
sed -i 's/UserRepositoryImpl(supabaseApi, authRepository)/UserRepositoryImpl(supabaseApi, authRepositoryImpl)/g' app/src/main/java/com/example/di/AppContainer.kt
sed -i 's/WallpaperRepositoryImpl(db.wallpaperDao(), supabaseApi, authRepository)/WallpaperRepositoryImpl(db.wallpaperDao(), supabaseApi, authRepositoryImpl)/g' app/src/main/java/com/example/di/AppContainer.kt
sed -i 's/BillingRepositoryImpl(context.applicationContext, userRepository, authRepository, supabaseApi)/BillingRepositoryImpl(context.applicationContext, userRepository, authRepositoryImpl, supabaseApi)/g' app/src/main/java/com/example/di/AppContainer.kt
sed -i 's/AuthRepositoryImpl(supabaseApi)/AuthRepositoryImpl(supabaseApi)/g' app/src/main/java/com/example/di/AppContainer.kt
sed -i '/val authRepositoryImpl:/a\    val authRepository: AuthRepository get() = authRepositoryImpl' app/src/main/java/com/example/di/AppContainer.kt


echo "Fixing UI Screens unresolved auth references..."
sed -i 's/val currentUserId = AppContainer.auth.currentUser?.uid/val currentUserId = kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/g' app/src/main/java/com/example/ui/explore/ExploreScreen.kt
sed -i 's/val currentUserId = AppContainer.auth.currentUser?.uid/val currentUserId = kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/g' app/src/main/java/com/example/ui/favorites/FavoritesScreen.kt
sed -i 's/val currentUserId = AppContainer.auth.currentUser?.uid/val currentUserId = kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/g' app/src/main/java/com/example/ui/premium/PremiumScreen.kt
sed -i 's/val currentUserId = AppContainer.auth.currentUser?.uid/val currentUserId = kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/g' app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt

sed -i 's/val currentUserId = AppContainer.auth.currentUser?.uid/val currentUserId = kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/g' app/src/main/java/com/example/service/AdvancedWallpaperService.kt

echo "Done fixing."

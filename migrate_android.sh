#!/bin/bash
set -e

echo "Updating AppContainer.kt..."
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

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(supabaseApi)
    }

    val userRepository: UserRepository by lazy {
        UserRepositoryImpl(supabaseApi, authRepository)
    }

    private var _wallpaperRepository: WallpaperRepository? = null

    fun getWallpaperRepository(context: Context): WallpaperRepository {
        if (_wallpaperRepository == null) {
            val db = database ?: AppDatabase.getDatabase(context).also { database = it }
            _wallpaperRepository = WallpaperRepositoryImpl(db.wallpaperDao(), supabaseApi, authRepository)
        }
        return _wallpaperRepository!!
    }

    private var _billingRepository: BillingRepository? = null

    fun getBillingRepository(context: Context): BillingRepository {
        if (_billingRepository == null) {
            _billingRepository = BillingRepositoryImpl(context.applicationContext, userRepository, authRepository, supabaseApi)
        }
        return _billingRepository!!
    }
}
INNER_EOF

echo "Updating AuthRepositoryImpl.kt..."
cat << 'INNER_EOF' > app/src/main/java/com/example/data/repository/AuthRepositoryImpl.kt
package com.example.data.repository

import com.example.data.network.SupabaseApiService
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

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
                // Using explicit anonymous sign-in endpoint for Supabase
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
}
INNER_EOF

echo "Updating UserRepositoryImpl.kt..."
cat << 'INNER_EOF' > app/src/main/java/com/example/data/repository/UserRepositoryImpl.kt
package com.example.data.repository

import com.example.data.network.SupabaseApiService
import com.example.domain.models.RetainedEntitlement
import com.example.domain.models.UserProfile
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class UserRepositoryImpl(
    private val supabaseApi: SupabaseApiService,
    private val authRepository: AuthRepositoryImpl
) : UserRepository {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)

    override fun getUserProfile(uid: String): Flow<UserProfile?> = _userProfile

    override suspend fun refreshUserProfile(uid: String) {
        withContext(Dispatchers.IO) {
            try {
                val token = authRepository.getAccessToken() ?: return@withContext
                val res = supabaseApi.getUser(
                    com.example.di.AppContainer.SUPABASE_ANON_KEY,
                    "Bearer $token",
                    "eq.$uid"
                )
                if (res.isSuccessful && res.body()?.isNotEmpty() == true) {
                    val data = res.body()!!.first()
                    _userProfile.value = UserProfile(
                        uid = data["id"] as? String ?: uid,
                        displayName = data["display_name"] as? String,
                        email = data["email"] as? String,
                        subscriptionStatus = data["subscription_status"] as? String ?: "NONE",
                        subscriptionExpiry = (data["subscription_expiry"] as? Number)?.toLong() ?: 0L,
                        currentAppliedWallpaperId = data["current_applied_wallpaper_id"] as? String,
                        retainedPremiumWallpaper = if (data["retained_wallpaper_id"] != null) {
                            RetainedEntitlement(
                                wallpaperId = data["retained_wallpaper_id"] as String,
                                subscriptionId = data["retained_subscription_id"] as? String ?: "",
                                appliedAtTimestamp = (data["retained_applied_at"] as? Number)?.toLong() ?: 0L,
                                expiryTimestamp = (data["retained_expiry"] as? Number)?.toLong() ?: 0L
                            )
                        } else null
                    )
                } else {
                    // Create basic profile if missing
                    val newProfile = mapOf("id" to uid)
                    supabaseApi.createUser(
                        com.example.di.AppContainer.SUPABASE_ANON_KEY,
                        "Bearer $token",
                        user = newProfile
                    )
                    _userProfile.value = UserProfile(uid, "User", null, "NONE", 0L, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun applyWallpaper(uid: String, wallpaperId: String, isPremium: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val token = authRepository.getAccessToken() ?: return@withContext Result.failure(Exception("Not auth"))
                val request = mapOf("wallpaperId" to wallpaperId)
                val response = supabaseApi.applyWallpaper("Bearer $token", request)
                if (response.isSuccessful) {
                    refreshUserProfile(uid)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Server returned error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
INNER_EOF

echo "Updating WallpaperRepositoryImpl.kt..."
cat << 'INNER_EOF' > app/src/main/java/com/example/data/repository/WallpaperRepositoryImpl.kt
package com.example.data.repository

import com.example.data.local.WallpaperDao
import com.example.data.network.SupabaseApiService
import com.example.domain.models.AdvancedConfig
import com.example.domain.models.Wallpaper
import com.example.domain.repository.WallpaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WallpaperRepositoryImpl(
    private val dao: WallpaperDao,
    private val supabaseApi: SupabaseApiService,
    private val authRepository: AuthRepositoryImpl
) : WallpaperRepository {

    override fun getWallpapers(): Flow<List<Wallpaper>> = dao.getAllWallpapers()

    override suspend fun refreshWallpapers(limit: Int) {
        withContext(Dispatchers.IO) {
            try {
                val token = authRepository.getAccessToken() ?: return@withContext
                val res = supabaseApi.getWallpapers(
                    com.example.di.AppContainer.SUPABASE_ANON_KEY,
                    "Bearer $token",
                    limit = limit
                )
                
                if (res.isSuccessful && res.body() != null) {
                    val wallpapers = res.body()!!.mapNotNull { doc ->
                        try {
                            val id = doc["id"] as? String ?: return@mapNotNull null
                            val advancedMap = doc["advanced_config"] as? Map<*, *>
                            val existing = dao.getWallpaperByIdSync(id)
                            Wallpaper(
                                id = id,
                                title = doc["title"] as? String ?: "Untitled",
                                description = doc["description"] as? String,
                                type = doc["type"] as? String ?: "STATIC",
                                imageUrl = doc["image_url"] as? String,
                                videoUrl = doc["video_url"] as? String,
                                thumbnailUrl = doc["thumbnail_url"] as? String ?: "",
                                isPremium = doc["is_premium"] as? Boolean ?: false,
                                isTrending = doc["is_trending"] as? Boolean ?: false,
                                isNew = doc["is_new"] as? Boolean ?: false,
                                isFeatured = doc["is_featured"] as? Boolean ?: false,
                                soundAvailable = doc["sound_available"] as? Boolean ?: false,
                                advancedConfig = if (advancedMap != null) {
                                    AdvancedConfig(
                                        lockAnimationEnabled = advancedMap["lockAnimationEnabled"] as? Boolean ?: false,
                                        lockAnimationVideoUrl = advancedMap["lockAnimationVideoUrl"] as? String,
                                        unlockTransitionEnabled = advancedMap["unlockTransitionEnabled"] as? Boolean ?: false,
                                        unlockTransitionVideoUrl = advancedMap["unlockTransitionVideoUrl"] as? String,
                                        chargingAnimationEnabled = advancedMap["chargingAnimationEnabled"] as? Boolean ?: false,
                                        chargingAnimationVideoUrl = advancedMap["chargingAnimationVideoUrl"] as? String,
                                        restartOnScreenOn = advancedMap["restartOnScreenOn"] as? Boolean ?: true,
                                        loopMainVideo = advancedMap["loopMainVideo"] as? Boolean ?: true,
                                        stopWhenScreenOff = advancedMap["stopWhenScreenOff"] as? Boolean ?: true
                                    )
                                } else null,
                                createdAt = (doc["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                categoryIds = doc["category_ids"] as? List<String> ?: emptyList(),
                                isFavorite = existing?.isFavorite ?: false
                            )
                        } catch (e: Exception) { null }
                    }
                    if (wallpapers.isNotEmpty()) dao.insertWallpapers(wallpapers)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun getWallpaper(id: String): Flow<Wallpaper?> = dao.getWallpaperById(id)

    override suspend fun getPremiumMediaUrl(wallpaperId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = authRepository.getAccessToken() ?: return@withContext Result.failure(Exception("Not auth"))
                val request = mapOf("wallpaperId" to wallpaperId)
                val response = supabaseApi.getPremiumMediaUrl("Bearer $token", request)
                
                if (response.isSuccessful) {
                    val url = response.body()?.get("url") as? String
                    if (url != null) Result.success(url)
                    else Result.failure(Exception("Invalid response"))
                } else {
                    Result.failure(Exception("Server returned error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        withContext(Dispatchers.IO) { dao.updateFavoriteStatus(id, isFavorite) }
    }

    override fun getFavoriteWallpapers(): Flow<List<Wallpaper>> = dao.getFavoriteWallpapers()
}
INNER_EOF

echo "Updating BillingRepositoryImpl.kt..."
cat << 'INNER_EOF' > app/src/main/java/com/example/data/repository/BillingRepositoryImpl.kt
package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.example.data.network.SupabaseApiService
import com.example.domain.repository.BillingRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BillingRepositoryImpl(
    private val context: Context,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepositoryImpl,
    private val supabaseApi: SupabaseApiService
) : BillingRepository, PurchasesUpdatedListener {

    private val billingScope = CoroutineScope(Job() + Dispatchers.IO)
    private var isConnected = false

    private val _subscriptionProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    override val subscriptionProducts: StateFlow<List<ProductDetails>> = _subscriptionProducts

    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    override val purchases: StateFlow<List<Purchase>> = _purchases

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val subscriptionProductIds = listOf(
        "premium_3_days", "premium_7_days", "premium_monthly", "premium_yearly"
    )

    override fun startBillingConnection() {
        if (isConnected) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isConnected = true
                    billingScope.launch {
                        querySubscriptionProducts()
                        queryPurchases()
                    }
                } else retryBillingConnection()
            }
            override fun onBillingServiceDisconnected() {
                isConnected = false
                retryBillingConnection()
            }
        })
    }

    private fun retryBillingConnection() {
        billingScope.launch {
            delay(2000L)
            startBillingConnection()
        }
    }

    private suspend fun querySubscriptionProducts() {
        val productList = subscriptionProductIds.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it).setProductType(BillingClient.ProductType.SUBS).build()
        }
        val result = billingClient.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(productList).build())
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _subscriptionProducts.value = result.productDetailsList ?: emptyList()
        }
    }

    private suspend fun queryPurchases() {
        val result = billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val purchasesList = result.purchasesList
            _purchases.value = purchasesList
            purchasesList.forEach { handlePurchase(it) }
        }
    }

    override fun launchBillingFlow(activity: Activity, productDetails: ProductDetails, offerToken: String) {
        val params = listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).setOfferToken(offerToken).build())
        billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(params).build())
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            _purchases.value = purchases
            purchases.forEach { billingScope.launch { handlePurchase(it) } }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val result = billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())
                if (result.responseCode == BillingClient.BillingResponseCode.OK) verifyPurchaseWithBackend(purchase)
            } else {
                verifyPurchaseWithBackend(purchase)
            }
        }
    }

    private suspend fun verifyPurchaseWithBackend(purchase: Purchase) {
        val uid = authRepository.getUserId() ?: return
        try {
            val token = authRepository.getAccessToken() ?: return
            val request = mapOf("purchaseToken" to purchase.purchaseToken, "productId" to purchase.products.firstOrNull())
            val response = supabaseApi.verifyPurchase("Bearer $token", request)
            if (response.isSuccessful) {
                userRepository.refreshUserProfile(uid)
            }
        } catch (e: Exception) {
            Log.e("Billing", "Failed to verify purchase", e)
        }
    }
}
INNER_EOF

echo "Done migrating Android."

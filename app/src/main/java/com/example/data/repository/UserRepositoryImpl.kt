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
                    val errorBody = response.errorBody()?.string() ?: ""
                    Result.failure(Exception("Server returned error: ${response.code()} $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

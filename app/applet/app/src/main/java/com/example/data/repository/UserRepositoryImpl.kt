package com.example.data.repository

import com.example.domain.models.RetainedEntitlement
import com.example.domain.models.UserProfile
import com.example.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : UserRepository {

    // Cache the user profile to prevent unnecessary reads
    private val _userProfile = MutableStateFlow<UserProfile?>(null)

    override fun getUserProfile(uid: String): Flow<UserProfile?> {
        return _userProfile
    }

    override suspend fun refreshUserProfile(uid: String) {
        withContext(Dispatchers.IO) {
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val retainedMap = doc.get("retainedPremiumWallpaper") as? Map<*, *>
                    val retained = if (retainedMap != null) {
                        RetainedEntitlement(
                            wallpaperId = retainedMap["wallpaperId"] as? String ?: "",
                            subscriptionId = retainedMap["subscriptionId"] as? String ?: "",
                            appliedAtTimestamp = (retainedMap["appliedAtTimestamp"] as? Number)?.toLong() ?: 0L,
                            expiryTimestamp = (retainedMap["expiryTimestamp"] as? Number)?.toLong() ?: 0L
                        )
                    } else null

                    val profile = UserProfile(
                        uid = uid,
                        displayName = doc.getString("displayName"),
                        email = doc.getString("email"),
                        subscriptionStatus = doc.getString("subscriptionStatus") ?: "NONE",
                        subscriptionExpiry = doc.getLong("subscriptionExpiry") ?: 0L,
                        currentAppliedWallpaperId = doc.getString("currentAppliedWallpaperId"),
                        retainedPremiumWallpaper = retained
                    )
                    _userProfile.value = profile
                } else {
                    // Create default profile
                    val profile = UserProfile(
                        uid = uid,
                        displayName = "User",
                        email = null,
                        subscriptionStatus = "NONE",
                        subscriptionExpiry = 0L,
                        currentAppliedWallpaperId = null,
                        retainedPremiumWallpaper = null
                    )
                    firestore.collection("users").document(uid).set(profile).await()
                    _userProfile.value = profile
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun applyWallpaper(uid: String, wallpaperId: String, isPremium: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Call the backend function to apply the wallpaper securely.
                // The backend handles entitlement checks and atomically updates Firestore.
                val data = hashMapOf(
                    "wallpaperId" to wallpaperId
                )
                
                // We use getHttpsCallable to call the secure backend function
                val result = functions
                    .getHttpsCallable("applyWallpaper")
                    .call(data)
                    .await()
                
                // The backend returns a successful response, we can now refresh the profile
                refreshUserProfile(uid)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

package com.example.data.repository

import com.example.domain.models.RetainedEntitlement
import com.example.domain.models.UserProfile
import com.example.domain.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepositoryImpl
) : UserRepository {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    override fun getUserProfile(uid: String): Flow<UserProfile?> = _userProfile

    override suspend fun refreshUserProfile(uid: String) {
        withContext(Dispatchers.IO) {
            try {
                val docRef = firestore.collection("users").document(uid)
                val snapshot = docRef.get().await()

                if (snapshot.exists()) {
                    val data = snapshot.data ?: emptyMap()
                    val retained = data["retainedPremiumWallpaper"] as? Map<*, *>
                    _userProfile.value = UserProfile(
                        uid = uid,
                        displayName = data["displayName"] as? String,
                        email = data["email"] as? String,
                        subscriptionStatus = data["subscriptionStatus"] as? String ?: "NONE",
                        subscriptionExpiry = (data["subscriptionExpiry"] as? Number)?.toLong() ?: 0L,
                        currentAppliedWallpaperId = data["currentAppliedWallpaperId"] as? String,
                        retainedPremiumWallpaper = retained?.let {
                            RetainedEntitlement(
                                wallpaperId = it["wallpaperId"] as? String ?: "",
                                subscriptionId = it["subscriptionId"] as? String ?: "",
                                appliedAtTimestamp = (it["appliedAtTimestamp"] as? Number)?.toLong() ?: 0L,
                                expiryTimestamp = (it["expiryTimestamp"] as? Number)?.toLong() ?: 0L
                            )
                        }
                    )
                } else {
                    // NOTE: firestore.rules must only allow this create when subscriptionStatus == "NONE"
                    val newProfile = mapOf(
                        "subscriptionStatus" to "NONE",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(newProfile).await()
                    _userProfile.value = UserProfile(uid, "User", null, "NONE", 0L, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // No backend call anymore — writes straight to Firestore. Firestore Security
    // Rules are what actually stop a free user from applying a premium wallpaper
    // (see the updated rules: a write to currentAppliedWallpaperId is only allowed
    // if the target wallpaper's isPremium == false, or the user's own
    // subscriptionStatus == "ACTIVE"). If the rule rejects it, Firestore throws a
    // PERMISSION_DENIED exception which we surface as a Result.failure.
    override suspend fun applyWallpaper(uid: String, wallpaperId: String, isPremium: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                firestore.collection("users").document(uid)
                    .update("currentAppliedWallpaperId", wallpaperId)
                    .await()
                refreshUserProfile(uid)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
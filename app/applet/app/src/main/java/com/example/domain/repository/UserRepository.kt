package com.example.domain.repository

import com.example.domain.models.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(uid: String): Flow<UserProfile?>
    suspend fun refreshUserProfile(uid: String)
    suspend fun applyWallpaper(uid: String, wallpaperId: String, isPremium: Boolean): Result<Unit>
}

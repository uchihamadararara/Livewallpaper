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
                            val rawExpType = (doc["live_experience_type"] as? String)
                                ?: (advancedMap?.get("live_experience_type") as? String)
                            val expType = if (rawExpType == "TRANSITION") {
                                com.example.domain.models.LiveExperienceType.TRANSITION
                            } else {
                                com.example.domain.models.LiveExperienceType.NORMAL
                            }
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
                                advancedConfig = if (advancedMap != null || expType == com.example.domain.models.LiveExperienceType.TRANSITION) {
                                    AdvancedConfig(
                                        liveExperienceType = expType,
                                        lockAnimationEnabled = advancedMap?.get("lockAnimationEnabled") as? Boolean ?: false,
                                        lockAnimationVideoUrl = advancedMap?.get("lockAnimationVideoUrl") as? String,
                                        lockDurationMs = (advancedMap?.get("lockDurationMs") as? Number)?.toLong() ?: 0L,
                                        unlockTransitionEnabled = advancedMap?.get("unlockTransitionEnabled") as? Boolean ?: false,
                                        unlockTransitionVideoUrl = advancedMap?.get("unlockTransitionVideoUrl") as? String,
                                        transitionDurationMs = (advancedMap?.get("transitionDurationMs") as? Number)?.toLong() ?: 0L,
                                        chargingAnimationEnabled = advancedMap?.get("chargingAnimationEnabled") as? Boolean ?: false,
                                        chargingAnimationVideoUrl = advancedMap?.get("chargingAnimationVideoUrl") as? String,
                                        chargingDurationMs = (advancedMap?.get("chargingDurationMs") as? Number)?.toLong() ?: 0L,
                                        chargingReturnAnimationVideoUrl = advancedMap?.get("chargingReturnAnimationVideoUrl") as? String,
                                        chargingReturnDurationMs = (advancedMap?.get("chargingReturnDurationMs") as? Number)?.toLong() ?: 0L,
                                        restartOnScreenOn = advancedMap?.get("restartOnScreenOn") as? Boolean ?: true,
                                        loopMainVideo = advancedMap?.get("loopMainVideo") as? Boolean ?: true,
                                        stopWhenScreenOff = advancedMap?.get("stopWhenScreenOff") as? Boolean ?: true
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

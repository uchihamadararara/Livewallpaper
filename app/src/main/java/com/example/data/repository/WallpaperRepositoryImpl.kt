package com.example.data.repository

import com.example.data.local.WallpaperDao
import com.example.domain.models.AdvancedConfig
import com.example.domain.models.LiveExperienceType
import com.example.domain.models.Wallpaper
import com.example.domain.repository.WallpaperRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WallpaperRepositoryImpl(
    private val dao: WallpaperDao,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepositoryImpl
) : WallpaperRepository {

    override fun getWallpapers(): Flow<List<Wallpaper>> = dao.getAllWallpapers()

    override suspend fun refreshWallpapers(limit: Int) {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("wallpapers")
                    .whereEqualTo("isActive", true)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()

                val wallpapers = snapshot.documents.mapNotNull { doc ->
                    try {
                        val advancedMap = doc.get("advancedConfig") as? Map<*, *>
                        val existing = dao.getWallpaperByIdSync(doc.id)
                        val rawExpType = advancedMap?.get("liveExperienceType") as? String
                        val expType = if (rawExpType == "TRANSITION") {
                            LiveExperienceType.TRANSITION
                        } else {
                            LiveExperienceType.NORMAL
                        }

                        Wallpaper(
                            id = doc.id,
                            title = doc.getString("title") ?: "Untitled",
                            description = doc.getString("description"),
                            type = doc.getString("type") ?: "STATIC",
                            imageUrl = doc.getString("imageUrl"),
                            videoUrl = doc.getString("videoUrl"),
                            thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                            isPremium = doc.getBoolean("isPremium") ?: false,
                            isTrending = doc.getBoolean("isTrending") ?: false,
                            isNew = doc.getBoolean("isNew") ?: false,
                            isFeatured = doc.getBoolean("isFeatured") ?: false,
                            soundAvailable = doc.getBoolean("soundAvailable") ?: false,
                            advancedConfig = if (advancedMap != null) {
                                AdvancedConfig(
                                    liveExperienceType = expType,
                                    lockAnimationEnabled = advancedMap["lockAnimationEnabled"] as? Boolean ?: false,
                                    lockAnimationVideoUrl = advancedMap["lockAnimationVideoUrl"] as? String,
                                    lockDurationMs = (advancedMap["lockDurationMs"] as? Number)?.toLong() ?: 0L,
                                    unlockTransitionEnabled = advancedMap["unlockTransitionEnabled"] as? Boolean ?: false,
                                    unlockTransitionVideoUrl = advancedMap["unlockTransitionVideoUrl"] as? String,
                                    transitionDurationMs = (advancedMap["transitionDurationMs"] as? Number)?.toLong() ?: 0L,
                                    chargingAnimationEnabled = advancedMap["chargingAnimationEnabled"] as? Boolean ?: false,
                                    chargingAnimationVideoUrl = advancedMap["chargingAnimationVideoUrl"] as? String,
                                    chargingDurationMs = (advancedMap["chargingDurationMs"] as? Number)?.toLong() ?: 0L,
                                    chargingReturnAnimationVideoUrl = advancedMap["chargingReturnAnimationVideoUrl"] as? String,
                                    chargingReturnDurationMs = (advancedMap["chargingReturnDurationMs"] as? Number)?.toLong() ?: 0L,
                                    restartOnScreenOn = advancedMap["restartOnScreenOn"] as? Boolean ?: true,
                                    loopMainVideo = advancedMap["loopMainVideo"] as? Boolean ?: true,
                                    stopWhenScreenOff = advancedMap["stopWhenScreenOff"] as? Boolean ?: true
                                )
                            } else null,
                            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time
                                ?: System.currentTimeMillis(),
                            categoryIds = (doc.get("categoryIds") as? List<*>)?.filterIsInstance<String>()
                                ?: emptyList(),
                            isFavorite = existing?.isFavorite ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                if (wallpapers.isNotEmpty()) dao.insertWallpapers(wallpapers)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun getWallpaper(id: String): Flow<Wallpaper?> = dao.getWallpaperById(id)

    // No signed-URL backend anymore — wallpaper docs store the direct Cloudflare
    // URL already (imageUrl/videoUrl), cached locally via Room. So this just
    // reads what we already have instead of calling out to a function.
    override suspend fun getPremiumMediaUrl(wallpaperId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            val wallpaper = dao.getWallpaperByIdSync(wallpaperId)
            val url = wallpaper?.videoUrl ?: wallpaper?.imageUrl
            if (url != null) Result.success(url) else Result.failure(Exception("Wallpaper URL not found"))
        }
    }

    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        withContext(Dispatchers.IO) { dao.updateFavoriteStatus(id, isFavorite) }
    }

    override fun getFavoriteWallpapers(): Flow<List<Wallpaper>> = dao.getFavoriteWallpapers()
}
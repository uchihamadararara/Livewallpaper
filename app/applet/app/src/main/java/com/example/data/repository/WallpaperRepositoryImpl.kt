package com.example.data.repository

import com.example.data.local.WallpaperDao
import com.example.domain.models.AdvancedConfig
import com.example.domain.models.Wallpaper
import com.example.domain.repository.WallpaperRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class WallpaperRepositoryImpl(
    private val dao: WallpaperDao,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : WallpaperRepository {

    override fun getWallpapers(): Flow<List<Wallpaper>> {
        return dao.getAllWallpapers()
    }

    override suspend fun refreshWallpapers(limit: Int) {
        withContext(Dispatchers.IO) {
            // ... (keep the try catch inner block same but throw error instead of silent catch)
            val snapshot = firestore.collection("wallpapers")
                .whereEqualTo("isActive", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val wallpapers = snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val title = doc.getString("title") ?: "Untitled"
                    val desc = doc.getString("description")
                    val type = doc.getString("type") ?: "STATIC"
                    val imageUrl = doc.getString("imageUrl")
                    val videoUrl = doc.getString("videoUrl")
                    val thumbnailUrl = doc.getString("thumbnailUrl") ?: ""
                    val isPremium = doc.getBoolean("isPremium") ?: false
                    val isTrending = doc.getBoolean("isTrending") ?: false
                    val isNew = doc.getBoolean("isNew") ?: false
                    val isFeatured = doc.getBoolean("isFeatured") ?: false
                    val soundAvailable = doc.getBoolean("soundAvailable") ?: false
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                    val advancedMap = doc.get("advancedConfig") as? Map<*, *>
                    val advancedConfig = if (advancedMap != null) {
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
                    } else null

                    Wallpaper(
                        id = id,
                        title = title,
                        description = desc,
                        type = type,
                        imageUrl = imageUrl,
                        videoUrl = videoUrl,
                        thumbnailUrl = thumbnailUrl,
                        isPremium = isPremium,
                        isTrending = isTrending,
                        isNew = isNew,
                        isFeatured = isFeatured,
                        soundAvailable = soundAvailable,
                        advancedConfig = advancedConfig,
                        createdAt = createdAt
                    )
                } catch (e: Exception) {
                    null // Skip corrupted documents
                }
            }

            if (wallpapers.isNotEmpty()) {
                dao.insertWallpapers(wallpapers)
            }
        }
    }

    override fun getWallpaper(id: String): Flow<Wallpaper?> {
        return dao.getWallpaperById(id)
    }

    override suspend fun getPremiumMediaUrl(wallpaperId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val data = hashMapOf("wallpaperId" to wallpaperId)
                val result = functions
                    .getHttpsCallable("getPremiumMediaUrl")
                    .call(data)
                    .await()
                
                val url = (result.data as? Map<*, *>)?.get("url") as? String
                if (url != null) {
                    Result.success(url)
                } else {
                    Result.failure(Exception("Invalid response from server"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

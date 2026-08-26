package com.example.domain.repository

import com.example.domain.models.Wallpaper
import kotlinx.coroutines.flow.Flow

interface WallpaperRepository {
    fun getWallpapers(): Flow<List<Wallpaper>>
    suspend fun refreshWallpapers(limit: Int = 20)
    fun getWallpaper(id: String): Flow<Wallpaper?>
    suspend fun getPremiumMediaUrl(wallpaperId: String): Result<String>
    suspend fun toggleFavorite(id: String, isFavorite: Boolean)
    fun getFavoriteWallpapers(): Flow<List<Wallpaper>>
}

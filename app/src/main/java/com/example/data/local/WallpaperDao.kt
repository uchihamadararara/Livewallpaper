package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.domain.models.Wallpaper
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {
    @Query("SELECT * FROM wallpapers ORDER BY createdAt DESC")
    fun getAllWallpapers(): Flow<List<Wallpaper>>
    
    @Query("SELECT * FROM wallpapers ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getWallpapersPaged(limit: Int, offset: Int): List<Wallpaper>

    @Query("SELECT * FROM wallpapers WHERE id = :id")
    fun getWallpaperById(id: String): Flow<Wallpaper?>
    
    @Query("SELECT * FROM wallpapers WHERE id = :id")
    suspend fun getWallpaperByIdSync(id: String): Wallpaper?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpapers(wallpapers: List<Wallpaper>)

    @Query("DELETE FROM wallpapers")
    suspend fun clearAll()

    @Query("UPDATE wallpapers SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    @Query("SELECT * FROM wallpapers WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteWallpapers(): Flow<List<Wallpaper>>
}

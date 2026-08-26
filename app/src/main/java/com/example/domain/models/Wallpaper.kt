package com.example.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallpapers")
data class Wallpaper(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val type: String,
    val imageUrl: String?,
    val videoUrl: String?,
    val thumbnailUrl: String,
    val isPremium: Boolean,
    val isTrending: Boolean,
    val isNew: Boolean,
    val isFeatured: Boolean = false,
    val soundAvailable: Boolean,
    val advancedConfig: AdvancedConfig? = null,
    val createdAt: Long,
    val categoryIds: List<String> = emptyList(),
    val isFavorite: Boolean = false
) {
    val hasChargingAnimation: Boolean
        get() = advancedConfig?.chargingAnimationEnabled == true || !advancedConfig?.chargingAnimationVideoUrl.isNullOrEmpty()

    val hasHomeTransition: Boolean
        get() = advancedConfig?.unlockTransitionEnabled == true || !advancedConfig?.unlockTransitionVideoUrl.isNullOrEmpty()

    val hasLockAnimation: Boolean
        get() = advancedConfig?.lockAnimationEnabled == true || !advancedConfig?.lockAnimationVideoUrl.isNullOrEmpty()
}

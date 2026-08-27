package com.example.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class LiveExperienceType {
    NORMAL,
    TRANSITION
}

@Serializable
data class LiveWallpaperManifest(
    val wallpaperId: String,
    val liveExperienceType: LiveExperienceType = LiveExperienceType.NORMAL,
    val soundAvailable: Boolean = false,
    val primaryVideoFile: String? = null,
    val lockVideoFile: String? = null,
    val transitionVideoFile: String? = null,
    val chargingVideoFile: String? = null,
    val chargingReturnVideoFile: String? = null,
    val primaryDurationMs: Long = 0L,
    val lockDurationMs: Long = 0L,
    val transitionDurationMs: Long = 0L,
    val chargingDurationMs: Long = 0L,
    val chargingReturnDurationMs: Long = 0L
)

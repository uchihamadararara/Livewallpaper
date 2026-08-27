package com.example.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class AdvancedConfig(
    val liveExperienceType: LiveExperienceType = LiveExperienceType.NORMAL,
    val lockAnimationEnabled: Boolean = false,
    val lockAnimationVideoUrl: String? = null,
    val lockDurationMs: Long = 0L,
    val unlockTransitionEnabled: Boolean = false,
    val unlockTransitionVideoUrl: String? = null,
    val transitionDurationMs: Long = 0L,
    val chargingAnimationEnabled: Boolean = false,
    val chargingAnimationVideoUrl: String? = null,
    val chargingDurationMs: Long = 0L,
    val chargingReturnAnimationVideoUrl: String? = null,
    val chargingReturnDurationMs: Long = 0L,
    val restartOnScreenOn: Boolean = true,
    val loopMainVideo: Boolean = true,
    val stopWhenScreenOff: Boolean = true
)


package com.example.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class AdvancedConfig(
    val lockAnimationEnabled: Boolean = false,
    val lockAnimationVideoUrl: String? = null,
    val unlockTransitionEnabled: Boolean = false,
    val unlockTransitionVideoUrl: String? = null,
    val chargingAnimationEnabled: Boolean = false,
    val chargingAnimationVideoUrl: String? = null,
    val restartOnScreenOn: Boolean = true,
    val loopMainVideo: Boolean = true,
    val stopWhenScreenOff: Boolean = true
)

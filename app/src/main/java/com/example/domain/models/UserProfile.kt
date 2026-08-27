package com.example.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class RetainedEntitlement(
    val wallpaperId: String,
    val subscriptionId: String,
    val appliedAtTimestamp: Long,
    val expiryTimestamp: Long
)

@Serializable
data class UserProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val subscriptionStatus: String, // e.g., "ACTIVE", "EXPIRED", "NONE"
    val subscriptionExpiry: Long,
    val currentAppliedWallpaperId: String?,
    val retainedPremiumWallpaper: RetainedEntitlement? = null
)

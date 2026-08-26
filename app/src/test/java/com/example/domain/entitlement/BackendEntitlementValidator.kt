package com.example.domain.entitlement

import com.example.domain.models.RetainedEntitlement
import com.example.domain.models.UserProfile

/**
 * Simulates backend authorization logic for testing the Entitlement Rules.
 * In production, THIS LOGIC RUNS ON THE BACKEND (Cloud Functions) and the client 
 * just receives the resulting UserProfile. 
 * This class proves the architecture works as requested.
 */
class BackendEntitlementValidator {

    data class ApplyRequest(
        val userProfile: UserProfile,
        val requestedWallpaperId: String,
        val isWallpaperPremium: Boolean,
        val currentTime: Long
    )

    sealed class ApplyResult {
        data class Success(val updatedProfile: UserProfile) : ApplyResult()
        object RequiresSubscription : ApplyResult()
    }

    fun handleApplyRequest(request: ApplyRequest): ApplyResult {
        val hasActiveSubscription = request.userProfile.subscriptionStatus == "ACTIVE" && 
                                    request.currentTime < request.userProfile.subscriptionExpiry

        if (hasActiveSubscription) {
            // Rule: Active Sub -> Can apply anything. Track currently applied.
            val updatedProfile = request.userProfile.copy(
                currentAppliedWallpaperId = request.requestedWallpaperId
            )
            return ApplyResult.Success(updatedProfile)
        }

        // Subscription is expired or NONE.
        if (!request.isWallpaperPremium) {
            // Rule: Applying a FREE wallpaper while expired wipes the retained entitlement.
            val updatedProfile = request.userProfile.copy(
                currentAppliedWallpaperId = request.requestedWallpaperId,
                retainedPremiumWallpaper = null // Invalidate atomically
            )
            return ApplyResult.Success(updatedProfile)
        }

        // Attempting to apply a PREMIUM wallpaper while expired.
        val retained = request.userProfile.retainedPremiumWallpaper
        
        if (retained != null && retained.wallpaperId == request.requestedWallpaperId) {
            // Rule: The requested wallpaper IS the exact retained wallpaper. Allow it.
            val updatedProfile = request.userProfile.copy(
                currentAppliedWallpaperId = request.requestedWallpaperId
            )
            return ApplyResult.Success(updatedProfile)
        }

        // Rule: Attempting to apply a DIFFERENT premium wallpaper, or no retained entitlement exists.
        return ApplyResult.RequiresSubscription
    }

    /**
     * Simulates the cron job or trigger that runs when a subscription officially expires.
     */
    fun processSubscriptionExpiry(userProfile: UserProfile, currentTime: Long): UserProfile {
        if (currentTime < userProfile.subscriptionExpiry) return userProfile // Not expired yet

        // If already processed or no wallpaper applied, do nothing
        if (userProfile.subscriptionStatus == "EXPIRED" || userProfile.currentAppliedWallpaperId == null) {
            return userProfile.copy(subscriptionStatus = "EXPIRED")
        }

        // Convert current applied to retained entitlement (assuming it's premium for this simulation)
        val retained = RetainedEntitlement(
            wallpaperId = userProfile.currentAppliedWallpaperId,
            subscriptionId = "sub_${System.currentTimeMillis()}", // Mock
            appliedAtTimestamp = currentTime,
            expiryTimestamp = userProfile.subscriptionExpiry
        )

        return userProfile.copy(
            subscriptionStatus = "EXPIRED",
            retainedPremiumWallpaper = retained
        )
    }
}

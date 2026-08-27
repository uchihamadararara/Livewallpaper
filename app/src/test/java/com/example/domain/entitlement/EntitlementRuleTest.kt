package com.example.domain.entitlement

import com.example.domain.models.RetainedEntitlement
import com.example.domain.models.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementRuleTest {

    private val validator = BackendEntitlementValidator()
    private val currentTime = 1000L

    @Test
    fun `Active subscription allows applying premium wallpaper`() {
        val user = UserProfile("uid1", "User", null, "ACTIVE", 2000L, null, null)
        val request = BackendEntitlementValidator.ApplyRequest(user, "premium_wp_1", true, currentTime)
        
        val result = validator.handleApplyRequest(request)
        
        assertTrue(result is BackendEntitlementValidator.ApplyResult.Success)
        val updatedUser = (result as BackendEntitlementValidator.ApplyResult.Success).updatedProfile
        assertEquals("premium_wp_1", updatedUser.currentAppliedWallpaperId)
    }

    @Test
    fun `Expired subscription retains exact currently applied wallpaper`() {
        // Setup: User had active sub, applied wallpaper, then time passed.
        val user = UserProfile("uid1", "User", null, "ACTIVE", 900L, "premium_wp_1", null)
        
        // Cron job processes expiry
        val expiredUser = validator.processSubscriptionExpiry(user, currentTime)
        
        assertEquals("EXPIRED", expiredUser.subscriptionStatus)
        assertEquals("premium_wp_1", expiredUser.retainedPremiumWallpaper?.wallpaperId)

        // Attempt to re-apply/verify the exact retained wallpaper
        val request = BackendEntitlementValidator.ApplyRequest(expiredUser, "premium_wp_1", true, currentTime)
        val result = validator.handleApplyRequest(request)
        
        assertTrue(result is BackendEntitlementValidator.ApplyResult.Success)
    }

    @Test
    fun `Expired subscription rejects applying a DIFFERENT premium wallpaper`() {
        val retained = RetainedEntitlement("premium_wp_1", "sub1", 500L, 900L)
        val user = UserProfile("uid1", "User", null, "EXPIRED", 900L, "premium_wp_1", retained)
        
        // Attempt to apply a different premium wallpaper
        val request = BackendEntitlementValidator.ApplyRequest(user, "premium_wp_2", true, currentTime)
        val result = validator.handleApplyRequest(request)
        
        assertTrue(result is BackendEntitlementValidator.ApplyResult.RequiresSubscription)
    }

    @Test
    fun `Applying a free wallpaper after expiry atomicaly wipes retained premium entitlement`() {
        val retained = RetainedEntitlement("premium_wp_1", "sub1", 500L, 900L)
        val user = UserProfile("uid1", "User", null, "EXPIRED", 900L, "premium_wp_1", retained)
        
        // Attempt to apply a FREE wallpaper
        val request = BackendEntitlementValidator.ApplyRequest(user, "free_wp_1", false, currentTime)
        val result = validator.handleApplyRequest(request)
        
        assertTrue(result is BackendEntitlementValidator.ApplyResult.Success)
        val updatedUser = (result as BackendEntitlementValidator.ApplyResult.Success).updatedProfile
        
        // Retained entitlement MUST be null now
        assertEquals(null, updatedUser.retainedPremiumWallpaper)
        assertEquals("free_wp_1", updatedUser.currentAppliedWallpaperId)
    }
}

package com.example.domain.state

import com.example.domain.models.AdvancedConfig
import com.example.domain.models.LiveExperienceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperStateMachineTest {

    private val fullTransitionConfig = AdvancedConfig(
        liveExperienceType = LiveExperienceType.TRANSITION,
        lockAnimationEnabled = true,
        unlockTransitionEnabled = true,
        chargingAnimationEnabled = true,
        chargingAnimationVideoUrl = "https://example.com/charging.mp4",
        chargingReturnAnimationVideoUrl = "https://example.com/charging_return.mp4",
        stopWhenScreenOff = true,
        restartOnScreenOn = true
    )

    private val minimalConfig = AdvancedConfig(
        liveExperienceType = LiveExperienceType.NORMAL,
        lockAnimationEnabled = false,
        unlockTransitionEnabled = false,
        chargingAnimationEnabled = false,
        stopWhenScreenOff = true
    )

    @Test
    fun `Test A - Screen OFF to Screen ON triggers Lock Screen restart when keyguard is locked`() {
        val machine = WallpaperStateMachine(fullTransitionConfig)
        machine.setKeyguardLocked(true)
        
        // Initial state is ScreenOff
        val actions = machine.processEvent(InputEvent.ScreenOn)
        
        assertEquals(ScreenState.LockScreen, machine.currentState)
        assertTrue(actions.contains(WallpaperAction.PlayLockScreen(startFromBeginning = true)))
    }

    @Test
    fun `Test B - Screen ON to Screen OFF pauses and mutes`() {
        val machine = WallpaperStateMachine(fullTransitionConfig)
        machine.setKeyguardLocked(true)
        machine.processEvent(InputEvent.ScreenOn)
        
        val actions = machine.processEvent(InputEvent.ScreenOff)
        
        assertEquals(ScreenState.ScreenOff, machine.currentState)
        assertTrue(actions.contains(WallpaperAction.Mute))
        assertTrue(actions.contains(WallpaperAction.Pause))
    }

    @Test
    fun `Test C - Repeated Screen ON restarts animation every time`() {
        val machine = WallpaperStateMachine(fullTransitionConfig)
        machine.setKeyguardLocked(true)
        
        machine.processEvent(InputEvent.ScreenOn)
        machine.processEvent(InputEvent.ScreenOff)
        
        val actions = machine.processEvent(InputEvent.ScreenOn)
        
        assertEquals(ScreenState.LockScreen, machine.currentState)
        assertTrue(actions.contains(WallpaperAction.PlayLockScreen(startFromBeginning = true)))
    }

    @Test
    fun `Test D - Unlock triggers transition once, then Home`() {
        val machine = WallpaperStateMachine(fullTransitionConfig)
        machine.setKeyguardLocked(true)
        machine.processEvent(InputEvent.ScreenOn)
        
        val unlockActions = machine.processEvent(InputEvent.UserUnlocked)
        assertEquals(ScreenState.Transitioning, machine.currentState)
        assertTrue(unlockActions.contains(WallpaperAction.PlayTransition))

        val finishActions = machine.processEvent(InputEvent.TransitionFinished)
        assertEquals(ScreenState.HomeScreen, machine.currentState)
        assertTrue(finishActions.contains(WallpaperAction.PlayHome))
    }

    @Test
    fun `Test D (No Transition) - Unlock switches directly to Home`() {
        val machine = WallpaperStateMachine(minimalConfig)
        machine.setKeyguardLocked(true)
        machine.processEvent(InputEvent.ScreenOn)
        
        val unlockActions = machine.processEvent(InputEvent.UserUnlocked)
        assertEquals(ScreenState.HomeScreen, machine.currentState)
        assertTrue(unlockActions.contains(WallpaperAction.PlayHome))
    }

    @Test
    fun `Test E - Charging from Home triggers HomeToCharging and PlayChargingEntry, then loops`() {
        val machine = WallpaperStateMachine(fullTransitionConfig)
        machine.setKeyguardLocked(false)
        machine.processEvent(InputEvent.ScreenOn)
        
        val entryActions = machine.processEvent(InputEvent.PowerConnected)
        assertEquals(ScreenState.HomeToCharging, machine.currentState)
        assertTrue(entryActions.contains(WallpaperAction.PlayChargingEntry))

        val loopActions = machine.processEvent(InputEvent.ChargingEntryFinished)
        assertEquals(ScreenState.ChargingLoop, machine.currentState)
        assertTrue(loopActions.contains(WallpaperAction.PlayChargingLoop))
    }

    @Test
    fun `Test E - Charging from Lock triggers LockToCharging and PlayChargingEntry, then loops`() {
        val machine = WallpaperStateMachine(fullTransitionConfig)
        machine.setKeyguardLocked(true)
        machine.processEvent(InputEvent.ScreenOn)
        
        val entryActions = machine.processEvent(InputEvent.PowerConnected)
        assertEquals(ScreenState.LockToCharging, machine.currentState)
        assertTrue(entryActions.contains(WallpaperAction.PlayChargingEntry))

        val loopActions = machine.processEvent(InputEvent.ChargingEntryFinished)
        assertEquals(ScreenState.ChargingLoop, machine.currentState)
        assertTrue(loopActions.contains(WallpaperAction.PlayChargingLoop))
    }

    @Test
    fun `Test E (Disconnection) - Power disconnected triggers ChargingReturn`() {
        val machine = WallpaperStateMachine(fullTransitionConfig)
        machine.setKeyguardLocked(false)
        machine.processEvent(InputEvent.ScreenOn)
        machine.processEvent(InputEvent.PowerConnected)
        machine.processEvent(InputEvent.ChargingEntryFinished)

        val disconnectActions = machine.processEvent(InputEvent.PowerDisconnected)
        assertEquals(ScreenState.ChargingReturnToHome, machine.currentState)
        assertTrue(disconnectActions.contains(WallpaperAction.PlayChargingReturnToHome))

        val returnFinishActions = machine.processEvent(InputEvent.ChargingReturnFinished)
        assertEquals(ScreenState.HomeScreen, machine.currentState)
        assertTrue(returnFinishActions.contains(WallpaperAction.PlayHome))
    }

    @Test
    fun `Test E (No Charging Animation) - Charging does not override`() {
        val machine = WallpaperStateMachine(minimalConfig)
        machine.setKeyguardLocked(true)
        machine.processEvent(InputEvent.ScreenOn)
        
        val chargingActions = machine.processEvent(InputEvent.PowerConnected)
        // State remains LockScreen (or Home) because there is no charging animation
        assertEquals(ScreenState.LockScreen, machine.currentState)
        assertTrue(chargingActions.isEmpty()) // No specific charging actions
    }

    @Test
    fun `Test F - Screen OFF while charging mutes and pauses`() {
        val machine = WallpaperStateMachine(fullTransitionConfig)
        machine.setKeyguardLocked(false)
        machine.processEvent(InputEvent.ScreenOn)
        machine.processEvent(InputEvent.PowerConnected)
        
        val actions = machine.processEvent(InputEvent.ScreenOff)
        assertEquals(ScreenState.ScreenOff, machine.currentState)
        assertTrue(actions.contains(WallpaperAction.Mute))
        assertTrue(actions.contains(WallpaperAction.Pause))
    }
}


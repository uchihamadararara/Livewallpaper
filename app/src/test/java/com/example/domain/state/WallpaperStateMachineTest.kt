package com.example.domain.state

import com.example.domain.models.AdvancedConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperStateMachineTest {

    private val fullConfig = AdvancedConfig(
        lockAnimationEnabled = true,
        unlockTransitionEnabled = true,
        chargingAnimationEnabled = true,
        stopWhenScreenOff = true
    )

    private val minimalConfig = AdvancedConfig(
        lockAnimationEnabled = false,
        unlockTransitionEnabled = false,
        chargingAnimationEnabled = false,
        stopWhenScreenOff = true
    )

    @Test
    fun `Test A - Screen OFF to Screen ON triggers Lock Screen restart`() {
        val machine = WallpaperStateMachine(fullConfig)
        
        // Initial state is ScreenOff
        val actions = machine.processEvent(InputEvent.ScreenOn)
        
        assertEquals(ScreenState.LockScreen, machine.currentState)
        assertTrue(actions.contains(WallpaperAction.PlayLockScreen(startFromBeginning = true)))
    }

    @Test
    fun `Test B - Screen ON to Screen OFF pauses and mutes`() {
        val machine = WallpaperStateMachine(fullConfig)
        machine.processEvent(InputEvent.ScreenOn)
        
        val actions = machine.processEvent(InputEvent.ScreenOff)
        
        assertEquals(ScreenState.ScreenOff, machine.currentState)
        assertTrue(actions.contains(WallpaperAction.Mute))
        assertTrue(actions.contains(WallpaperAction.Pause))
    }

    @Test
    fun `Test C - Repeated Screen ON restarts animation every time`() {
        val machine = WallpaperStateMachine(fullConfig)
        
        machine.processEvent(InputEvent.ScreenOn)
        machine.processEvent(InputEvent.ScreenOff)
        
        val actions = machine.processEvent(InputEvent.ScreenOn)
        
        assertEquals(ScreenState.LockScreen, machine.currentState)
        assertTrue(actions.contains(WallpaperAction.PlayLockScreen(startFromBeginning = true)))
    }

    @Test
    fun `Test D - Unlock triggers transition once, then Home`() {
        val machine = WallpaperStateMachine(fullConfig)
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
        machine.processEvent(InputEvent.ScreenOn)
        
        val unlockActions = machine.processEvent(InputEvent.UserUnlocked)
        assertEquals(ScreenState.HomeScreen, machine.currentState)
        assertTrue(unlockActions.contains(WallpaperAction.PlayHome))
    }

    @Test
    fun `Test E - Charging overrides active screen`() {
        val machine = WallpaperStateMachine(fullConfig)
        machine.processEvent(InputEvent.ScreenOn)
        
        val chargingActions = machine.processEvent(InputEvent.PowerConnected)
        assertEquals(ScreenState.Charging, machine.currentState)
        assertTrue(chargingActions.contains(WallpaperAction.PlayCharging))
    }

    @Test
    fun `Test E (No Charging Animation) - Charging does not override`() {
        val machine = WallpaperStateMachine(minimalConfig)
        machine.processEvent(InputEvent.ScreenOn)
        
        val chargingActions = machine.processEvent(InputEvent.PowerConnected)
        // State remains LockScreen (or Home) because there is no charging animation
        assertEquals(ScreenState.LockScreen, machine.currentState)
        assertTrue(chargingActions.isEmpty()) // No specific charging actions
    }

    @Test
    fun `Test F - Screen OFF while charging mutes and pauses`() {
        val machine = WallpaperStateMachine(fullConfig)
        machine.processEvent(InputEvent.ScreenOn)
        machine.processEvent(InputEvent.PowerConnected)
        
        val actions = machine.processEvent(InputEvent.ScreenOff)
        assertEquals(ScreenState.ScreenOff, machine.currentState)
        assertTrue(actions.contains(WallpaperAction.Mute))
        assertTrue(actions.contains(WallpaperAction.Pause))
    }
}

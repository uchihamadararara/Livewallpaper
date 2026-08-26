package com.example.domain.state

import com.example.domain.models.AdvancedConfig

sealed class ScreenState {
    object ScreenOff : ScreenState()
    object LockScreen : ScreenState()
    object Transitioning : ScreenState()
    object HomeScreen : ScreenState()
    object Charging : ScreenState()
}

sealed class WallpaperAction {
    object Pause : WallpaperAction()
    object Mute : WallpaperAction()
    data class PlayLockScreen(val startFromBeginning: Boolean) : WallpaperAction()
    object PlayTransition : WallpaperAction()
    object PlayHome : WallpaperAction()
    object PlayCharging : WallpaperAction()
}

sealed class InputEvent {
    object ScreenOn : InputEvent()
    object ScreenOff : InputEvent()
    object UserUnlocked : InputEvent() // Represents ACTION_USER_PRESENT
    object PowerConnected : InputEvent()
    object PowerDisconnected : InputEvent()
    object TransitionFinished : InputEvent()
}

/**
 * A strict state machine to govern the WallpaperService lifecycle.
 * Isolates the logic from the Android framework for 100% testability.
 */
class WallpaperStateMachine(
    val config: AdvancedConfig
) {
    var currentState: ScreenState = ScreenState.ScreenOff
        private set

    var isScreenOn: Boolean = false
        private set

    var isCharging: Boolean = false
        private set

    fun processEvent(event: InputEvent): List<WallpaperAction> {
        val actions = mutableListOf<WallpaperAction>()

        when (event) {
            is InputEvent.ScreenOff -> {
                isScreenOn = false
                currentState = ScreenState.ScreenOff
                actions.add(WallpaperAction.Mute)
                if (config.stopWhenScreenOff) {
                    actions.add(WallpaperAction.Pause)
                }
            }

            is InputEvent.ScreenOn -> {
                isScreenOn = true
                
                if (isCharging && config.chargingAnimationEnabled) {
                    currentState = ScreenState.Charging
                    actions.add(WallpaperAction.PlayCharging)
                } else if (currentState == ScreenState.ScreenOff || currentState == ScreenState.LockScreen) {
                    // Always restart Lock Screen animation from the beginning when screen turns on
                    currentState = ScreenState.LockScreen
                    if (config.lockAnimationEnabled) {
                        actions.add(WallpaperAction.PlayLockScreen(startFromBeginning = true))
                    } else {
                        actions.add(WallpaperAction.PlayHome) // Fallback if no lock animation
                    }
                } else {
                    // E.g. screen turned on but we were already on HomeScreen state (maybe device doesn't use lock screen)
                    actions.add(WallpaperAction.PlayHome)
                }
            }

            is InputEvent.UserUnlocked -> {
                if (isScreenOn) {
                    if (isCharging && config.chargingAnimationEnabled) {
                        // Keep charging animation playing
                        currentState = ScreenState.Charging
                    } else if (config.unlockTransitionEnabled) {
                        currentState = ScreenState.Transitioning
                        actions.add(WallpaperAction.PlayTransition)
                    } else {
                        currentState = ScreenState.HomeScreen
                        actions.add(WallpaperAction.PlayHome)
                    }
                } else {
                    // Unlocked while screen is off (e.g. fast fingerprint). When screen turns on, go straight to Home.
                    currentState = ScreenState.HomeScreen
                }
            }

            is InputEvent.TransitionFinished -> {
                if (currentState == ScreenState.Transitioning) {
                    currentState = ScreenState.HomeScreen
                    actions.add(WallpaperAction.PlayHome)
                }
            }

            is InputEvent.PowerConnected -> {
                isCharging = true
                if (isScreenOn && config.chargingAnimationEnabled) {
                    currentState = ScreenState.Charging
                    actions.add(WallpaperAction.PlayCharging)
                }
            }

            is InputEvent.PowerDisconnected -> {
                isCharging = false
                if (isScreenOn) {
                    // Return to previous logical state (we can't know for sure if locked or home without querying, 
                    // but we assume home if we were charging, unless we have a strict lock state tracking).
                    // For simplicity, fallback to home. Android's visibility callbacks will correct this if needed.
                    currentState = ScreenState.HomeScreen
                    actions.add(WallpaperAction.PlayHome)
                }
            }
        }

        return actions
    }
}

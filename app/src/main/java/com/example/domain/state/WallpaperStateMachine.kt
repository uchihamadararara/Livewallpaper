package com.example.domain.state

import com.example.domain.models.AdvancedConfig
import com.example.domain.models.LiveExperienceType

sealed class ScreenState {
    object ScreenOff : ScreenState()
    object LockScreen : ScreenState()
    object Transitioning : ScreenState()
    object HomeScreen : ScreenState()
    object HomeToCharging : ScreenState()
    object LockToCharging : ScreenState()
    object ChargingLoop : ScreenState()
    object ChargingReturnToHome : ScreenState()
    object ChargingReturnToLock : ScreenState()
}

sealed class WallpaperAction {
    object Pause : WallpaperAction()
    object Mute : WallpaperAction()
    data class PlayLockScreen(val startFromBeginning: Boolean) : WallpaperAction()
    object PlayTransition : WallpaperAction()
    object PlayHome : WallpaperAction()
    object PlayChargingEntry : WallpaperAction()
    object PlayChargingLoop : WallpaperAction()
    object PlayChargingReturnToHome : WallpaperAction()
    object PlayChargingReturnToLock : WallpaperAction()
}

sealed class InputEvent {
    object ScreenOn : InputEvent()
    object ScreenOff : InputEvent()
    object UserUnlocked : InputEvent() // Represents ACTION_USER_PRESENT
    object PowerConnected : InputEvent()
    object PowerDisconnected : InputEvent()
    object TransitionFinished : InputEvent()
    object ChargingEntryFinished : InputEvent()
    object ChargingReturnFinished : InputEvent()
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

    var isKeyguardLocked: Boolean = false
        private set

    fun setKeyguardLocked(locked: Boolean) {
        isKeyguardLocked = locked
    }

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
                    currentState = ScreenState.ChargingLoop
                    actions.add(WallpaperAction.PlayChargingLoop)
                } else if (isKeyguardLocked) {
                    currentState = ScreenState.LockScreen
                    if (config.liveExperienceType == LiveExperienceType.TRANSITION && config.lockAnimationEnabled) {
                        actions.add(WallpaperAction.PlayLockScreen(startFromBeginning = config.restartOnScreenOn))
                    } else {
                        actions.add(WallpaperAction.PlayHome)
                    }
                } else {
                    currentState = ScreenState.HomeScreen
                    actions.add(WallpaperAction.PlayHome)
                }
            }

            is InputEvent.UserUnlocked -> {
                isKeyguardLocked = false
                if (isScreenOn) {
                    if (isCharging && config.chargingAnimationEnabled) {
                        // User unlocked while plugged in: transition charging return to home or stay in charging
                        // Charging loop continues until disconnected
                    } else if (config.liveExperienceType == LiveExperienceType.TRANSITION && config.unlockTransitionEnabled) {
                        currentState = ScreenState.Transitioning
                        actions.add(WallpaperAction.PlayTransition)
                    } else {
                        currentState = ScreenState.HomeScreen
                        actions.add(WallpaperAction.PlayHome)
                    }
                } else {
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
                    if (currentState == ScreenState.LockScreen || isKeyguardLocked) {
                        currentState = ScreenState.LockToCharging
                        actions.add(WallpaperAction.PlayChargingEntry)
                    } else {
                        currentState = ScreenState.HomeToCharging
                        actions.add(WallpaperAction.PlayChargingEntry)
                    }
                }
            }

            is InputEvent.ChargingEntryFinished -> {
                if (currentState == ScreenState.HomeToCharging || currentState == ScreenState.LockToCharging) {
                    currentState = ScreenState.ChargingLoop
                    actions.add(WallpaperAction.PlayChargingLoop)
                }
            }

            is InputEvent.PowerDisconnected -> {
                isCharging = false
                if (isScreenOn) {
                    if (!config.chargingReturnAnimationVideoUrl.isNullOrEmpty()) {
                        if (isKeyguardLocked) {
                            currentState = ScreenState.ChargingReturnToLock
                            actions.add(WallpaperAction.PlayChargingReturnToLock)
                        } else {
                            currentState = ScreenState.ChargingReturnToHome
                            actions.add(WallpaperAction.PlayChargingReturnToHome)
                        }
                    } else {
                        if (isKeyguardLocked) {
                            currentState = ScreenState.LockScreen
                            if (config.liveExperienceType == LiveExperienceType.TRANSITION && config.lockAnimationEnabled) {
                                actions.add(WallpaperAction.PlayLockScreen(startFromBeginning = false))
                            } else {
                                actions.add(WallpaperAction.PlayHome)
                            }
                        } else {
                            currentState = ScreenState.HomeScreen
                            actions.add(WallpaperAction.PlayHome)
                        }
                    }
                }
            }

            is InputEvent.ChargingReturnFinished -> {
                if (currentState == ScreenState.ChargingReturnToLock) {
                    currentState = ScreenState.LockScreen
                    if (config.liveExperienceType == LiveExperienceType.TRANSITION && config.lockAnimationEnabled) {
                        actions.add(WallpaperAction.PlayLockScreen(startFromBeginning = false))
                    } else {
                        actions.add(WallpaperAction.PlayHome)
                    }
                } else if (currentState == ScreenState.ChargingReturnToHome) {
                    currentState = ScreenState.HomeScreen
                    actions.add(WallpaperAction.PlayHome)
                }
            }
        }

        return actions
    }
}


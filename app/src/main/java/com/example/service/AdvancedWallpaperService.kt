package com.example.service

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.LocalWallpaperStorageManager
import com.example.di.AppContainer
import com.example.domain.models.AdvancedConfig
import com.example.domain.models.LiveExperienceType
import com.example.domain.models.LiveWallpaperManifest
import com.example.domain.state.InputEvent
import com.example.domain.state.WallpaperAction
import com.example.domain.state.WallpaperStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

/**
 * The core Android WallpaperService engine.
 *
 * Architecture Highlights:
 * 1. Content-Driven Multi-State: Plays actual Admin-configured local video assets for each state
 *    (primary, lock, transition, charging entry/loop, charging return).
 * 2. Explicit Live Experience Type: Handles NORMAL vs TRANSITION modes accurately.
 * 3. Offline First & Atomic Bundle: Loads exclusively from active bundle directory + manifest.json.
 * 4. Reboot Resilient: Operates independently of Main Activity or network availability.
 * 5. Default-Deny Audio Safety: Audio is strictly muted whenever legitimate wallpaper visibility
 *    cannot be confirmed (screen off, obscured, keyguard changes).
 * 6. Zero Generic Procedural Overlays: All visuals are 100% content-driven from video media assets.
 */
class AdvancedWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return AdvancedEngine()
    }

    inner class AdvancedEngine : Engine() {
        private var player: ExoPlayer? = null
        private var stateMachine: WallpaperStateMachine? = null
        private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

        private var currentConfig: AdvancedConfig? = null
        private var activeManifest: LiveWallpaperManifest? = null
        private var isSoundEnabled = false
        private var currentHolder: SurfaceHolder? = null
        private var currentPlayingState: String = ""

        // Cached local file references from the active bundle
        private var primaryVideoFile: File? = null
        private var lockVideoFile: File? = null
        private var transitionVideoFile: File? = null
        private var chargingVideoFile: File? = null
        private var chargingReturnVideoFile: File? = null

        private val keyguardManager by lazy {
            getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        }
        private val powerManager by lazy {
            getSystemService(Context.POWER_SERVICE) as? PowerManager
        }

        private val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        updateKeyguardState()
                        processEvent(InputEvent.ScreenOn)
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        processEvent(InputEvent.ScreenOff)
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        stateMachine?.setKeyguardLocked(false)
                        processEvent(InputEvent.UserUnlocked)
                    }
                    Intent.ACTION_POWER_CONNECTED -> {
                        processEvent(InputEvent.PowerConnected)
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        processEvent(InputEvent.PowerDisconnected)
                    }
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            currentHolder = surfaceHolder

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            registerReceiver(screenReceiver, filter)

            initializePlayer()
            loadActiveWallpaperBundle()
            observePreferences()
        }

        private fun updateKeyguardState() {
            val isLocked = keyguardManager?.isKeyguardLocked ?: false
            stateMachine?.setKeyguardLocked(isLocked)
        }

        private fun checkIsCharging(): Boolean {
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        }

        private fun observePreferences() {
            val prefs = AppContainer.getUserPreferencesRepository(applicationContext)
            serviceScope.launch {
                prefs.isAppliedWallpaperSoundEnabled.collect { userPref ->
                    val soundAvailable = activeManifest?.soundAvailable ?: prefs.isAppliedWallpaperSoundAvailableSync()
                    isSoundEnabled = userPref && soundAvailable
                    enforceAudioSafety()
                }
            }
        }

        @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
        private fun initializePlayer() {
            val renderersFactory = DefaultRenderersFactory(applicationContext)
                .setEnableDecoderFallback(true)

            player = ExoPlayer.Builder(applicationContext, renderersFactory)
                .build()
                .apply {
                    volume = 0f // Start muted by default (Default-Deny Audio Safety)
                    playWhenReady = true
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                onMediaEnded()
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            // Fallback to looping primary video on decoding error
                            playAsset(primaryVideoFile, loop = true, stateKey = "primary_fallback")
                        }
                    })
                }
        }

        private fun onMediaEnded() {
            when (currentPlayingState) {
                "transition" -> {
                    processEvent(InputEvent.TransitionFinished)
                }
                "charging_entry" -> {
                    processEvent(InputEvent.ChargingEntryFinished)
                }
                "charging_return_home", "charging_return_lock" -> {
                    processEvent(InputEvent.ChargingReturnFinished)
                }
            }
        }

        private fun loadActiveWallpaperBundle() {
            serviceScope.launch {
                val prefs = AppContainer.getUserPreferencesRepository(applicationContext)
                val soundUserPref = prefs.isAppliedWallpaperSoundEnabledSync()
                val manifest = LocalWallpaperStorageManager.getActiveManifest(applicationContext)

                if (manifest != null) {
                    activeManifest = manifest
                    isSoundEnabled = soundUserPref && manifest.soundAvailable

                    primaryVideoFile = LocalWallpaperStorageManager.getActiveAssetFile(applicationContext, manifest.primaryVideoFile)
                    lockVideoFile = LocalWallpaperStorageManager.getActiveAssetFile(applicationContext, manifest.lockVideoFile)
                    transitionVideoFile = LocalWallpaperStorageManager.getActiveAssetFile(applicationContext, manifest.transitionVideoFile)
                    chargingVideoFile = LocalWallpaperStorageManager.getActiveAssetFile(applicationContext, manifest.chargingVideoFile)
                    chargingReturnVideoFile = LocalWallpaperStorageManager.getActiveAssetFile(applicationContext, manifest.chargingReturnVideoFile)

                    currentConfig = AdvancedConfig(
                        liveExperienceType = manifest.liveExperienceType,
                        lockAnimationEnabled = lockVideoFile != null,
                        lockAnimationVideoUrl = manifest.lockVideoFile,
                        lockDurationMs = manifest.lockDurationMs,
                        unlockTransitionEnabled = transitionVideoFile != null,
                        unlockTransitionVideoUrl = manifest.transitionVideoFile,
                        transitionDurationMs = manifest.transitionDurationMs,
                        chargingAnimationEnabled = chargingVideoFile != null,
                        chargingAnimationVideoUrl = manifest.chargingVideoFile,
                        chargingDurationMs = manifest.chargingDurationMs,
                        chargingReturnAnimationVideoUrl = manifest.chargingReturnVideoFile,
                        chargingReturnDurationMs = manifest.chargingReturnDurationMs,
                        loopMainVideo = true,
                        stopWhenScreenOff = true,
                        restartOnScreenOn = true
                    )
                } else {
                    // Fallback for single legacy file if manifest not yet written
                    val legacyFile = LocalWallpaperStorageManager.getActiveLiveWallpaperFile(applicationContext)
                    primaryVideoFile = legacyFile
                    val soundAvailable = prefs.isAppliedWallpaperSoundAvailableSync()
                    val chargingAvailable = prefs.isAppliedWallpaperChargingAnimationAvailableSync()
                    isSoundEnabled = soundUserPref && soundAvailable

                    currentConfig = AdvancedConfig(
                        liveExperienceType = LiveExperienceType.NORMAL,
                        loopMainVideo = true,
                        stopWhenScreenOff = true,
                        restartOnScreenOn = false,
                        chargingAnimationEnabled = chargingAvailable
                    )
                }

                stateMachine = WallpaperStateMachine(currentConfig!!)
                updateKeyguardState()

                if (checkIsCharging()) {
                    processEvent(InputEvent.PowerConnected)
                }

                if (isVisible) {
                    processEvent(InputEvent.ScreenOn)
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            currentHolder = holder
            player?.setVideoSurfaceHolder(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            currentHolder = null
            player?.setVideoSurfaceHolder(null)
            enforceAudioSafety()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                updateKeyguardState()
                if (stateMachine?.isScreenOn == false) {
                    processEvent(InputEvent.ScreenOn)
                } else {
                    player?.play()
                    enforceAudioSafety()
                }
            } else {
                enforceAudioSafety()
                processEvent(InputEvent.ScreenOff)
            }
        }

        private fun enforceAudioSafety() {
            val p = player ?: return
            val isScreenInteractive = powerManager?.isInteractive ?: true
            if (isVisible && isScreenInteractive && isSoundEnabled) {
                p.volume = 1f
            } else {
                p.volume = 0f
            }
        }

        private fun processEvent(event: InputEvent) {
            val sm = stateMachine ?: return
            val actions = sm.processEvent(event)
            actions.forEach { executeAction(it) }
        }

        private fun executeAction(action: WallpaperAction) {
            val holder = currentHolder

            when (action) {
                is WallpaperAction.PlayLockScreen -> {
                    val file = lockVideoFile ?: primaryVideoFile
                    playAsset(file, loop = true, stateKey = "lock", seekToStart = action.startFromBeginning)
                }
                is WallpaperAction.PlayTransition -> {
                    if (transitionVideoFile != null) {
                        playAsset(transitionVideoFile, loop = false, stateKey = "transition", seekToStart = true)
                    } else {
                        playAsset(primaryVideoFile, loop = true, stateKey = "home")
                    }
                }
                is WallpaperAction.PlayHome -> {
                    playAsset(primaryVideoFile, loop = true, stateKey = "home")
                }
                is WallpaperAction.PlayChargingEntry -> {
                    if (chargingVideoFile != null) {
                        playAsset(chargingVideoFile, loop = false, stateKey = "charging_entry", seekToStart = true)
                    } else {
                        playAsset(primaryVideoFile, loop = true, stateKey = "home")
                    }
                }
                is WallpaperAction.PlayChargingLoop -> {
                    if (chargingVideoFile != null) {
                        playAsset(chargingVideoFile, loop = true, stateKey = "charging_loop")
                    } else {
                        playAsset(primaryVideoFile, loop = true, stateKey = "home")
                    }
                }
                is WallpaperAction.PlayChargingReturnToLock -> {
                    if (chargingReturnVideoFile != null) {
                        playAsset(chargingReturnVideoFile, loop = false, stateKey = "charging_return_lock", seekToStart = true)
                    } else {
                        val file = lockVideoFile ?: primaryVideoFile
                        playAsset(file, loop = true, stateKey = "lock")
                    }
                }
                is WallpaperAction.PlayChargingReturnToHome -> {
                    if (chargingReturnVideoFile != null) {
                        playAsset(chargingReturnVideoFile, loop = false, stateKey = "charging_return_home", seekToStart = true)
                    } else {
                        playAsset(primaryVideoFile, loop = true, stateKey = "home")
                    }
                }
                is WallpaperAction.Pause -> {
                    enforceAudioSafety()
                    player?.pause()
                }
                is WallpaperAction.Mute -> {
                    player?.volume = 0f
                }
            }
        }

        private fun playAsset(file: File?, loop: Boolean, stateKey: String, seekToStart: Boolean = false) {
            val p = player ?: return
            val targetFile = file ?: primaryVideoFile ?: return
            if (!targetFile.exists()) return

            currentPlayingState = stateKey

            val currentUri = p.currentMediaItem?.localConfiguration?.uri
            val targetUri = Uri.fromFile(targetFile)

            if (currentUri != targetUri) {
                p.setMediaItem(MediaItem.fromUri(targetUri))
                p.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                p.prepare()
                p.play()
            } else {
                p.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                if (seekToStart) {
                    p.seekTo(0)
                }
                p.play()
            }
            enforceAudioSafety()
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                unregisterReceiver(screenReceiver)
            } catch (_: Exception) {}
            player?.stop()
            player?.release()
            player = null
            serviceScope.cancel()
        }
    }
}


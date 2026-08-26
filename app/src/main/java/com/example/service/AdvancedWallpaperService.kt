package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.LocalWallpaperStorageManager
import com.example.di.AppContainer
import com.example.domain.models.AdvancedConfig
import com.example.domain.state.InputEvent
import com.example.domain.state.WallpaperAction
import com.example.domain.state.WallpaperStateMachine
import com.example.service.charging.WallpaperChargingRenderer
import com.example.util.getCurrentBatteryChargingState
import com.example.util.parseBatteryIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File

/**
 * The core Android WallpaperService engine.
 *
 * Architecture Highlights:
 * 1. Offline First: Loads applied video exclusively from persistent local storage.
 * 2. Reboot Resilient: Operates independently of Main Activity or network availability.
 * 3. Lifecycle & Battery Conscious: Automatically pauses playback and decoders when invisible or screen OFF.
 * 4. Audio Control: Muted by default; respects explicit user sound preferences.
 * 5. Luxury Charging Presentation: Seamlessly cross-fades into the obsidian/gold charging visual
 *    directly on the wallpaper surface when plugged in, with zero intrusive permissions.
 */
class AdvancedWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return AdvancedEngine()
    }

    inner class AdvancedEngine : Engine() {
        private var player: ExoPlayer? = null
        private var stateMachine: WallpaperStateMachine? = null
        private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
        private val chargingRenderer = WallpaperChargingRenderer(applicationContext)

        private var currentConfig: AdvancedConfig? = null
        private var localVideoUri: Uri? = null
        private var isSoundEnabled = false
        private var currentHolder: SurfaceHolder? = null

        private val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> processEvent(InputEvent.ScreenOn)
                    Intent.ACTION_SCREEN_OFF -> processEvent(InputEvent.ScreenOff)
                    Intent.ACTION_USER_PRESENT -> processEvent(InputEvent.UserUnlocked)
                    Intent.ACTION_POWER_CONNECTED -> {
                        val batteryState = getCurrentBatteryChargingState(applicationContext)
                        chargingRenderer.updateBatteryState(batteryState.copy(isCharging = true))
                        processEvent(InputEvent.PowerConnected)
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        val batteryState = getCurrentBatteryChargingState(applicationContext)
                        chargingRenderer.updateBatteryState(batteryState.copy(isCharging = false))
                        processEvent(InputEvent.PowerDisconnected)
                    }
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val batteryState = parseBatteryIntent(intent)
                        chargingRenderer.updateBatteryState(batteryState)
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
                addAction(Intent.ACTION_BATTERY_CHANGED)
            }
            registerReceiver(screenReceiver, filter)

            initializePlayer()
            loadActiveWallpaper()
            observePreferences()
        }

        private fun observePreferences() {
            val prefs = AppContainer.getUserPreferencesRepository(applicationContext)
            serviceScope.launch {
                prefs.isChargingAnimationEnabled.collect { enabled ->
                    currentConfig = currentConfig?.copy(chargingAnimationEnabled = enabled)
                        ?: AdvancedConfig(chargingAnimationEnabled = enabled)
                    currentConfig?.let {
                        stateMachine = WallpaperStateMachine(it)
                    }
                }
            }
            serviceScope.launch {
                prefs.isSoundEnabled.collect { userPref ->
                    val soundAvailable = prefs.isAppliedWallpaperSoundAvailableSync()
                    isSoundEnabled = userPref && soundAvailable
                    player?.volume = if (isSoundEnabled) 1f else 0f
                }
            }
        }

        private fun initializePlayer() {
            val renderersFactory = DefaultRenderersFactory(applicationContext)
                .setEnableDecoderFallback(true)

            player = ExoPlayer.Builder(applicationContext, renderersFactory)
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = 0f // Start muted by default
                    playWhenReady = true
                }
        }

        private fun loadActiveWallpaper() {
            serviceScope.launch {
                val prefs = AppContainer.getUserPreferencesRepository(applicationContext)
                val soundUserPref = prefs.isSoundEnabledSync()
                val soundAvailable = prefs.isAppliedWallpaperSoundAvailableSync()
                val chargingAnimationPref = prefs.isChargingAnimationEnabledSync()
                isSoundEnabled = soundUserPref && soundAvailable
                player?.volume = if (isSoundEnabled) 1f else 0f

                // Retrieve local persistent file
                val localFile = LocalWallpaperStorageManager.getActiveLiveWallpaperFile(applicationContext)
                if (localFile != null && localFile.exists() && localFile.length() > 0) {
                    localVideoUri = Uri.fromFile(localFile)
                    currentConfig = AdvancedConfig(
                        loopMainVideo = true,
                        stopWhenScreenOff = true,
                        restartOnScreenOn = false,
                        chargingAnimationEnabled = chargingAnimationPref
                    )
                    stateMachine = WallpaperStateMachine(currentConfig!!)

                    if (isVisible) {
                        processEvent(InputEvent.ScreenOn)
                    }
                } else {
                    // Fallback to room database if local file was not yet committed
                    val activeId = prefs.getAppliedWallpaperIdSync()
                    if (activeId != null) {
                        val wallpaper = AppContainer.getWallpaperRepository(applicationContext)
                            .getWallpaper(activeId)
                            .firstOrNull()

                        if (wallpaper != null && !wallpaper.videoUrl.isNullOrEmpty()) {
                            localVideoUri = Uri.parse(wallpaper.videoUrl)
                            val baseConfig = wallpaper.advancedConfig ?: AdvancedConfig()
                            currentConfig = baseConfig.copy(
                                chargingAnimationEnabled = chargingAnimationPref
                            )
                            stateMachine = WallpaperStateMachine(currentConfig!!)

                            if (isVisible) {
                                processEvent(InputEvent.ScreenOn)
                            }
                        }
                    }
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
            chargingRenderer.stopImmediate()
            currentHolder = null
            player?.setVideoSurfaceHolder(null)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                if (stateMachine?.isScreenOn == false) {
                    processEvent(InputEvent.ScreenOn)
                } else {
                    val sm = stateMachine
                    if (sm != null && sm.currentState is com.example.domain.state.ScreenState.Charging && sm.config.chargingAnimationEnabled) {
                        currentHolder?.let { chargingRenderer.start(it) }
                    } else {
                        player?.play()
                    }
                }
            } else {
                chargingRenderer.stopImmediate()
                processEvent(InputEvent.ScreenOff)
            }
        }

        private fun processEvent(event: InputEvent) {
            val sm = stateMachine ?: return
            val actions = sm.processEvent(event)
            actions.forEach { executeAction(it) }
        }

        private fun executeAction(action: WallpaperAction) {
            val p = player ?: return
            val uri = localVideoUri ?: return
            val holder = currentHolder

            when (action) {
                is WallpaperAction.PlayCharging -> {
                    // Visual transition: smoothly fade in the luxury Canvas charging presentation over the surface
                    // Audio behavior: strictly preserve the active Live Wallpaper sound preference (no forced mute)
                    p.volume = if (isSoundEnabled) 1f else 0f
                    if (holder != null && isVisible) {
                        val batteryState = getCurrentBatteryChargingState(applicationContext)
                        chargingRenderer.updateBatteryState(batteryState.copy(isCharging = true))
                        chargingRenderer.start(holder)
                    }
                }
                is WallpaperAction.PlayHome -> {
                    // Smoothly fade out charging visual back to the same live wallpaper
                    chargingRenderer.stopWithFade()
                    if (holder != null) {
                        p.setVideoSurfaceHolder(holder)
                    }
                    if (p.currentMediaItem == null) {
                        p.setMediaItem(MediaItem.fromUri(uri))
                        p.repeatMode = Player.REPEAT_MODE_ONE
                        p.volume = if (isSoundEnabled) 1f else 0f
                        p.prepare()
                    } else {
                        p.volume = if (isSoundEnabled) 1f else 0f
                    }
                    p.play()
                }
                is WallpaperAction.PlayLockScreen -> {
                    chargingRenderer.stopWithFade()
                    if (holder != null) {
                        p.setVideoSurfaceHolder(holder)
                    }
                    if (p.currentMediaItem == null) {
                        p.setMediaItem(MediaItem.fromUri(uri))
                        p.repeatMode = Player.REPEAT_MODE_ONE
                        p.volume = if (isSoundEnabled) 1f else 0f
                        p.prepare()
                    }
                    if (action.startFromBeginning) {
                        p.seekTo(0)
                    }
                    p.volume = if (isSoundEnabled) 1f else 0f
                    p.play()
                }
                is WallpaperAction.PlayTransition -> {
                    chargingRenderer.stopWithFade()
                    if (holder != null) {
                        p.setVideoSurfaceHolder(holder)
                    }
                    p.volume = if (isSoundEnabled) 1f else 0f
                    p.play()
                }
                is WallpaperAction.Pause -> {
                    chargingRenderer.stopImmediate()
                    p.pause()
                }
                is WallpaperAction.Mute -> {
                    p.volume = 0f
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                unregisterReceiver(screenReceiver)
            } catch (_: Exception) {}
            chargingRenderer.release()
            player?.stop()
            player?.release()
            player = null
            serviceScope.cancel()
        }
    }
}

package com.example.ui.wallpapers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalWallpaperStorageManager
import com.example.domain.models.Wallpaper
import com.example.domain.repository.UserPreferencesRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WallpaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class WallpaperDetailViewModel(
    private val wallpaperId: String,
    private val wallpaperRepository: WallpaperRepository,
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authUserId: String?
) : ViewModel() {

    private val _wallpaper = MutableStateFlow<Wallpaper?>(null)
    val wallpaper: StateFlow<Wallpaper?> = _wallpaper.asStateFlow()

    private val _applyState = MutableStateFlow<ApplyState>(ApplyState.Idle)
    val applyState: StateFlow<ApplyState> = _applyState.asStateFlow()

    // Preview sound state (muted by default)
    private val _isPreviewSoundOn = MutableStateFlow(false)
    val isPreviewSoundOn: StateFlow<Boolean> = _isPreviewSoundOn.asStateFlow()

    init {
        loadWallpaper()
    }

    private fun loadWallpaper() {
        viewModelScope.launch {
            wallpaperRepository.getWallpaper(wallpaperId).collect { wp ->
                _wallpaper.value = wp
            }
        }
    }

    fun togglePreviewSound() {
        _isPreviewSoundOn.value = !_isPreviewSoundOn.value
    }

    fun onApplyClicked() {
        val wp = _wallpaper.value ?: return

        viewModelScope.launch {
            if (authUserId == null) {
                _applyState.value = ApplyState.RequiresAuth
                return@launch
            }
            initiateApply(wp)
        }
    }

    private fun initiateApply(wallpaper: Wallpaper) {
        viewModelScope.launch {
            _applyState.value = ApplyState.Applying("Verifying authorization...")
            if (authUserId != null) {
                val result = userRepository.applyWallpaper(authUserId, wallpaper.id, wallpaper.isPremium)
                if (result.isFailure) {
                    val message = result.exceptionOrNull()?.message ?: ""
                    if (message.contains("Premium subscription required")) {
                        _applyState.value = ApplyState.RequiresSubscription
                    } else if (message.contains("Reward ad verification missing")) {
                        _applyState.value = ApplyState.RequiresRewardAd(wallpaper, authUserId)
                    } else {
                        _applyState.value = ApplyState.Error("Backend authorization failed: $message")
                    }
                    return@launch
                }
            }

            if (wallpaper.type == "STATIC") {
                _applyState.value = ApplyState.ShowStaticTargetSelection(wallpaper)
            } else {
                // Only show sound prompt if this live wallpaper supports audio
                if (wallpaper.soundAvailable) {
                    _applyState.value = ApplyState.ShowSoundPrompt(wallpaper)
                } else {
                    prepareLiveWallpaperDownload(wallpaper, soundEnabled = false)
                }
            }
        }
    }

    fun onStaticTargetSelected(context: Context, wallpaper: Wallpaper, targetFlags: Int) {
        viewModelScope.launch {
            _applyState.value = ApplyState.Applying("Preparing wallpaper image...")
            try {
                var mediaUrl = wallpaper.imageUrl ?: wallpaper.thumbnailUrl
                if (wallpaper.isPremium) {
                    val premiumRes = wallpaperRepository.getPremiumMediaUrl(wallpaper.id)
                    if (premiumRes.isSuccess) {
                        mediaUrl = premiumRes.getOrNull() ?: mediaUrl
                    }
                }

                val downloadRes = LocalWallpaperStorageManager.downloadMediaToTemp(
                    context = context,
                    urlString = mediaUrl,
                    tempFileName = "temp_static_${wallpaper.id}.jpg"
                )

                if (downloadRes.isSuccess) {
                    val tempFile = downloadRes.getOrNull()!!
                    _applyState.value = ApplyState.ReadyToApplyStatic(wallpaper, tempFile, targetFlags)
                } else {
                    _applyState.value = ApplyState.Error("Failed to download image. Existing wallpaper was not changed.")
                }
            } catch (e: Exception) {
                _applyState.value = ApplyState.Error("Failed to prepare wallpaper: ${e.message}")
            }
        }
    }

    fun onSoundPreferenceSelected(wallpaper: Wallpaper, soundOn: Boolean) {
        prepareLiveWallpaperDownload(wallpaper, soundEnabled = soundOn)
    }

    private fun prepareLiveWallpaperDownload(wallpaper: Wallpaper, soundEnabled: Boolean) {
        viewModelScope.launch {
            _applyState.value = ApplyState.Applying("Preparing live wallpaper assets...")
            userPreferencesRepository.setAppliedWallpaperSoundEnabled(soundEnabled)

            try {
                var videoUrl = wallpaper.videoUrl
                if (wallpaper.isPremium) {
                    val premiumRes = wallpaperRepository.getPremiumMediaUrl(wallpaper.id)
                    if (premiumRes.isSuccess) {
                        videoUrl = premiumRes.getOrNull()
                    }
                }

                if (videoUrl.isNullOrEmpty()) {
                    _applyState.value = ApplyState.Error("Video stream unavailable.")
                    return@launch
                }

                _applyState.value = ApplyState.ReadyToDownloadLive(wallpaper, videoUrl, soundEnabled)
            } catch (e: Exception) {
                _applyState.value = ApplyState.Error("Error preparing download: ${e.message}")
            }
        }
    }

    fun executeLiveDownloadAndPrepare(
        context: Context,
        wallpaper: Wallpaper,
        primaryVideoUrl: String,
        soundEnabled: Boolean
    ) {
        viewModelScope.launch {
            _applyState.value = ApplyState.Applying("Downloading wallpaper bundle...")
            try {
                val stagingDir = LocalWallpaperStorageManager.getStagingDirectory(context, wallpaper.id)
                val primaryFile = File(stagingDir, "primary.mp4")

                val primaryRes = LocalWallpaperStorageManager.downloadMediaToFile(primaryFile, primaryVideoUrl)
                if (primaryRes.isFailure) {
                    _applyState.value = ApplyState.Error("Failed to download primary video. Your current wallpaper was not changed.")
                    return@launch
                }

                val config = wallpaper.advancedConfig
                var lockFileName: String? = null
                var transitionFileName: String? = null
                var chargingFileName: String? = null
                var chargingReturnFileName: String? = null

                if (wallpaper.liveExperienceType == com.example.domain.models.LiveExperienceType.TRANSITION) {
                    if (config?.lockAnimationEnabled == true && !config.lockAnimationVideoUrl.isNullOrEmpty()) {
                        _applyState.value = ApplyState.Applying("Downloading lock screen asset...")
                        val lockFile = File(stagingDir, "lock.mp4")
                        if (LocalWallpaperStorageManager.downloadMediaToFile(lockFile, config.lockAnimationVideoUrl).isSuccess) {
                            lockFileName = "lock.mp4"
                        }
                    }

                    if (config?.unlockTransitionEnabled == true && !config.unlockTransitionVideoUrl.isNullOrEmpty()) {
                        _applyState.value = ApplyState.Applying("Downloading unlock transition asset...")
                        val transFile = File(stagingDir, "transition.mp4")
                        if (LocalWallpaperStorageManager.downloadMediaToFile(transFile, config.unlockTransitionVideoUrl).isSuccess) {
                            transitionFileName = "transition.mp4"
                        }
                    }
                }

                if (config?.chargingAnimationEnabled == true && !config.chargingAnimationVideoUrl.isNullOrEmpty()) {
                    _applyState.value = ApplyState.Applying("Downloading charging asset...")
                    val chargingFile = File(stagingDir, "charging.mp4")
                    if (LocalWallpaperStorageManager.downloadMediaToFile(chargingFile, config.chargingAnimationVideoUrl).isSuccess) {
                        chargingFileName = "charging.mp4"
                    }

                    if (!config.chargingReturnAnimationVideoUrl.isNullOrEmpty()) {
                        val returnFile = File(stagingDir, "charging_return.mp4")
                        if (LocalWallpaperStorageManager.downloadMediaToFile(returnFile, config.chargingReturnAnimationVideoUrl).isSuccess) {
                            chargingReturnFileName = "charging_return.mp4"
                        }
                    }
                }

                val manifest = com.example.domain.models.LiveWallpaperManifest(
                    wallpaperId = wallpaper.id,
                    liveExperienceType = wallpaper.liveExperienceType,
                    soundAvailable = wallpaper.soundAvailable,
                    primaryVideoFile = "primary.mp4",
                    lockVideoFile = lockFileName,
                    transitionVideoFile = transitionFileName,
                    chargingVideoFile = chargingFileName,
                    chargingReturnVideoFile = chargingReturnFileName,
                    lockDurationMs = config?.lockDurationMs ?: 0L,
                    transitionDurationMs = config?.transitionDurationMs ?: 0L,
                    chargingDurationMs = config?.chargingDurationMs ?: 0L,
                    chargingReturnDurationMs = config?.chargingReturnDurationMs ?: 0L
                )

                _applyState.value = ApplyState.ReadyToLaunchLiveSystemIntent(
                    wallpaper = wallpaper,
                    manifest = manifest,
                    soundEnabled = soundEnabled
                )
            } catch (e: Exception) {
                _applyState.value = ApplyState.Error("Error downloading bundle: ${e.message}")
            }
        }
    }

    fun onStaticWallpaperAppliedSuccessfully(context: Context, wallpaper: Wallpaper, tempFile: File) {
        viewModelScope.launch {
            LocalWallpaperStorageManager.commitAppliedStaticWallpaper(context, wallpaper.id, tempFile)
            _applyState.value = ApplyState.Success("Wallpaper applied successfully!")
        }
    }

    fun onLiveWallpaperPickerLaunched(
        context: Context,
        wallpaper: Wallpaper,
        manifest: com.example.domain.models.LiveWallpaperManifest,
        soundEnabled: Boolean
    ) {
        viewModelScope.launch {
            LocalWallpaperStorageManager.promoteStagingToActive(
                context = context,
                wallpaperId = wallpaper.id,
                manifest = manifest,
                soundEnabled = soundEnabled
            )
            _applyState.value = ApplyState.Success("Live wallpaper applied! Choose Home or Home & Lock Screen in system preview.")
        }
    }

    fun onRewardAdEarned(wallpaper: Wallpaper) {
        viewModelScope.launch {
            _applyState.value = ApplyState.Applying("Verifying reward ad...")
            var attempts = 0
            var success = false

            while (attempts < 5 && !success) {
                val result = userRepository.applyWallpaper(authUserId!!, wallpaper.id, wallpaper.isPremium)
                if (result.isSuccess) {
                    success = true
                    if (wallpaper.type == "STATIC") {
                        _applyState.value = ApplyState.ShowStaticTargetSelection(wallpaper)
                    } else {
                        if (wallpaper.soundAvailable) {
                            _applyState.value = ApplyState.ShowSoundPrompt(wallpaper)
                        } else {
                            prepareLiveWallpaperDownload(wallpaper, soundEnabled = false)
                        }
                    }
                } else {
                    val message = result.exceptionOrNull()?.message ?: ""
                    if (message.contains("Reward ad verification missing")) {
                        attempts++
                        kotlinx.coroutines.delay(2000)
                    } else {
                        _applyState.value = ApplyState.Error("Backend rejected ad verification: $message")
                        break
                    }
                }
            }

            if (!success && _applyState.value is ApplyState.Applying) {
                _applyState.value = ApplyState.Error("Ad verification timeout. Please try again.")
            }
        }
    }

    fun toggleFavorite() {
        val wp = _wallpaper.value ?: return
        viewModelScope.launch {
            wallpaperRepository.toggleFavorite(wp.id, !wp.isFavorite)
        }
    }

    fun resetApplyState(context: Context? = null) {
        if (context != null) {
            viewModelScope.launch {
                LocalWallpaperStorageManager.cleanupTempFiles(context)
            }
        }
        _applyState.value = ApplyState.Idle
    }
}

sealed class ApplyState {
    object Idle : ApplyState()
    data class Applying(val message: String) : ApplyState()
    object RequiresAuth : ApplyState()
    object RequiresSubscription : ApplyState()
    data class RequiresRewardAd(val wallpaper: Wallpaper, val uid: String) : ApplyState()
    data class ShowStaticTargetSelection(val wallpaper: Wallpaper) : ApplyState()
    data class ShowSoundPrompt(val wallpaper: Wallpaper) : ApplyState()
    data class ReadyToApplyStatic(val wallpaper: Wallpaper, val tempFile: File, val targetFlags: Int) : ApplyState()
    data class ReadyToDownloadLive(val wallpaper: Wallpaper, val videoUrl: String, val soundEnabled: Boolean) : ApplyState()
    data class ReadyToLaunchLiveSystemIntent(
        val wallpaper: Wallpaper,
        val manifest: com.example.domain.models.LiveWallpaperManifest,
        val soundEnabled: Boolean
    ) : ApplyState()
    data class Success(val message: String) : ApplyState()
    data class Error(val message: String) : ApplyState()
}

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
            _applyState.value = ApplyState.Applying("Downloading video for offline playback...")
            userPreferencesRepository.setSoundEnabled(soundEnabled)

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
        videoUrl: String,
        soundEnabled: Boolean
    ) {
        viewModelScope.launch {
            _applyState.value = ApplyState.Applying("Downloading video for offline playback...")
            val downloadRes = LocalWallpaperStorageManager.downloadMediaToTemp(
                context = context,
                urlString = videoUrl,
                tempFileName = "temp_live_${wallpaper.id}.mp4"
            )

            if (downloadRes.isSuccess) {
                val tempFile = downloadRes.getOrNull()!!
                _applyState.value = ApplyState.ReadyToLaunchLiveSystemIntent(wallpaper, tempFile, soundEnabled)
            } else {
                _applyState.value = ApplyState.Error("Download failed. Your current wallpaper was not changed.")
            }
        }
    }

    fun onStaticWallpaperAppliedSuccessfully(context: Context, wallpaper: Wallpaper, tempFile: File) {
        viewModelScope.launch {
            LocalWallpaperStorageManager.commitAppliedStaticWallpaper(context, wallpaper.id, tempFile)
            _applyState.value = ApplyState.Success("Wallpaper applied successfully!")
        }
    }

    fun onLiveWallpaperPickerLaunched(context: Context, wallpaper: Wallpaper, tempFile: File, soundEnabled: Boolean) {
        viewModelScope.launch {
            LocalWallpaperStorageManager.commitAppliedLiveWallpaper(
                context = context,
                wallpaperId = wallpaper.id,
                tempDownloadedFile = tempFile,
                soundAvailable = wallpaper.soundAvailable && soundEnabled,
                chargingAnimationAvailable = wallpaper.hasChargingAnimation
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
    data class ReadyToLaunchLiveSystemIntent(val wallpaper: Wallpaper, val tempFile: File, val soundEnabled: Boolean) : ApplyState()
    data class Success(val message: String) : ApplyState()
    data class Error(val message: String) : ApplyState()
}

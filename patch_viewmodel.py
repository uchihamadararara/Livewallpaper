with open('app/src/main/java/com/example/ui/wallpapers/WallpaperDetailViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""class WallpaperDetailViewModel(
    private val wallpaperId: String,
    private val wallpaperRepository: WallpaperRepository,
    private val userRepository: UserRepository,
    private val authUserId: String?
)""",
"""import com.example.domain.repository.UserPreferencesRepository

class WallpaperDetailViewModel(
    private val wallpaperId: String,
    private val wallpaperRepository: WallpaperRepository,
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authUserId: String?
)"""
)

content = content.replace(
"""            _applyState.value = ApplyState.ReadyToLaunchSystemIntent(wallpaper)
        }
    }""",
"""            if (wallpaper.type != "STATIC") {
                _applyState.value = ApplyState.ShowSoundPrompt(wallpaper)
            } else {
                _applyState.value = ApplyState.ReadyToLaunchSystemIntent(wallpaper)
            }
        }
    }
    
    fun onSoundPreferenceSelected(wallpaper: Wallpaper, soundOn: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSoundEnabled(soundOn)
            _applyState.value = ApplyState.ReadyToLaunchSystemIntent(wallpaper)
        }
    }"""
)

# Also update the place where onRewardAdEarned calls ReadyToLaunchSystemIntent
content = content.replace(
"""                    success = true
                    _applyState.value = ApplyState.ReadyToLaunchSystemIntent(wallpaper)""",
"""                    success = true
                    if (wallpaper.type != "STATIC") {
                        _applyState.value = ApplyState.ShowSoundPrompt(wallpaper)
                    } else {
                        _applyState.value = ApplyState.ReadyToLaunchSystemIntent(wallpaper)
                    }"""
)

content = content.replace(
"""    data class ReadyToLaunchSystemIntent(val wallpaper: Wallpaper) : ApplyState()
}""",
"""    data class ReadyToLaunchSystemIntent(val wallpaper: Wallpaper) : ApplyState()
    data class ShowSoundPrompt(val wallpaper: Wallpaper) : ApplyState()
}"""
)

with open('app/src/main/java/com/example/ui/wallpapers/WallpaperDetailViewModel.kt', 'w') as f:
    f.write(content)

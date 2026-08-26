package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.models.Wallpaper
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}

class HomeViewModel(
    private val wallpaperRepository: WallpaperRepository,
    private val userRepository: UserRepository,
    private val authUserId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState
    
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    val featuredWallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapers()
        .map { list -> list.filter { it.isFeatured } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val trendingWallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapers()
        .map { list -> list.filter { it.isTrending } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val newWallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapers()
        .map { list -> list.filter { it.isNew } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())



    val allWallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val liveWallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapers()
        .map { list -> list.filter { it.type == "LIVE" || it.type == "ADVANCED_LIVE" } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val premiumWallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapers()
        .map { list -> list.filter { it.isPremium } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    val favoriteWallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getFavoriteWallpapers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        refresh()
        
        if (authUserId != null) {
            viewModelScope.launch {
                userRepository.getUserProfile(authUserId).collect { profile ->
                    _isPremium.value = profile?.subscriptionStatus == "ACTIVE"
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                wallpaperRepository.refreshWallpapers(limit = 50)
                if (authUserId != null) {
                    userRepository.refreshUserProfile(authUserId)
                }
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}

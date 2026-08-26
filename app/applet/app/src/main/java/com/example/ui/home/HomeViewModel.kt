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

    // In a full production app, we would query the database with specific flags
    // For now, we simulate the breakdown using the single stream.
    
    val featuredWallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapers()
        .map { list -> list.filter { it.isFeatured } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val trendingWallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapers()
        .map { list -> list.filter { it.isTrending } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val newWallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapers()
        .map { list -> list.filter { it.isNew } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        refresh()
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

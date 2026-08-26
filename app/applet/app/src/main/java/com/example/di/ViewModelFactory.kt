package com.example.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.domain.repository.WallpaperRepository
import com.example.ui.home.HomeViewModel

class ViewModelFactory(
    private val wallpaperRepository: WallpaperRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(wallpaperRepository, userRepository, authUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BillingRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WallpaperRepository
import com.example.domain.repository.UserPreferencesRepository
import com.example.ui.home.HomeViewModel
import com.example.ui.premium.PremiumViewModel
import com.example.ui.wallpapers.WallpaperDetailViewModel
import com.example.ui.settings.SettingsViewModel

class ViewModelFactory(
    private val wallpaperRepository: WallpaperRepository? = null,
    private val userRepository: UserRepository? = null,
    private val authRepository: AuthRepository? = null,
    private val authUserId: String? = null,
    private val extraId: String? = null,
    private val billingRepository: BillingRepository? = null,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(wallpaperRepository!!, userRepository!!, authUserId) as T
        }
        if (modelClass.isAssignableFrom(WallpaperDetailViewModel::class.java)) {
            return WallpaperDetailViewModel(extraId!!, wallpaperRepository!!, userRepository!!, userPreferencesRepository!!, authUserId) as T
        }
        if (modelClass.isAssignableFrom(PremiumViewModel::class.java)) {
            return PremiumViewModel(billingRepository!!, userRepository!!, authUserId) as T
        }
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(userRepository!!, authRepository!!, billingRepository!!, authUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

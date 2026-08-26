package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val isSoundEnabled = userPreferencesRepository.isSoundEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val isChargingAnimationEnabled = userPreferencesRepository.isChargingAnimationEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSoundEnabled(enabled)
        }
    }

    fun toggleChargingAnimation(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setChargingAnimationEnabled(enabled)
        }
    }
}

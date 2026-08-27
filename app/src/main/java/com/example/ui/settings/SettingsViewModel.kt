package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.models.UserProfile
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BillingRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val billingRepository: BillingRepository,
    private val authUserId: String?
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = if (authUserId != null) {
        userRepository.getUserProfile(authUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    } else {
        MutableStateFlow(null)
    }

    private val _syncState = MutableStateFlow<String?>(null)
    val syncState: StateFlow<String?> = _syncState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (authUserId != null) {
            viewModelScope.launch {
                try {
                    userRepository.refreshUserProfile(authUserId)
                } catch (_: Exception) {}
            }
        }
    }

    fun syncAccount() {
        if (authUserId != null) {
            viewModelScope.launch {
                _syncState.value = "Syncing..."
                try {
                    userRepository.refreshUserProfile(authUserId)
                    _syncState.value = "Synced successfully!"
                } catch (e: Exception) {
                    _syncState.value = "Sync failed: ${e.message}"
                }
            }
        }
    }

    fun clearSyncMessage() {
        _syncState.value = null
    }
}

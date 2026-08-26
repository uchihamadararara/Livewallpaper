package com.example.ui.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.example.domain.models.UserProfile
import com.example.domain.repository.BillingRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PremiumViewModel(
    private val billingRepository: BillingRepository,
    private val userRepository: UserRepository,
    private val authUserId: String?
) : ViewModel() {

    init {
        billingRepository.startBillingConnection()
    }

    val subscriptionProducts: StateFlow<List<ProductDetails>?> = billingRepository.subscriptionProducts
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
        
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        if (authUserId != null) {
            viewModelScope.launch {
                userRepository.getUserProfile(authUserId).collect {
                    _userProfile.value = it
                }
            }
        }
    }

    fun subscribe(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken != null) {
            billingRepository.launchBillingFlow(activity, productDetails, offerToken)
        }
    }
}

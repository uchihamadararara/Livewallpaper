package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.example.data.billing.BillingManager
import com.example.data.network.BackendApiService
import com.example.domain.repository.BillingRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow

class BillingRepositoryImpl(
    private val context: Context,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepositoryImpl,
    private val backendApi: BackendApiService
) : BillingRepository {

    private val billingScope = CoroutineScope(Job() + Dispatchers.IO)
    private val billingManager = BillingManager(context) { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { billingScope.launch { handlePurchase(it) } }
        }
    }

    override val subscriptionProducts: StateFlow<List<ProductDetails>?> = billingManager.productDetails
    override val purchases: StateFlow<List<Purchase>> = billingManager.purchases

    init {
        billingScope.launch {
            billingManager.purchases.collect { purchasesList ->
                purchasesList.forEach { handlePurchase(it) }
            }
        }
    }

    override fun startBillingConnection() {
        billingManager.startBillingConnection()
    }

    override fun launchBillingFlow(activity: Activity, productDetails: ProductDetails, offerToken: String?) {
        billingManager.launchBillingFlow(activity, productDetails, offerToken)
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val result = billingManager.acknowledgePurchase(purchase.purchaseToken)
                if (result.responseCode == BillingClient.BillingResponseCode.OK) verifyPurchaseWithBackend(purchase)
            } else {
                verifyPurchaseWithBackend(purchase)
            }
        }
    }

    // The Firebase ID token is sent in the Authorization header so the
    // Cloudflare Worker can verify who's calling (using Firebase's public
    // keys) before it trusts the purchase token and writes to Firestore
    // with its own service-account credentials.
    private suspend fun verifyPurchaseWithBackend(purchase: Purchase) {
        val uid = authRepository.getUserId() ?: return
        try {
            val idToken = authRepository.getIdToken() ?: return
            val request = mapOf(
                "purchaseToken" to purchase.purchaseToken,
                "productId" to purchase.products.firstOrNull()
            )
            val response = backendApi.verifyPurchase("Bearer $idToken", request)
            if (response.isSuccessful) {
                userRepository.refreshUserProfile(uid)
            } else {
                Log.e("Billing", "Verify failed: ${response.code()} ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("Billing", "Failed to verify purchase", e)
        }
    }
}
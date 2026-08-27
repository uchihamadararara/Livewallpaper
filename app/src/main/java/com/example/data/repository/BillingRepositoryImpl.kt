package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.example.data.network.SupabaseApiService
import com.example.domain.repository.BillingRepository
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BillingRepositoryImpl(
    private val context: Context,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepositoryImpl,
    private val supabaseApi: SupabaseApiService
) : BillingRepository, PurchasesUpdatedListener {

    private val billingScope = CoroutineScope(Job() + Dispatchers.IO)
    private var isConnected = false

    private val _subscriptionProducts = MutableStateFlow<List<ProductDetails>?>(null)
    override val subscriptionProducts: StateFlow<List<ProductDetails>?> = _subscriptionProducts

    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    override val purchases: StateFlow<List<Purchase>> = _purchases

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val subscriptionProductIds = listOf(
        "premium_3_days", "premium_7_days", "premium_monthly", "premium_yearly"
    )

    override fun startBillingConnection() {
        if (isConnected) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isConnected = true
                    billingScope.launch {
                        querySubscriptionProducts()
                        queryPurchases()
                    }
                } else retryBillingConnection()
            }
            override fun onBillingServiceDisconnected() {
                isConnected = false
                retryBillingConnection()
            }
        })
    }

    private fun retryBillingConnection() {
        billingScope.launch {
            delay(2000L)
            startBillingConnection()
        }
    }

    private suspend fun querySubscriptionProducts() {
        val productList = subscriptionProductIds.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it).setProductType(BillingClient.ProductType.SUBS).build()
        }
        val result = billingClient.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(productList).build())
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _subscriptionProducts.value = result.productDetailsList ?: emptyList()
        }
    }

    private suspend fun queryPurchases() {
        val result = billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val purchasesList = result.purchasesList
            _purchases.value = purchasesList
            purchasesList.forEach { handlePurchase(it) }
        }
    }

    override fun launchBillingFlow(activity: Activity, productDetails: ProductDetails, offerToken: String) {
        val params = listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).setOfferToken(offerToken).build())
        billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(params).build())
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            _purchases.value = purchases
            purchases.forEach { billingScope.launch { handlePurchase(it) } }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val result = billingClient.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())
                if (result.responseCode == BillingClient.BillingResponseCode.OK) verifyPurchaseWithBackend(purchase)
            } else {
                verifyPurchaseWithBackend(purchase)
            }
        }
    }

    private suspend fun verifyPurchaseWithBackend(purchase: Purchase) {
        val uid = authRepository.getUserId() ?: return
        try {
            val token = authRepository.getAccessToken() ?: return
            val request = mapOf("purchaseToken" to purchase.purchaseToken, "productId" to purchase.products.firstOrNull())
            val response = supabaseApi.verifyPurchase("Bearer $token", request)
            if (response.isSuccessful) {
                userRepository.refreshUserProfile(uid)
            }
        } catch (e: Exception) {
            Log.e("Billing", "Failed to verify purchase", e)
        }
    }
}

package com.example.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Encapsulates Google Play Billing Library client management, connection handling,
 * product details retrieval (Subscriptions & In-App purchases), and purchase flows.
 */
class BillingManager(
    private val context: Context,
    private val onPurchasesUpdatedListener: ((BillingResult, List<Purchase>?) -> Unit)? = null
) : PurchasesUpdatedListener {

    private val billingScope = CoroutineScope(Job() + Dispatchers.IO)
    private var isConnected = false

    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails.asStateFlow()

    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases: StateFlow<List<Purchase>> = _purchases.asStateFlow()

    private val _billingState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.DISCONNECTED)
    val billingState: StateFlow<BillingConnectionState> = _billingState.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    // Subscription product IDs covering requested tiers
    val subscriptionProductIds = listOf(
        "vip_3days",
        "vip_7days",
        "vip_14days",
        "vip_1month",
        "vip_lifetime",
        "premium_3_days",
        "premium_7_days",
        "premium_14_days",
        "premium_monthly",
        "premium_yearly",
        "premium_lifetime"
    )

    // In-app one-time product IDs (Lifetime Pass)
    val inAppProductIds = listOf(
        "vip_lifetime",
        "premium_lifetime"
    )

    fun startBillingConnection(onConnected: (() -> Unit)? = null) {
        if (isConnected) {
            onConnected?.invoke()
            return
        }
        _billingState.value = BillingConnectionState.CONNECTING
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isConnected = true
                    _billingState.value = BillingConnectionState.CONNECTED
                    billingScope.launch {
                        queryProducts()
                        queryPurchases()
                        onConnected?.invoke()
                    }
                } else {
                    _billingState.value = BillingConnectionState.ERROR(billingResult.debugMessage)
                    retryBillingConnection()
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnected = false
                _billingState.value = BillingConnectionState.DISCONNECTED
                retryBillingConnection()
            }
        })
    }

    private fun retryBillingConnection() {
        billingScope.launch {
            kotlinx.coroutines.delay(3000)
            startBillingConnection()
        }
    }

    suspend fun queryProducts() {
        val allProducts = mutableListOf<ProductDetails>()

        // 1. Query SUBS (3 days, 7 days, 14 days, 1 month, yearly, etc.)
        try {
            val subProductList = subscriptionProductIds.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }
            val subResult = billingClient.queryProductDetails(
                QueryProductDetailsParams.newBuilder().setProductList(subProductList).build()
            )
            if (subResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                subResult.productDetailsList?.let { allProducts.addAll(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying SUBS product details", e)
        }

        // 2. Query INAPP (Lifetime one-time passes)
        try {
            val inAppProductList = inAppProductIds.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }
            val inAppResult = billingClient.queryProductDetails(
                QueryProductDetailsParams.newBuilder().setProductList(inAppProductList).build()
            )
            if (inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                inAppResult.productDetailsList?.let { inApps ->
                    inApps.forEach { inApp ->
                        if (allProducts.none { it.productId == inApp.productId }) {
                            allProducts.add(inApp)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying INAPP product details", e)
        }

        _productDetails.value = allProducts
    }

    suspend fun queryPurchases() {
        val allPurchases = mutableListOf<Purchase>()

        try {
            val subResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
            )
            if (subResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                allPurchases.addAll(subResult.purchasesList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying SUBS purchases", e)
        }

        try {
            val inAppResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
            )
            if (inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                allPurchases.addAll(inAppResult.purchasesList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying INAPP purchases", e)
        }

        _purchases.value = allPurchases
    }

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String? = null
    ): BillingResult {
        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        val selectedOfferToken = offerToken ?: productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (!selectedOfferToken.isNullOrEmpty()) {
            productDetailsParamsBuilder.setOfferToken(selectedOfferToken)
        }

        val params = listOf(productDetailsParamsBuilder.build())
        return billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(params).build()
        )
    }

    suspend fun acknowledgePurchase(purchaseToken: String): BillingResult {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        return billingClient.acknowledgePurchase(params)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            _purchases.value = purchases
        }
        onPurchasesUpdatedListener?.invoke(billingResult, purchases)
    }

    fun getProductDetailsForPlan(planIds: List<String>): ProductDetails? {
        return _productDetails.value.firstOrNull { it.productId in planIds }
    }

    fun getFormattedPrice(productDetails: ProductDetails?): String? {
        if (productDetails == null) return null
        val subPrice = productDetails.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
        if (!subPrice.isNullOrEmpty()) return subPrice

        val inAppPrice = productDetails.oneTimePurchaseOfferDetails?.formattedPrice
        if (!inAppPrice.isNullOrEmpty()) return inAppPrice

        return null
    }

    fun endConnection() {
        if (isConnected) {
            billingClient.endConnection()
            isConnected = false
            _billingState.value = BillingConnectionState.DISCONNECTED
        }
    }

    companion object {
        private const val TAG = "BillingManager"
    }
}

sealed class BillingConnectionState {
    object DISCONNECTED : BillingConnectionState()
    object CONNECTING : BillingConnectionState()
    object CONNECTED : BillingConnectionState()
    data class ERROR(val message: String) : BillingConnectionState()
}

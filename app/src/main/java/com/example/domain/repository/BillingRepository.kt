package com.example.domain.repository

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.StateFlow

interface BillingRepository {
    val subscriptionProducts: StateFlow<List<ProductDetails>?>
    val purchases: StateFlow<List<Purchase>>
    
    fun startBillingConnection()
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails, offerToken: String)
}

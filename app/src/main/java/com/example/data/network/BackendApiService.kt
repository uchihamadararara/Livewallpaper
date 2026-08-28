package com.example.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// Only ONE backend endpoint left: purchase verification. Everything else
// (wallpapers list, applying a wallpaper, favorites) is done directly
// against Firestore from the client, guarded by Firestore Security Rules.
// This hits a Cloudflare Worker, not Firebase Functions, so Spark (free) plan is enough.
interface BackendApiService {
    @POST("verify-purchase")
    suspend fun verifyPurchase(
        @Header("Authorization") auth: String,
        @Body request: Map<String, String?>
    ): Response<Map<String, Any>>
}
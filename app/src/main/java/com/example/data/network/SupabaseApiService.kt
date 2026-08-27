package com.example.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

interface SupabaseApiService {
    @POST("auth/v1/signup")
    suspend fun signUpAnonymously(
        @Header("apikey") apiKey: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Header("apikey") apiKey: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @GET("rest/v1/wallpapers")
    suspend fun getWallpapers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("select") select: String = "*",
        @Query("is_active") isActive: String = "eq.true",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 100
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/users")
    suspend fun getUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("id") eqId: String
    ): Response<List<Map<String, Any>>>

    @POST("rest/v1/users")
    suspend fun createUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body user: Map<String, Any>
    ): Response<List<Map<String, Any>>>

    @POST("functions/v1/apply-wallpaper")
    suspend fun applyWallpaper(
        @Header("Authorization") auth: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("functions/v1/verify-purchase")
    suspend fun verifyPurchase(
        @Header("Authorization") auth: String,
        @Body request: Map<String, String?>
    ): Response<Map<String, Any>>

    @POST("functions/v1/get-premium-url")
    suspend fun getPremiumMediaUrl(
        @Header("Authorization") auth: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>
}

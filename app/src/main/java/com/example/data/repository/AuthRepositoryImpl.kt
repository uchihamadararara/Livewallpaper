package com.example.data.repository

import com.example.data.network.SupabaseApiService
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val supabaseApi: SupabaseApiService
) : AuthRepository {
    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn
    
    private var currentToken: String? = null
    private var currentUserId: String? = null

    suspend fun getAccessToken(): String? = currentToken
    suspend fun getUserId(): String? = currentUserId

    override suspend fun signInAnonymously(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = emptyMap<String, String>()
                val response = supabaseApi.signUpAnonymously(
                    com.example.di.AppContainer.SUPABASE_ANON_KEY,
                    request
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    currentToken = body?.get("access_token") as? String
                    val user = body?.get("user") as? Map<*, *>
                    currentUserId = user?.get("id") as? String
                    _isLoggedIn.value = true
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Auth failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun signOut() {
        currentToken = null
        currentUserId = null
        _isLoggedIn.value = false
    }
}

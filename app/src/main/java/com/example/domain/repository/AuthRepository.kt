package com.example.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val isLoggedIn: StateFlow<Boolean>
    suspend fun signInAnonymously(): Result<Unit>
    suspend fun signOut()
}

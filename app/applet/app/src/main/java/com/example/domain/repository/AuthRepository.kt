package com.example.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: Flow<String?>
    suspend fun signInAnonymously(): Result<String> // Used as a fallback if Google sign in isn't requested yet
    suspend fun signOut()
}

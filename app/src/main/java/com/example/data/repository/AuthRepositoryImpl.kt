package com.example.data.repository

import com.example.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    private val _isLoggedIn = MutableStateFlow(firebaseAuth.currentUser != null)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        // Keeps isLoggedIn in sync automatically, no manual token bookkeeping needed
        firebaseAuth.addAuthStateListener { auth ->
            _isLoggedIn.value = auth.currentUser != null
        }
    }

    // Firestore attaches the ID token automatically, so repos generally just
    // need the uid. getIdToken() is only for the one plain-HTTP call left
    // (verify-purchase on the Cloudflare Worker), which needs the raw token
    // in an Authorization header since it isn't a Firebase SDK call.
    fun getUserId(): String? = firebaseAuth.currentUser?.uid

    suspend fun getIdToken(): String? = firebaseAuth.currentUser?.getIdToken(false)?.await()?.token

    override suspend fun signInAnonymously(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (firebaseAuth.currentUser != null) return@withContext Result.success(Unit)
                firebaseAuth.signInAnonymously().await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }
}
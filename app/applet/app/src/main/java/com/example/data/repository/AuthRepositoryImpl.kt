package com.example.data.repository

import com.example.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUserId: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        // Initial state
        trySend(auth.currentUser?.uid)

        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    override suspend fun signInAnonymously(): Result<String> {
        return try {
            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid ?: throw Exception("Auth failed, uid is null")
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}

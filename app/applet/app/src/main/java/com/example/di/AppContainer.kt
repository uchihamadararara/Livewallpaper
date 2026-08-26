package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.WallpaperRepositoryImpl
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.UserRepository
import com.example.domain.repository.WallpaperRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.functions.FirebaseFunctions

/**
 * A lightweight manual dependency injection container.
 * Keeps architecture clean without adding heavy frameworks like Hilt unless requested.
 */
object AppContainer {
    
    private var database: AppDatabase? = null

    // Firebase Singletons
    val functions: FirebaseFunctions by lazy {
        FirebaseFunctions.getInstance()
    }

    val firestore: FirebaseFirestore by lazy {
        val fs = FirebaseFirestore.getInstance()
        // We use default settings, but can enable persistence if needed.
        fs.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        fs
    }
    
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Repositories
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(auth)
    }

    val userRepository: UserRepository by lazy {
        UserRepositoryImpl(firestore, functions)
    }

    private var _wallpaperRepository: WallpaperRepository? = null

    fun getWallpaperRepository(context: Context): WallpaperRepository {
        if (_wallpaperRepository == null) {
            val db = database ?: AppDatabase.getDatabase(context).also { database = it }
            _wallpaperRepository = WallpaperRepositoryImpl(db.wallpaperDao(), firestore, functions)
        }
        return _wallpaperRepository!!
    }
}

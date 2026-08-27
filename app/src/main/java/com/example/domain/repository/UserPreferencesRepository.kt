package com.example.domain.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {
    companion object {
        val APPLIED_WALLPAPER_SOUND_ENABLED = booleanPreferencesKey("applied_wallpaper_sound_enabled")
        val APPLIED_WALLPAPER_ID = stringPreferencesKey("applied_wallpaper_id")
        val APPLIED_WALLPAPER_TYPE = stringPreferencesKey("applied_wallpaper_type")
        val APPLIED_WALLPAPER_EXPERIENCE_TYPE = stringPreferencesKey("applied_wallpaper_experience_type")
        val APPLIED_WALLPAPER_PATH = stringPreferencesKey("applied_wallpaper_path")
        val APPLIED_WALLPAPER_SOUND_AVAILABLE = booleanPreferencesKey("applied_wallpaper_sound_available")
        val APPLIED_WALLPAPER_CHARGING_AVAILABLE = booleanPreferencesKey("applied_wallpaper_charging_available")
    }

    val isAppliedWallpaperSoundEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[APPLIED_WALLPAPER_SOUND_ENABLED] ?: false
        }

    val appliedWallpaperId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[APPLIED_WALLPAPER_ID]
        }

    val appliedWallpaperPath: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[APPLIED_WALLPAPER_PATH]
        }

    val appliedWallpaperType: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[APPLIED_WALLPAPER_TYPE]
        }

    val appliedWallpaperExperienceType: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[APPLIED_WALLPAPER_EXPERIENCE_TYPE]
        }

    val appliedWallpaperSoundAvailable: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[APPLIED_WALLPAPER_SOUND_AVAILABLE] ?: false
        }

    val appliedWallpaperChargingAvailable: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[APPLIED_WALLPAPER_CHARGING_AVAILABLE] ?: false
        }

    suspend fun setAppliedWallpaperSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APPLIED_WALLPAPER_SOUND_ENABLED] = enabled
        }
    }

    suspend fun setAppliedWallpaper(
        id: String,
        type: String,
        experienceType: String,
        localPath: String,
        soundAvailable: Boolean,
        chargingAnimationAvailable: Boolean = false,
        soundEnabled: Boolean = false
    ) {
        context.dataStore.edit { preferences ->
            preferences[APPLIED_WALLPAPER_ID] = id
            preferences[APPLIED_WALLPAPER_TYPE] = type
            preferences[APPLIED_WALLPAPER_EXPERIENCE_TYPE] = experienceType
            preferences[APPLIED_WALLPAPER_PATH] = localPath
            preferences[APPLIED_WALLPAPER_SOUND_AVAILABLE] = soundAvailable
            preferences[APPLIED_WALLPAPER_CHARGING_AVAILABLE] = chargingAnimationAvailable
            preferences[APPLIED_WALLPAPER_SOUND_ENABLED] = if (soundAvailable) soundEnabled else false
        }
    }

    suspend fun getAppliedWallpaperIdSync(): String? {
        return context.dataStore.data.map { it[APPLIED_WALLPAPER_ID] }.firstOrNull()
    }

    suspend fun getAppliedWallpaperPathSync(): String? {
        return context.dataStore.data.map { it[APPLIED_WALLPAPER_PATH] }.firstOrNull()
    }

    suspend fun getAppliedWallpaperExperienceTypeSync(): String? {
        return context.dataStore.data.map { it[APPLIED_WALLPAPER_EXPERIENCE_TYPE] }.firstOrNull()
    }

    suspend fun isAppliedWallpaperSoundEnabledSync(): Boolean {
        return context.dataStore.data.map { it[APPLIED_WALLPAPER_SOUND_ENABLED] ?: false }.firstOrNull() ?: false
    }

    suspend fun isAppliedWallpaperSoundAvailableSync(): Boolean {
        return context.dataStore.data.map { it[APPLIED_WALLPAPER_SOUND_AVAILABLE] ?: false }.firstOrNull() ?: false
    }

    suspend fun isAppliedWallpaperChargingAnimationAvailableSync(): Boolean {
        return context.dataStore.data.map { it[APPLIED_WALLPAPER_CHARGING_AVAILABLE] ?: false }.firstOrNull() ?: false
    }
}


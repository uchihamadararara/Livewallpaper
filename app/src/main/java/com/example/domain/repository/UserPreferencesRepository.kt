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
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val CHARGING_ANIMATION_ENABLED = booleanPreferencesKey("charging_animation_enabled")
        val APPLIED_WALLPAPER_ID = stringPreferencesKey("applied_wallpaper_id")
        val APPLIED_WALLPAPER_TYPE = stringPreferencesKey("applied_wallpaper_type")
        val APPLIED_WALLPAPER_PATH = stringPreferencesKey("applied_wallpaper_path")
        val APPLIED_WALLPAPER_SOUND_AVAILABLE = booleanPreferencesKey("applied_wallpaper_sound_available")
        val APPLIED_WALLPAPER_CHARGING_AVAILABLE = booleanPreferencesKey("applied_wallpaper_charging_available")
    }

    val isSoundEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SOUND_ENABLED] ?: false
        }

    val isChargingAnimationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CHARGING_ANIMATION_ENABLED] ?: true
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

    val appliedWallpaperSoundAvailable: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[APPLIED_WALLPAPER_SOUND_AVAILABLE] ?: false
        }

    val appliedWallpaperChargingAvailable: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[APPLIED_WALLPAPER_CHARGING_AVAILABLE] ?: false
        }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SOUND_ENABLED] = enabled
        }
    }

    suspend fun setChargingAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CHARGING_ANIMATION_ENABLED] = enabled
        }
    }

    suspend fun setAppliedWallpaper(
        id: String,
        type: String,
        localPath: String,
        soundAvailable: Boolean,
        chargingAnimationAvailable: Boolean = false
    ) {
        context.dataStore.edit { preferences ->
            preferences[APPLIED_WALLPAPER_ID] = id
            preferences[APPLIED_WALLPAPER_TYPE] = type
            preferences[APPLIED_WALLPAPER_PATH] = localPath
            preferences[APPLIED_WALLPAPER_SOUND_AVAILABLE] = soundAvailable
            preferences[APPLIED_WALLPAPER_CHARGING_AVAILABLE] = chargingAnimationAvailable
        }
    }

    suspend fun getAppliedWallpaperIdSync(): String? {
        return context.dataStore.data.map { it[APPLIED_WALLPAPER_ID] }.firstOrNull()
    }

    suspend fun getAppliedWallpaperPathSync(): String? {
        return context.dataStore.data.map { it[APPLIED_WALLPAPER_PATH] }.firstOrNull()
    }

    suspend fun isSoundEnabledSync(): Boolean {
        return context.dataStore.data.map { it[SOUND_ENABLED] ?: false }.firstOrNull() ?: false
    }

    suspend fun isChargingAnimationEnabledSync(): Boolean {
        return context.dataStore.data.map { it[CHARGING_ANIMATION_ENABLED] ?: true }.firstOrNull() ?: true
    }

    suspend fun isAppliedWallpaperSoundAvailableSync(): Boolean {
        return context.dataStore.data.map { it[APPLIED_WALLPAPER_SOUND_AVAILABLE] ?: false }.firstOrNull() ?: false
    }

    suspend fun isAppliedWallpaperChargingAnimationAvailableSync(): Boolean {
        return context.dataStore.data.map { it[APPLIED_WALLPAPER_CHARGING_AVAILABLE] ?: false }.firstOrNull() ?: false
    }
}

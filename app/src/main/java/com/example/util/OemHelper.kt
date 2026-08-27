package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Helper to detect device manufacturer differences and handle OEM restrictions gracefully
 * without using undocumented hacks or bypassing system security.
 */
object OemHelper {

    fun getManufacturerName(): String {
        return Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    fun isSamsung(): Boolean {
        return Build.MANUFACTURER.contains("samsung", ignoreCase = true) ||
                Build.BRAND.contains("samsung", ignoreCase = true)
    }

    fun isXiaomi(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") ||
                b.contains("xiaomi") || b.contains("redmi") || b.contains("poco")
    }

    fun isOppoOrRealme(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("oppo") || m.contains("realme") || m.contains("oneplus") ||
                b.contains("oppo") || b.contains("realme") || b.contains("oneplus")
    }

    fun isVivo(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("vivo") || m.contains("iqoo") ||
                b.contains("vivo") || b.contains("iqoo")
    }

    fun isPixelOrAosp(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("google") || b.contains("google")
    }

    /**
     * Returns an informational notice regarding OEM live wallpaper behavior if applicable.
     */
    fun getLiveWallpaperLimitationNotice(): String? {
        return when {
            isSamsung() -> "Samsung One UI typically applies Live Wallpapers to the Home Screen."
            isXiaomi() -> "Xiaomi HyperOS/MIUI manages Lock Screen wallpapers via the system Themes engine."
            isOppoOrRealme() -> "ColorOS may limit third-party Live Wallpapers to the Home Screen."
            isVivo() -> "Funtouch OS / OriginOS may restrict Live Wallpapers to the Home Screen."
            else -> null
        }
    }

    /**
     * Opens system battery optimization settings safely so the user can optionally
     * whitelist the app if their OEM background manager pauses the live wallpaper.
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(appDetailsIntent)
            } catch (_: Exception) {
                // Ignore if settings cannot be opened
            }
        }
    }
}

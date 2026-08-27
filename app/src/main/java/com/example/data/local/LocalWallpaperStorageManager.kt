package com.example.data.local

import android.content.Context
import com.example.di.AppContainer
import com.example.domain.models.LiveWallpaperManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages persistent app-private storage for the currently applied wallpaper.
 *
 * Rules:
 * 1. Only the CURRENTLY APPLIED wallpaper has persistent local bundle media in filesDir/wallpapers/active/.
 * 2. Downloading multi-state assets is performed into a staging folder filesDir/wallpapers/staging/{wallpaperId}/.
 * 3. Atomic bundle promotion promotes staging to active ONLY AFTER all configured assets download and verify successfully.
 * 4. Active bundle includes manifest.json mapping multi-state video assets (primary, lock, transition, charging, charging_return).
 * 5. Reboot and offline playback read from active bundle without network requests.
 */
object LocalWallpaperStorageManager {

    private const val WALLPAPERS_DIR_NAME = "wallpapers"
    private const val ACTIVE_BUNDLE_DIR_NAME = "active"
    private const val STAGING_DIR_NAME = "staging"

    fun getWallpapersDirectory(context: Context): File {
        val dir = File(context.filesDir, WALLPAPERS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getActiveBundleDirectory(context: Context): File {
        val dir = File(getWallpapersDirectory(context), ACTIVE_BUNDLE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getStagingDirectory(context: Context, wallpaperId: String): File {
        val stagingRoot = File(getWallpapersDirectory(context), STAGING_DIR_NAME)
        val dir = File(stagingRoot, wallpaperId)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun downloadMediaToFile(
        targetFile: File,
        urlString: String
    ): Result<File> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            targetFile.parentFile?.mkdirs()

            var currentUrl = urlString
            var redirectCount = 0
            val maxRedirects = 5

            while (redirectCount < maxRedirects) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.instanceFollowRedirects = true

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == 307 || responseCode == 308
                ) {
                    val newUrl = connection.getHeaderField("Location") ?: break
                    connection.disconnect()
                    currentUrl = newUrl
                    redirectCount++
                } else if (responseCode == HttpURLConnection.HTTP_OK) {
                    break
                } else {
                    return@withContext Result.failure(Exception("HTTP error $responseCode downloading media"))
                }
            }

            val conn = connection ?: return@withContext Result.failure(Exception("Could not open connection"))
            inputStream = conn.inputStream
            outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()

            if (targetFile.exists() && targetFile.length() > 0) {
                Result.success(targetFile)
            } else {
                targetFile.delete()
                Result.failure(Exception("Downloaded file is empty"))
            }
        } catch (e: Exception) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            Result.failure(e)
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }

    suspend fun downloadMediaToTemp(
        context: Context,
        urlString: String,
        tempFileName: String
    ): Result<File> = withContext(Dispatchers.IO) {
        val dir = getWallpapersDirectory(context)
        val tempFile = File(dir, tempFileName)
        downloadMediaToFile(tempFile, urlString)
    }

    suspend fun promoteStagingToActive(
        context: Context,
        wallpaperId: String,
        manifest: LiveWallpaperManifest,
        soundEnabled: Boolean
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val stagingDir = getStagingDirectory(context, wallpaperId)
            val activeDir = getActiveBundleDirectory(context)

            // Write manifest.json to staging
            val manifestFile = File(stagingDir, "manifest.json")
            manifestFile.writeText(Json.encodeToString(manifest))

            // Delete existing active directory contents
            val activeFiles = activeDir.listFiles() ?: emptyArray()
            for (f in activeFiles) {
                f.deleteRecursively()
            }

            // Move or copy staging files to active directory
            val stagingFiles = stagingDir.listFiles() ?: emptyArray()
            for (f in stagingFiles) {
                val dest = File(activeDir, f.name)
                if (!f.renameTo(dest)) {
                    f.copyTo(dest, overwrite = true)
                    f.delete()
                }
            }

            // Clean up staging directory
            stagingDir.deleteRecursively()

            // Update user preferences for immediate offline/reboot retrieval
            val primaryFile = manifest.primaryVideoFile?.let { File(activeDir, it) }
            val prefs = AppContainer.getUserPreferencesRepository(context)
            prefs.setAppliedWallpaper(
                id = wallpaperId,
                type = "LIVE",
                experienceType = manifest.liveExperienceType.name,
                localPath = primaryFile?.absolutePath ?: activeDir.absolutePath,
                soundAvailable = manifest.soundAvailable,
                chargingAnimationAvailable = !manifest.chargingVideoFile.isNullOrEmpty(),
                soundEnabled = soundEnabled
            )

            Result.success(activeDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveManifest(context: Context): LiveWallpaperManifest? = withContext(Dispatchers.IO) {
        try {
            val activeDir = getActiveBundleDirectory(context)
            val manifestFile = File(activeDir, "manifest.json")
            if (manifestFile.exists() && manifestFile.length() > 0) {
                return@withContext Json.decodeFromString<LiveWallpaperManifest>(manifestFile.readText())
            }
        } catch (_: Exception) {}
        null
    }

    suspend fun getActiveAssetFile(context: Context, filename: String?): File? = withContext(Dispatchers.IO) {
        if (filename.isNullOrEmpty()) return@withContext null
        val activeDir = getActiveBundleDirectory(context)
        val file = File(activeDir, filename)
        if (file.exists() && file.length() > 0) file else null
    }

    suspend fun commitAppliedStaticWallpaper(
        context: Context,
        wallpaperId: String,
        tempDownloadedFile: File? = null
    ): Result<File?> = withContext(Dispatchers.IO) {
        try {
            val dir = getWallpapersDirectory(context)
            var targetFile: File? = null

            if (tempDownloadedFile != null && tempDownloadedFile.exists()) {
                targetFile = File(dir, "active_static_${wallpaperId}.jpg")
                if (tempDownloadedFile.canonicalPath != targetFile.canonicalPath) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    val renamed = tempDownloadedFile.renameTo(targetFile)
                    if (!renamed) {
                        tempDownloadedFile.copyTo(targetFile, overwrite = true)
                        tempDownloadedFile.delete()
                    }
                }
            }

            // Cleanup old static wallpaper files
            val allFiles = dir.listFiles() ?: emptyArray()
            for (file in allFiles) {
                if (file.isFile && file.name.startsWith("active_static_") && (targetFile == null || file.name != targetFile.name)) {
                    file.delete()
                }
            }

            val prefs = AppContainer.getUserPreferencesRepository(context)
            prefs.setAppliedWallpaper(
                id = wallpaperId,
                type = "STATIC",
                experienceType = "NORMAL",
                localPath = targetFile?.absolutePath ?: "",
                soundAvailable = false,
                chargingAnimationAvailable = false,
                soundEnabled = false
            )

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveLiveWallpaperFile(context: Context): File? = withContext(Dispatchers.IO) {
        // 1. Check active bundle directory via manifest
        val manifest = getActiveManifest(context)
        if (manifest != null && !manifest.primaryVideoFile.isNullOrEmpty()) {
            val file = getActiveAssetFile(context, manifest.primaryVideoFile)
            if (file != null) return@withContext file
        }

        // 2. Check saved path in prefs
        val prefs = AppContainer.getUserPreferencesRepository(context)
        val savedPath = prefs.getAppliedWallpaperPathSync()
        if (!savedPath.isNullOrEmpty()) {
            val file = File(savedPath)
            if (file.exists() && file.length() > 0) {
                return@withContext file
            }
        }

        // 3. Fallback: look for any .mp4 in active bundle dir
        val activeDir = getActiveBundleDirectory(context)
        val activeMp4s = activeDir.listFiles { _, name -> name.endsWith(".mp4") }
        if (!activeMp4s.isNullOrEmpty()) {
            val active = activeMp4s.firstOrNull { it.exists() && it.length() > 0 }
            if (active != null) return@withContext active
        }

        // 4. Fallback: look for legacy active_live_*.mp4 in wallpapers directory
        val dir = getWallpapersDirectory(context)
        val liveFiles = dir.listFiles { _, name -> name.startsWith("active_live_") && name.endsWith(".mp4") }
        if (!liveFiles.isNullOrEmpty()) {
            val active = liveFiles.maxByOrNull { it.lastModified() }
            if (active != null && active.exists() && active.length() > 0) {
                return@withContext active
            }
        }
        null
    }

    suspend fun cleanupTempFiles(context: Context) = withContext(Dispatchers.IO) {
        try {
            val dir = getWallpapersDirectory(context)
            val tempFiles = dir.listFiles { _, name -> name.startsWith("temp_") }
            tempFiles?.forEach { it.delete() }
            val stagingRoot = File(dir, STAGING_DIR_NAME)
            if (stagingRoot.exists()) {
                stagingRoot.deleteRecursively()
            }
        } catch (_: Exception) {}
    }
}


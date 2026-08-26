package com.example.data.local

import android.content.Context
import com.example.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages persistent app-private storage for the currently applied wallpaper.
 *
 * Rules:
 * 1. Only the CURRENTLY APPLIED wallpaper has persistent local media in filesDir/wallpapers/.
 * 2. Downloading is performed directly to a temporary file before application.
 * 3. Previous media is deleted ONLY AFTER the new wallpaper is successfully applied.
 * 4. Reboot and offline playback read from this persistent local file without network requests.
 */
object LocalWallpaperStorageManager {

    private const val WALLPAPERS_DIR_NAME = "wallpapers"

    fun getWallpapersDirectory(context: Context): File {
        val dir = File(context.filesDir, WALLPAPERS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun downloadMediaToTemp(
        context: Context,
        urlString: String,
        tempFileName: String
    ): Result<File> = withContext(Dispatchers.IO) {
        val dir = getWallpapersDirectory(context)
        val tempFile = File(dir, tempFileName)
        
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            if (tempFile.exists()) {
                tempFile.delete()
            }

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
            outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()

            if (tempFile.exists() && tempFile.length() > 0) {
                Result.success(tempFile)
            } else {
                tempFile.delete()
                Result.failure(Exception("Downloaded file is empty"))
            }
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            Result.failure(e)
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }

    suspend fun commitAppliedLiveWallpaper(
        context: Context,
        wallpaperId: String,
        tempDownloadedFile: File,
        soundAvailable: Boolean,
        chargingAnimationAvailable: Boolean = false
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = getWallpapersDirectory(context)
            val targetFile = File(dir, "active_live_${wallpaperId}.mp4")

            // Atomic rename or copy
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

            // Cleanup any previous old wallpaper files so ONLY ONE active wallpaper exists locally
            val allFiles = dir.listFiles() ?: emptyArray()
            for (file in allFiles) {
                if (file.name != targetFile.name) {
                    file.delete()
                }
            }

            // Update user preferences for immediate offline/reboot retrieval
            val prefs = AppContainer.getUserPreferencesRepository(context)
            prefs.setAppliedWallpaper(
                id = wallpaperId,
                type = "LIVE",
                localPath = targetFile.absolutePath,
                soundAvailable = soundAvailable,
                chargingAnimationAvailable = chargingAnimationAvailable
            )

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

            // Cleanup old wallpaper files
            val allFiles = dir.listFiles() ?: emptyArray()
            for (file in allFiles) {
                if (targetFile == null || file.name != targetFile.name) {
                    file.delete()
                }
            }

            val prefs = AppContainer.getUserPreferencesRepository(context)
            prefs.setAppliedWallpaper(
                id = wallpaperId,
                type = "STATIC",
                localPath = targetFile?.absolutePath ?: "",
                soundAvailable = false
            )

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveLiveWallpaperFile(context: Context): File? = withContext(Dispatchers.IO) {
        val prefs = AppContainer.getUserPreferencesRepository(context)
        val savedPath = prefs.getAppliedWallpaperPathSync()
        if (!savedPath.isNullOrEmpty()) {
            val file = File(savedPath)
            if (file.exists() && file.length() > 0) {
                return@withContext file
            }
        }

        // Fallback: look for any active_live_*.mp4 in wallpapers directory
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
        } catch (_: Exception) {}
    }
}

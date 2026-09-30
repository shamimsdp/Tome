package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Representation of a GitHub Release version asset.
 */
data class AppReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val publishedAt: String,
    val downloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long,
    val isNewer: Boolean,
    val htmlUrl: String
) {
    val formattedSize: String
        get() {
            if (apkSizeBytes <= 0) return "Direct APK"
            val mb = apkSizeBytes / (1024.0 * 1024.0)
            return String.format(Locale.US, "%.1f MB", mb)
        }
}

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : UpdateDownloadState()
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadState()
    data class Error(val message: String) : UpdateDownloadState()
}

/**
 * AppUpdateManager handles:
 * - Querying GitHub Releases API (https://api.github.com/repos/{owner}/{repo}/releases/latest)
 * - Semantic version comparison against current BuildConfig.VERSION_NAME / VERSION_CODE
 * - Notifying when a new version is available
 * - Downloading the APK asset with live progress tracking
 * - Prompting Android Package Installer via FileProvider to update the application
 */
class AppUpdateManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("tome_app_update_prefs", Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        const val PREF_GITHUB_REPO = "github_repo"
        const val PREF_LAST_CHECK_TIME = "last_update_check_time"
        const val PREF_DISMISSED_TAG = "dismissed_release_tag"
        const val DEFAULT_REPO = "shamim-bjit/tome-pdf-reader"
    }

    private val _latestRelease = MutableStateFlow<AppReleaseInfo?>(null)
    val latestRelease: StateFlow<AppReleaseInfo?> = _latestRelease.asStateFlow()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _checkStatusMessage = MutableStateFlow<String?>(null)
    val checkStatusMessage: StateFlow<String?> = _checkStatusMessage.asStateFlow()

    fun getGitHubRepo(): String {
        return prefs.getString(PREF_GITHUB_REPO, DEFAULT_REPO) ?: DEFAULT_REPO
    }

    fun setGitHubRepo(repo: String) {
        val cleaned = repo.trim().removePrefix("https://github.com/").removeSuffix(".git").trim('/')
        prefs.edit().putString(PREF_GITHUB_REPO, cleaned).apply()
    }

    fun getLastCheckTime(): Long {
        return prefs.getLong(PREF_LAST_CHECK_TIME, 0L)
    }

    fun dismissCurrentUpdateNotification() {
        val tag = _latestRelease.value?.tagName ?: return
        prefs.edit().putString(PREF_DISMISSED_TAG, tag).apply()
        _latestRelease.value = null
    }

    /**
     * Checks GitHub API for the latest published release in the configured repository.
     */
    suspend fun checkForUpdates(forceCheck: Boolean = false): Result<AppReleaseInfo?> = withContext(Dispatchers.IO) {
        _isChecking.value = true
        _checkStatusMessage.value = "Checking for new versions on GitHub..."

        try {
            val repo = getGitHubRepo()
            val url = "https://api.github.com/repos/$repo/releases/latest"

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Tome-Android-Reader")
                .build()

            val response = httpClient.newCall(request).execute()
            val now = System.currentTimeMillis()
            prefs.edit().putLong(PREF_LAST_CHECK_TIME, now).apply()

            if (!response.isSuccessful) {
                val code = response.code
                val errorMsg = if (code == 404) {
                    "No releases found for GitHub repo: $repo"
                } else {
                    "GitHub API response: HTTP $code"
                }
                _checkStatusMessage.value = errorMsg
                _isChecking.value = false
                return@withContext Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)

            val tagName = json.optString("tag_name", "")
            val releaseTitle = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "Bug fixes and performance improvements.")
            val publishedAt = json.optString("published_at", "")
            val htmlUrl = json.optString("html_url", "https://github.com/$repo/releases")

            // Parse clean version name (e.g., "v1.2.0" -> "1.2.0")
            val cleanVersion = tagName.trimStart('v', 'V')

            // Compare versions
            val currentVersion = BuildConfig.VERSION_NAME
            val isNewer = isVersionNewer(cleanVersion, currentVersion)

            // Look for APK in assets
            var apkDownloadUrl = ""
            var apkFileName = "Tome-$tagName.apk"
            var apkSizeBytes = 0L

            val assets = json.optJSONArray("assets")
            if (assets != null && assets.length() > 0) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url", "")
                        apkFileName = name
                        apkSizeBytes = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            // Fallback: If release has no direct APK uploaded, use release page
            if (apkDownloadUrl.isBlank()) {
                apkDownloadUrl = htmlUrl
            }

            val dismissedTag = prefs.getString(PREF_DISMISSED_TAG, "")
            val shouldNotify = isNewer && (forceCheck || dismissedTag != tagName)

            val releaseInfo = AppReleaseInfo(
                tagName = tagName,
                versionName = cleanVersion,
                releaseTitle = releaseTitle.ifBlank { "Tome v$cleanVersion" },
                releaseNotes = releaseNotes,
                publishedAt = publishedAt,
                downloadUrl = apkDownloadUrl,
                apkFileName = apkFileName,
                apkSizeBytes = apkSizeBytes,
                isNewer = isNewer,
                htmlUrl = htmlUrl
            )

            if (shouldNotify) {
                _latestRelease.value = releaseInfo
                _checkStatusMessage.value = "New version available: $tagName"
            } else if (!isNewer) {
                _checkStatusMessage.value = "You are on the latest version ($currentVersion)"
            }

            _isChecking.value = false
            Result.success(if (isNewer) releaseInfo else null)
        } catch (e: Exception) {
            _isChecking.value = false
            _checkStatusMessage.value = "Update check failed: ${e.localizedMessage}"
            Result.failure(e)
        }
    }

    /**
     * Downloads the APK file from GitHub release asset and notifies download progress.
     */
    suspend fun downloadAndInstallUpdate(releaseInfo: AppReleaseInfo): Result<File> = withContext(Dispatchers.IO) {
        if (!releaseInfo.downloadUrl.endsWith(".apk", ignoreCase = true) && !releaseInfo.downloadUrl.contains("/download/")) {
            // URL is web release page, launch in browser
            launchBrowserUrl(releaseInfo.htmlUrl)
            return@withContext Result.failure(Exception("Direct APK asset not attached. Opening release web page."))
        }

        _downloadState.value = UpdateDownloadState.Downloading(0, 0L, releaseInfo.apkSizeBytes)

        try {
            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) updateDir.mkdirs()

            val apkFile = File(updateDir, releaseInfo.apkFileName)
            if (apkFile.exists()) apkFile.delete()

            val request = Request.Builder()
                .url(releaseInfo.downloadUrl)
                .header("User-Agent", "Tome-Android-Reader")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = "Download failed with HTTP ${response.code}"
                _downloadState.value = UpdateDownloadState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = if (releaseInfo.apkSizeBytes > 0) releaseInfo.apkSizeBytes else body.contentLength()

            val input = body.byteStream()
            val output = FileOutputStream(apkFile)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L
            var lastReportPercent = -1

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                if (totalBytes > 0) {
                    val percent = ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                    if (percent != lastReportPercent) {
                        lastReportPercent = percent
                        _downloadState.value = UpdateDownloadState.Downloading(percent, totalRead, totalBytes)
                    }
                }
            }

            output.flush()
            output.close()
            input.close()

            _downloadState.value = UpdateDownloadState.ReadyToInstall(apkFile)

            // Prompt installation on UI/Main thread
            withContext(Dispatchers.Main) {
                installApk(apkFile)
            }

            Result.success(apkFile)
        } catch (e: Exception) {
            val err = "Download error: ${e.localizedMessage}"
            _downloadState.value = UpdateDownloadState.Error(err)
            Result.failure(e)
        }
    }

    /**
     * Prompts the Android Package Installer to install the updated APK.
     */
    fun installApk(apkFile: File) {
        try {
            // Android 8.0+ Unknown App Sources Permission check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(manageIntent)
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            _downloadState.value = UpdateDownloadState.Error("Install launch failed: ${e.localizedMessage}")
        }
    }

    fun launchBrowserUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    /**
     * Simulates a new GitHub release notification for live UI demonstration and testing.
     */
    fun simulateNewRelease() {
        val current = BuildConfig.VERSION_NAME
        val nextVersion = incrementVersion(current)
        val mockRelease = AppReleaseInfo(
            tagName = "v$nextVersion",
            versionName = nextVersion,
            releaseTitle = "Tome v$nextVersion Update (Bangla Voice & GitHub Release)",
            releaseNotes = """
                ### What's New in v$nextVersion:
                • Enhanced natural female and male TTS voice selection for English & Bangla.
                • Seamless in-app GitHub Release updates with automatic APK download.
                • Improved PDF note-taking with inline Gemini AI translations.
                • Performance optimizations for fast page flipping and rendering.
            """.trimIndent(),
            publishedAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            downloadUrl = "https://github.com/${getGitHubRepo()}/releases/download/v$nextVersion/Tome-v$nextVersion.apk",
            apkFileName = "Tome-v$nextVersion.apk",
            apkSizeBytes = 18_450_000L,
            isNewer = true,
            htmlUrl = "https://github.com/${getGitHubRepo()}/releases"
        )
        _latestRelease.value = mockRelease
        _checkStatusMessage.value = "Simulated update detected: v$nextVersion"
    }

    private fun incrementVersion(ver: String): String {
        val parts = ver.split(".").mapNotNull { it.toIntOrNull() }.toMutableList()
        if (parts.isEmpty()) return "1.1"
        parts[parts.lastIndex] = parts.last() + 1
        return parts.joinToString(".")
    }

    /**
     * Compares two semantic version strings (e.g. "1.1.0" vs "1.0").
     * Returns true if remoteVersion is strictly newer than currentVersion.
     */
    fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        try {
            val remoteParts = remoteVersion.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            val currentParts = currentVersion.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (_: Exception) {
            return remoteVersion != currentVersion
        }
    }
}

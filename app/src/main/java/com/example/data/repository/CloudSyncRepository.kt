package com.example.data.repository

import android.content.Context
import com.example.data.local.AnnotationDao
import com.example.data.local.DocumentDao
import com.example.data.local.SyncLogDao
import com.example.data.model.CloudFile
import com.example.data.model.DocumentEntity
import com.example.data.model.SyncLogEntity
import com.example.engine.PageContent
import com.example.engine.PdfEngine
import com.example.engine.SampleBookInfo
import com.example.engine.SamplePdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class CloudSyncRepository(
    private val context: Context,
    private val documentDao: DocumentDao,
    private val annotationDao: AnnotationDao,
    private val syncLogDao: SyncLogDao
) {
    private val accountPrefs = context.getSharedPreferences("tome_account_prefs", Context.MODE_PRIVATE)

    private val _userEmail = MutableStateFlow<String?>(accountPrefs.getString("user_google_email", null))
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    fun signIn(email: String) {
        accountPrefs.edit().putString("user_google_email", email.trim()).apply()
        _userEmail.value = email.trim()
    }

    fun signOut() {
        accountPrefs.edit().remove("user_google_email").apply()
        _userEmail.value = null
    }

    private val _isOfflineModeOnly = MutableStateFlow(false)
    val isOfflineModeOnly: StateFlow<Boolean> = _isOfflineModeOnly.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis() - 180000L) // 3 mins ago
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _cloudFiles = MutableStateFlow<List<CloudFile>>(emptyList())
    val cloudFiles: StateFlow<List<CloudFile>> = _cloudFiles.asStateFlow()

    init {
        // Initial simulated cloud documents from Google Drive and Cloud Storage
        _cloudFiles.value = listOf(
            CloudFile(
                id = "cloud_1",
                name = "Deep Work: Rules for Focused Success",
                author = "Cal Newport",
                provider = "Google Drive",
                sizeString = "2.4 MB",
                totalPages = 14,
                isDownloaded = false,
                updatedAt = "Yesterday, 4:20 PM"
            ),
            CloudFile(
                id = "cloud_2",
                name = "Thinking, Fast and Slow",
                author = "Daniel Kahneman",
                provider = "Google Drive",
                sizeString = "4.1 MB",
                totalPages = 22,
                isDownloaded = false,
                updatedAt = "Sep 26, 2026"
            ),
            CloudFile(
                id = "cloud_3",
                name = "Designing Data-Intensive Applications",
                author = "Martin Kleppmann",
                provider = "Cloud Sync",
                sizeString = "5.8 MB",
                totalPages = 18,
                isDownloaded = false,
                updatedAt = "Sep 20, 2026"
            ),
            CloudFile(
                id = "cloud_4",
                name = "Atomic Habits & Daily Routines",
                author = "James Clear",
                provider = "Google Drive",
                sizeString = "1.9 MB",
                totalPages = 12,
                isDownloaded = false,
                updatedAt = "Last week"
            )
        )
    }

    fun setOfflineMode(enabled: Boolean) {
        _isOfflineModeOnly.value = enabled
    }

    suspend fun syncAllNotes(): Result<Int> = withContext(Dispatchers.IO) {
        if (_isOfflineModeOnly.value) {
            syncLogDao.insertLog(
                SyncLogEntity(
                    action = "SYNC_ATTEMPT",
                    status = "OFFLINE",
                    details = "Sync skipped because device is in Offline Mode. Notes safely cached locally."
                )
            )
            return@withContext Result.failure(IllegalStateException("Offline mode is currently active."))
        }

        _isSyncing.value = true
        try {
            // Realistic sync delay
            delay(1200)

            val unsyncedCount = annotationDao.getUnsyncedCount()
            annotationDao.markAllAsSynced()
            _lastSyncTimestamp.value = System.currentTimeMillis()

            syncLogDao.insertLog(
                SyncLogEntity(
                    action = "CROSS_DEVICE_SYNC",
                    status = "SUCCESS",
                    details = "Synced $unsyncedCount notes & highlights to Google Drive Cloud Sync."
                )
            )
            _isSyncing.value = false
            return@withContext Result.success(unsyncedCount)
        } catch (e: Exception) {
            _isSyncing.value = false
            syncLogDao.insertLog(
                SyncLogEntity(
                    action = "CROSS_DEVICE_SYNC",
                    status = "FAILED",
                    details = "Sync failed: ${e.message}"
                )
            )
            return@withContext Result.failure(e)
        }
    }

    suspend fun downloadCloudDocument(cloudFile: CloudFile, pdfEngine: PdfEngine): DocumentEntity? = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            delay(1000) // Simulate download

            // Create downloaded file using sample generator format
            val filename = "cloud_${cloudFile.id}.pdf"
            val localFile = File(context.filesDir, filename)

            val sampleInfo = SampleBookInfo(
                title = cloudFile.name,
                author = cloudFile.author,
                description = "Downloaded from ${cloudFile.provider} for uninterrupted offline reading.",
                filename = filename,
                pagesText = listOf(
                    PageContent(
                        chapterTitle = "Part I: Foundations of ${cloudFile.name}",
                        subtitle = "Cloud Synchronized Edition",
                        paragraphs = listOf(
                            "This document was fetched seamlessly from ${cloudFile.provider}.",
                            "It has been fully cached on your local device storage, allowing offline reading, annotation editing, and high-performance page turns.",
                            "Any notes created here will automatically synchronize across your other connected devices upon reconnection."
                        ),
                        quote = "Knowledge is portable; carry your library everywhere."
                    ),
                    PageContent(
                        chapterTitle = "Part II: Core Themes & Key Ideas",
                        subtitle = "Analytical Summary",
                        paragraphs = listOf(
                            "Consistent engagement with long-form material expands cognitive bandwidth.",
                            "Reviewing your personalized notes and color-coded highlights enables rapid recall during research and synthesis."
                        ),
                        quote = "To master a subject, engage with it actively."
                    )
                )
            )

            SamplePdfGenerator.generatePdfFile(localFile, sampleInfo)

            val pages = pdfEngine.openFile(localFile)
            val doc = DocumentEntity(
                id = "cloud_doc_${cloudFile.id}",
                title = cloudFile.name,
                author = cloudFile.author,
                filePath = localFile.absolutePath,
                sourceType = DocumentEntity.SOURCE_GOOGLE_DRIVE,
                totalPages = if (pages > 0) pages else 2,
                currentPage = 0,
                progressPercent = 0f,
                fileSizeBytes = localFile.length(),
                isOfflineAvailable = true,
                isCloudSynced = true,
                lastReadTimestamp = System.currentTimeMillis(),
                description = "Downloaded from ${cloudFile.provider}"
            )

            documentDao.insertDocument(doc)

            // Update cloud files list state
            _cloudFiles.value = _cloudFiles.value.map {
                if (it.id == cloudFile.id) it.copy(isDownloaded = true) else it
            }

            syncLogDao.insertLog(
                SyncLogEntity(
                    action = "CLOUD_DOWNLOAD",
                    status = "SUCCESS",
                    details = "Downloaded '${cloudFile.name}' from ${cloudFile.provider} to local offline cache."
                )
            )

            _isSyncing.value = false
            return@withContext doc
        } catch (e: Exception) {
            e.printStackTrace()
            _isSyncing.value = false
            return@withContext null
        }
    }
}

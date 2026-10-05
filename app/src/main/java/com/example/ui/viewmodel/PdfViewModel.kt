package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.CloudFile
import com.example.data.model.DocumentEntity
import com.example.data.model.PageElementEntity
import com.example.data.model.ReadingTheme
import com.example.data.model.ThemeMode
import com.example.data.model.SyncLogEntity
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.PdfRepository
import com.example.engine.PdfEngine
import com.example.engine.SearchMatch
import com.example.util.AmbientLightLevel
import com.example.util.LightSensorManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val pdfEngine = PdfEngine()
    val repository = PdfRepository(
        context = application,
        documentDao = database.documentDao(),
        bookmarkDao = database.bookmarkDao(),
        annotationDao = database.annotationDao(),
        syncLogDao = database.syncLogDao(),
        pageElementDao = database.pageElementDao()
    )
    val cloudSyncRepository = CloudSyncRepository(
        context = application,
        documentDao = database.documentDao(),
        annotationDao = database.annotationDao(),
        syncLogDao = database.syncLogDao()
    )

    // Data flows
    val libraryDocuments: StateFlow<List<DocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDocuments: StateFlow<List<DocumentEntity>> = repository.recentDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineDocuments: StateFlow<List<DocumentEntity>> = repository.offlineDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAnnotationsAcrossDocs: StateFlow<List<AnnotationEntity>> = repository.allAnnotations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSyncLogs: StateFlow<List<SyncLogEntity>> = repository.recentSyncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isOfflineModeOnly: StateFlow<Boolean> = cloudSyncRepository.isOfflineModeOnly
    val isSyncing: StateFlow<Boolean> = cloudSyncRepository.isSyncing
    val lastSyncTimestamp: StateFlow<Long> = cloudSyncRepository.lastSyncTimestamp
    val cloudFiles: StateFlow<List<CloudFile>> = cloudSyncRepository.cloudFiles
    val userEmail: StateFlow<String?> = cloudSyncRepository.userEmail

    fun signInUser(email: String) {
        cloudSyncRepository.signIn(email)
    }

    fun signOutUser() {
        cloudSyncRepository.signOut()
    }

    // Active Reader State
    private val _activeDocument = MutableStateFlow<DocumentEntity?>(null)
    val activeDocument: StateFlow<DocumentEntity?> = _activeDocument.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _totalPages = MutableStateFlow(1)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _currentPageBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPageBitmap: StateFlow<Bitmap?> = _currentPageBitmap.asStateFlow()

    private val _currentAnnotations = MutableStateFlow<List<AnnotationEntity>>(emptyList())
    val currentAnnotations: StateFlow<List<AnnotationEntity>> = _currentAnnotations.asStateFlow()

    private val _currentBookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val currentBookmarks: StateFlow<List<BookmarkEntity>> = _currentBookmarks.asStateFlow()

    private val _isCurrentPageBookmarked = MutableStateFlow(false)
    val isCurrentPageBookmarked: StateFlow<Boolean> = _isCurrentPageBookmarked.asStateFlow()

    // Reading display preferences
    private val _readingTheme = MutableStateFlow(ReadingTheme.SEPIA)
    val readingTheme: StateFlow<ReadingTheme> = _readingTheme.asStateFlow()

    private val _isTwoPageSpread = MutableStateFlow(false)
    val isTwoPageSpread: StateFlow<Boolean> = _isTwoPageSpread.asStateFlow()

    private val _isControlsVisible = MutableStateFlow(true)
    val isControlsVisible: StateFlow<Boolean> = _isControlsVisible.asStateFlow()

    private val _isHighlightModeActive = MutableStateFlow(false)
    val isHighlightModeActive: StateFlow<Boolean> = _isHighlightModeActive.asStateFlow()

    private val _selectedHighlightColor = MutableStateFlow("#FFEB3B") // Amber default
    val selectedHighlightColor: StateFlow<String> = _selectedHighlightColor.asStateFlow()

    // Full-Text Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchMatch>>(emptyList())
    val searchResults: StateFlow<List<SearchMatch>> = _searchResults.asStateFlow()

    private val _currentSearchMatchIndex = MutableStateFlow(0)
    val currentSearchMatchIndex: StateFlow<Int> = _currentSearchMatchIndex.asStateFlow()

    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    // Reading ruler guide
    private val _isReadingRulerEnabled = MutableStateFlow(false)
    val isReadingRulerEnabled: StateFlow<Boolean> = _isReadingRulerEnabled.asStateFlow()

    private val _readingRulerPositionRatio = MutableStateFlow(0.35f)
    val readingRulerPositionRatio: StateFlow<Float> = _readingRulerPositionRatio.asStateFlow()

    // Active annotation dialog / editor
    private val _editingAnnotation = MutableStateFlow<AnnotationEntity?>(null)
    val editingAnnotation: StateFlow<AnnotationEntity?> = _editingAnnotation.asStateFlow()

    private val _isAddingNoteDialog = MutableStateFlow(false)
    val isAddingNoteDialog: StateFlow<Boolean> = _isAddingNoteDialog.asStateFlow()

    private val _pendingHighlightText = MutableStateFlow("")
    val pendingHighlightText: StateFlow<String> = _pendingHighlightText.asStateFlow()

    private val _pendingHighlightTopRatio = MutableStateFlow(0.25f)
    val pendingHighlightTopRatio: StateFlow<Float> = _pendingHighlightTopRatio.asStateFlow()

    // Page Elements State (Text blocks, images, stamps)
    private val _currentElements = MutableStateFlow<List<PageElementEntity>>(emptyList())
    val currentElements: StateFlow<List<PageElementEntity>> = _currentElements.asStateFlow()

    // Dedicated Bookmarks Panel
    private val _isBookmarksPanelOpen = MutableStateFlow(false)
    val isBookmarksPanelOpen: StateFlow<Boolean> = _isBookmarksPanelOpen.asStateFlow()

    // Text Selection & Highlighting with Custom Colors Dialog
    private val _isTextSelectionHighlightDialogOpen = MutableStateFlow(false)
    val isTextSelectionHighlightDialogOpen: StateFlow<Boolean> = _isTextSelectionHighlightDialogOpen.asStateFlow()

    // Document Text / Element Editing Mode
    private val _isEditElementsMode = MutableStateFlow(false)
    val isEditElementsMode: StateFlow<Boolean> = _isEditElementsMode.asStateFlow()

    private val _isAddElementDialogOpen = MutableStateFlow(false)
    val isAddElementDialogOpen: StateFlow<Boolean> = _isAddElementDialogOpen.asStateFlow()

    private val _editingElement = MutableStateFlow<PageElementEntity?>(null)
    val editingElement: StateFlow<PageElementEntity?> = _editingElement.asStateFlow()

    // Status snackbar message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Reading Session Timer & Progress State
    private var sessionTimerJob: Job? = null
    private var unpersistedSessionSeconds: Long = 0L
    private val _sessionVisitedPages = mutableSetOf<Int>()

    private val _sessionDurationSeconds = MutableStateFlow(0L)
    val sessionDurationSeconds: StateFlow<Long> = _sessionDurationSeconds.asStateFlow()

    private val _isSessionTimerRunning = MutableStateFlow(true)
    val isSessionTimerRunning: StateFlow<Boolean> = _isSessionTimerRunning.asStateFlow()

    private val _pagesReadThisSession = MutableStateFlow(1)
    val pagesReadThisSession: StateFlow<Int> = _pagesReadThisSession.asStateFlow()

    private val _readingGoalMinutes = MutableStateFlow(20)
    val readingGoalMinutes: StateFlow<Int> = _readingGoalMinutes.asStateFlow()

    private val _isReadingSessionSheetOpen = MutableStateFlow(false)
    val isReadingSessionSheetOpen: StateFlow<Boolean> = _isReadingSessionSheetOpen.asStateFlow()

    // Page flip animation preference & direction
    private val readerPrefs = application.getSharedPreferences("tome_reader_prefs", android.content.Context.MODE_PRIVATE)
    private val _isPageFlipEnabled = MutableStateFlow(readerPrefs.getBoolean("pref_page_flip_enabled", true))
    val isPageFlipEnabled: StateFlow<Boolean> = _isPageFlipEnabled.asStateFlow()

    // System-wide Dark Theme mode (System Default, Light Mode, Dark Theme)
    private val _themeMode = MutableStateFlow(
        try {
            val savedMode = readerPrefs.getString("pref_app_theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            ThemeMode.valueOf(savedMode)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        readerPrefs.edit().putString("pref_app_theme_mode", mode.name).apply()
        _statusMessage.value = when (mode) {
            ThemeMode.SYSTEM -> "Theme: Follow System"
            ThemeMode.LIGHT -> "Theme: Light Mode"
            ThemeMode.DARK -> "Theme: Dark Mode (Low Light)"
        }
    }

    fun toggleDarkTheme() {
        val nextMode = when (_themeMode.value) {
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
        setThemeMode(nextMode)
    }

    // =========================================================================
    // SYSTEM LIGHT SENSOR & AUTO-BRIGHTNESS ADJUSTMENT
    // =========================================================================
    val lightSensorManager = LightSensorManager(application)

    private val _isAutoBrightnessEnabled = MutableStateFlow(readerPrefs.getBoolean("pref_auto_brightness_enabled", true))
    val isAutoBrightnessEnabled: StateFlow<Boolean> = _isAutoBrightnessEnabled.asStateFlow()

    private val _manualBrightness = MutableStateFlow(readerPrefs.getFloat("pref_manual_brightness", 0.65f))
    val manualBrightness: StateFlow<Float> = _manualBrightness.asStateFlow()

    private val _autoComplementDarkTheme = MutableStateFlow(readerPrefs.getBoolean("pref_auto_complement_dark_theme", true))
    val autoComplementDarkTheme: StateFlow<Boolean> = _autoComplementDarkTheme.asStateFlow()

    val currentLux: StateFlow<Float> = lightSensorManager.currentLux
    val ambientLightLevel: StateFlow<AmbientLightLevel> = lightSensorManager.ambientLightLevel
    val isLightSensorAvailable: Boolean = lightSensorManager.isSensorAvailable

    private val _currentAppBrightness = MutableStateFlow(
        if (readerPrefs.getBoolean("pref_auto_brightness_enabled", true))
            lightSensorManager.calculatedBrightness.value
        else
            readerPrefs.getFloat("pref_manual_brightness", 0.65f)
    )
    val currentAppBrightness: StateFlow<Float> = _currentAppBrightness.asStateFlow()

    fun setAutoBrightnessEnabled(enabled: Boolean) {
        _isAutoBrightnessEnabled.value = enabled
        readerPrefs.edit().putBoolean("pref_auto_brightness_enabled", enabled).apply()
        if (enabled) {
            lightSensorManager.startListening()
            _currentAppBrightness.value = lightSensorManager.calculatedBrightness.value
            _statusMessage.value = "Auto-brightness: ON (Light sensor active)"
        } else {
            lightSensorManager.stopListening()
            _currentAppBrightness.value = _manualBrightness.value
            _statusMessage.value = "Auto-brightness: OFF (Manual control)"
        }
    }

    fun setManualBrightness(brightness: Float) {
        val clamped = brightness.coerceIn(0.08f, 1.0f)
        _manualBrightness.value = clamped
        readerPrefs.edit().putFloat("pref_manual_brightness", clamped).apply()
        if (!_isAutoBrightnessEnabled.value) {
            _currentAppBrightness.value = clamped
        }
    }

    fun setAutoComplementDarkTheme(enabled: Boolean) {
        _autoComplementDarkTheme.value = enabled
        readerPrefs.edit().putBoolean("pref_auto_complement_dark_theme", enabled).apply()
        _statusMessage.value = if (enabled) "Auto-complement Dark Theme enabled" else "Auto-complement Dark Theme disabled"
    }

    fun simulateLightSensorLux(lux: Float?) {
        lightSensorManager.setSimulatedLux(lux)
        if (lux != null) {
            _statusMessage.value = "Simulated Light: ${lux.toInt()} lux"
        } else {
            _statusMessage.value = "Light Sensor: Real hardware mode"
        }
    }

    init {
        viewModelScope.launch {
            repository.initializeSamplesIfNeeded(pdfEngine)
        }
        viewModelScope.launch {
            delay(1500)
            appUpdateManager.checkForUpdates(forceCheck = false)
        }

        // Initialize light sensor and auto-brightness reactive loop
        if (_isAutoBrightnessEnabled.value) {
            lightSensorManager.startListening()
        }

        viewModelScope.launch {
            lightSensorManager.calculatedBrightness.collect { targetBrightness ->
                if (_isAutoBrightnessEnabled.value) {
                    _currentAppBrightness.value = targetBrightness
                }
            }
        }

        viewModelScope.launch {
            lightSensorManager.ambientLightLevel.collect { level ->
                if (_isAutoBrightnessEnabled.value && _autoComplementDarkTheme.value) {
                    if (level == AmbientLightLevel.DARK) {
                        // In low ambient light (<15 lux), ensure dark theme is active to prevent eye fatigue
                        if (_themeMode.value == ThemeMode.SYSTEM || _themeMode.value == ThemeMode.LIGHT) {
                            _themeMode.value = ThemeMode.DARK
                        }
                    }
                }
            }
        }
    }

    fun openDocument(doc: DocumentEntity, startPage: Int = -1) {
        viewModelScope.launch {
            val file = File(doc.filePath)
            if (!file.exists()) {
                _statusMessage.value = "File not found locally."
                return@launch
            }

            val pages = pdfEngine.openFile(file)
            _totalPages.value = if (pages > 0) pages else doc.totalPages
            _activeDocument.value = doc

            val targetPage = if (startPage >= 0) startPage else doc.currentPage
            _sessionVisitedPages.clear()
            _sessionVisitedPages.add(targetPage)
            _pagesReadThisSession.value = 1
            _sessionDurationSeconds.value = 0L
            _isSessionTimerRunning.value = true
            unpersistedSessionSeconds = 0L

            repository.updateLastReadTimestamp(doc.id, System.currentTimeMillis())

            goToPage(targetPage.coerceIn(0, (_totalPages.value - 1).coerceAtLeast(0)))

            // Start Reading Session Timer loop
            sessionTimerJob?.cancel()
            sessionTimerJob = launch {
                while (true) {
                    delay(1000)
                    if (_isSessionTimerRunning.value && _activeDocument.value != null) {
                        _sessionDurationSeconds.value += 1
                        unpersistedSessionSeconds += 1

                        // Periodically sync reading duration to database every 30 seconds
                        if (unpersistedSessionSeconds >= 30) {
                            val activeId = _activeDocument.value?.id
                            val toFlush = unpersistedSessionSeconds
                            unpersistedSessionSeconds = 0L
                            if (activeId != null) {
                                repository.incrementReadingTime(activeId, toFlush)
                            }
                        }
                    }
                }
            }

            // Observe bookmarks and annotations for this document
            launch {
                repository.getBookmarksForDocument(doc.id).collect { bookmarks ->
                    _currentBookmarks.value = bookmarks
                    checkBookmarkStatus()
                }
            }
            launch {
                repository.getAnnotationsForDocument(doc.id).collect { annotations ->
                    _currentAnnotations.value = annotations
                }
            }
            launch {
                repository.getElementsForDocument(doc.id).collect { elements ->
                    _currentElements.value = elements
                }
            }
        }
    }

    fun closeDocument() {
        closeVoiceReader()
        sessionTimerJob?.cancel()
        sessionTimerJob = null
        _isSessionTimerRunning.value = false
        _isBookmarksPanelOpen.value = false
        _isEditElementsMode.value = false
        _isAddElementDialogOpen.value = false
        _isTextSelectionHighlightDialogOpen.value = false
        _currentElements.value = emptyList()

        // Flush any remaining session reading time
        val activeDocId = _activeDocument.value?.id
        val remainingToFlush = unpersistedSessionSeconds
        unpersistedSessionSeconds = 0L
        if (activeDocId != null && remainingToFlush > 0) {
            viewModelScope.launch {
                repository.incrementReadingTime(activeDocId, remainingToFlush)
            }
        }

        _activeDocument.value = null
        _currentPageBitmap.value = null
        _isReadingSessionSheetOpen.value = false
        pdfEngine.close()
    }

    // Reading Session Timer Controls
    fun toggleSessionTimer() {
        _isSessionTimerRunning.value = !_isSessionTimerRunning.value
    }

    fun pauseSessionTimer() {
        _isSessionTimerRunning.value = false
    }

    fun resumeSessionTimer() {
        _isSessionTimerRunning.value = true
    }

    fun resetSessionTimer() {
        // Flush current elapsed time first
        val activeDocId = _activeDocument.value?.id
        val toFlush = unpersistedSessionSeconds
        unpersistedSessionSeconds = 0L
        if (activeDocId != null && toFlush > 0) {
            viewModelScope.launch {
                repository.incrementReadingTime(activeDocId, toFlush)
            }
        }
        _sessionDurationSeconds.value = 0L
        _isSessionTimerRunning.value = true
    }

    fun openReadingSessionSheet() {
        _isReadingSessionSheetOpen.value = true
    }

    fun closeReadingSessionSheet() {
        _isReadingSessionSheetOpen.value = false
    }

    fun setReadingGoalMinutes(minutes: Int) {
        _readingGoalMinutes.value = minutes.coerceIn(5, 180)
    }

    fun formatTimerDisplay(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formatHumanDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hrs > 0 -> "${hrs}h ${mins}m"
            mins > 0 -> "${mins}m ${secs}s"
            else -> "${secs}s"
        }
    }

    fun getEstimatedRemainingMinutes(): Int {
        val total = _totalPages.value
        val current = _currentPageIndex.value
        val remainingPages = (total - current - 1).coerceAtLeast(0)
        if (remainingPages == 0) return 0

        val pagesRead = _pagesReadThisSession.value.coerceAtLeast(1)
        val elapsedMinutes = (_sessionDurationSeconds.value / 60f).coerceAtLeast(0.5f)
        val minutesPerPage = (elapsedMinutes / pagesRead).coerceIn(0.5f, 5.0f)
        return (remainingPages * minutesPerPage).toInt().coerceAtLeast(1)
    }

    fun goToPage(page: Int) {
        val total = _totalPages.value
        val clamped = page.coerceIn(0, (total - 1).coerceAtLeast(0))
        if (clamped != _currentPageIndex.value) {
            _lastPageTurnDelta.value = if (clamped > _currentPageIndex.value) 1 else -1
        }

        // Immediately update bitmap from cache if available; if not yet cached, set null so old page's bitmap never leaks
        val cachedBitmap = pdfEngine.getCachedBitmap(clamped)
        _currentPageBitmap.value = cachedBitmap

        _currentPageIndex.value = clamped

        _sessionVisitedPages.add(clamped)
        _pagesReadThisSession.value = _sessionVisitedPages.size

        if (_isVoicePlayerVisible.value) {
            val docTitle = _activeDocument.value?.title ?: "Document"
            val pageText = pdfEngine.getPageText(clamped, docTitle, total)
            val wasPlaying = isVoiceReadingPlaying.value
            voiceReaderEngine.loadText(pageText, docTitle, clamped + 1)
            if (wasPlaying) {
                voiceReaderEngine.play()
            }
        }

        viewModelScope.launch {
            val bitmap = cachedBitmap ?: pdfEngine.renderPage(clamped)
            if (bitmap != null && _currentPageIndex.value == clamped && _currentPageBitmap.value !== bitmap) {
                _currentPageBitmap.value = bitmap
            }
            checkBookmarkStatus()

            // Pre-fetch adjacent pages into cache so next page turns are instantaneous
            if (clamped + 1 < total && !pdfEngine.isPageCached(clamped + 1)) {
                pdfEngine.renderPage(clamped + 1)
            }
            if (clamped - 1 >= 0 && !pdfEngine.isPageCached(clamped - 1)) {
                pdfEngine.renderPage(clamped - 1)
            }
            if (clamped + 2 < total && !pdfEngine.isPageCached(clamped + 2)) {
                pdfEngine.renderPage(clamped + 2)
            }
            if (clamped - 2 >= 0 && !pdfEngine.isPageCached(clamped - 2)) {
                pdfEngine.renderPage(clamped - 2)
            }

            _activeDocument.value?.let { doc ->
                repository.updateReadingProgress(doc.id, clamped, total)
            }
        }
    }

    fun nextPage() {
        if (_currentPageIndex.value < _totalPages.value - 1) {
            goToPage(_currentPageIndex.value + 1)
        }
    }

    fun prevPage() {
        if (_currentPageIndex.value > 0) {
            goToPage(_currentPageIndex.value - 1)
        }
    }

    private fun checkBookmarkStatus() {
        val docId = _activeDocument.value?.id ?: return
        val current = _currentPageIndex.value
        _isCurrentPageBookmarked.value = _currentBookmarks.value.any { it.documentId == docId && it.pageNumber == current }
    }

    fun toggleBookmark() {
        val doc = _activeDocument.value ?: return
        val page = _currentPageIndex.value
        viewModelScope.launch {
            val title = "Page ${page + 1} Bookmark"
            val wasBookmarked = _isCurrentPageBookmarked.value
            repository.toggleBookmark(doc.id, page, title)
            _isCurrentPageBookmarked.value = !wasBookmarked
            _statusMessage.value = if (!wasBookmarked) "Page ${page + 1} added to 'Bookmarks'" else "Page ${page + 1} removed from 'Bookmarks'"
            checkBookmarkStatus()
        }
    }

    fun openBookmarksPanel() {
        _isBookmarksPanelOpen.value = true
    }

    fun closeBookmarksPanel() {
        _isBookmarksPanelOpen.value = false
    }

    fun addCustomBookmark(page: Int, title: String, note: String) {
        val doc = _activeDocument.value ?: return
        viewModelScope.launch {
            repository.addBookmark(doc.id, page, title, note)
            checkBookmarkStatus()
            _statusMessage.value = "Page ${page + 1} bookmark saved"
        }
    }

    fun updateBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            repository.updateBookmark(bookmark)
            checkBookmarkStatus()
            _statusMessage.value = "Bookmark updated"
        }
    }

    fun deleteBookmark(id: String) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
            checkBookmarkStatus()
            _statusMessage.value = "Bookmark removed"
        }
    }

    // Text Selection & Highlighting with Custom Colors
    fun openTextSelectionHighlightDialog(initialText: String = "", topRatio: Float = 0.35f) {
        _pendingHighlightText.value = initialText
        _pendingHighlightTopRatio.value = topRatio
        _isTextSelectionHighlightDialogOpen.value = true
    }

    fun closeTextSelectionHighlightDialog() {
        _isTextSelectionHighlightDialogOpen.value = false
    }

    fun saveHighlightWithDetails(
        text: String,
        colorHex: String,
        note: String,
        tag: String,
        topRatio: Float = _pendingHighlightTopRatio.value
    ) {
        val doc = _activeDocument.value ?: return
        val page = _currentPageIndex.value
        viewModelScope.launch {
            repository.addAnnotation(
                docId = doc.id,
                page = page,
                text = text,
                colorHex = colorHex,
                note = note,
                tag = tag,
                topRatio = topRatio
            )
            _selectedHighlightColor.value = colorHex
            _isTextSelectionHighlightDialogOpen.value = false
            _statusMessage.value = "Highlight saved in custom color"
        }
    }

    fun getPageTextSegments(pageIndex: Int): List<String> {
        val pageText = pdfEngine.getPageText(pageIndex)
        if (pageText.isBlank()) return emptyList()
        return pageText.split(Regex("[\\r\\n]+|(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length > 10 }
    }

    // Document Text / Element Editing Mode
    fun toggleEditElementsMode() {
        _isEditElementsMode.value = !_isEditElementsMode.value
        _statusMessage.value = if (_isEditElementsMode.value) "Edit Mode Active: Drag or tap to modify elements" else "Edit Mode Disabled"
    }

    fun setEditElementsMode(enabled: Boolean) {
        _isEditElementsMode.value = enabled
    }

    fun openAddElementDialog() {
        _isAddElementDialogOpen.value = true
    }

    fun closeAddElementDialog() {
        _isAddElementDialogOpen.value = false
    }

    fun addTextElement(
        text: String,
        colorHex: String,
        bgHex: String,
        fontSize: Int,
        isBold: Boolean,
        isItalic: Boolean
    ) {
        val doc = _activeDocument.value ?: return
        val page = _currentPageIndex.value
        val element = PageElementEntity(
            documentId = doc.id,
            pageNumber = page,
            elementType = "TEXT",
            content = text,
            xRatio = 0.25f,
            yRatio = 0.35f,
            colorHex = colorHex,
            backgroundColorHex = bgHex,
            fontSize = fontSize,
            isBold = isBold,
            isItalic = isItalic
        )
        viewModelScope.launch {
            repository.addPageElement(element)
            _isEditElementsMode.value = true
            _statusMessage.value = "Text element added. Drag to reposition!"
        }
    }

    fun addImageElement(uriString: String) {
        val doc = _activeDocument.value ?: return
        val page = _currentPageIndex.value
        val element = PageElementEntity(
            documentId = doc.id,
            pageNumber = page,
            elementType = "IMAGE",
            content = uriString,
            xRatio = 0.35f,
            yRatio = 0.35f,
            widthRatio = 0.35f,
            heightRatio = 0.2f
        )
        viewModelScope.launch {
            repository.addPageElement(element)
            _isEditElementsMode.value = true
            _statusMessage.value = "Image added to page. Drag to reposition!"
        }
    }

    fun addStampElement(stampTitle: String, colorHex: String) {
        val doc = _activeDocument.value ?: return
        val page = _currentPageIndex.value
        val element = PageElementEntity(
            documentId = doc.id,
            pageNumber = page,
            elementType = "STAMP",
            content = stampTitle,
            colorHex = colorHex,
            xRatio = 0.4f,
            yRatio = 0.2f
        )
        viewModelScope.launch {
            repository.addPageElement(element)
            _isEditElementsMode.value = true
            _statusMessage.value = "Stamp placed on page"
        }
    }

    fun updateElementPosition(element: PageElementEntity, newXRatio: Float, newYRatio: Float) {
        viewModelScope.launch {
            repository.updatePageElement(element.copy(xRatio = newXRatio, yRatio = newYRatio))
        }
    }

    fun updatePageElement(element: PageElementEntity) {
        viewModelScope.launch {
            repository.updatePageElement(element)
            _statusMessage.value = "Element updated"
        }
    }

    fun deletePageElement(id: String) {
        viewModelScope.launch {
            repository.deletePageElement(id)
            _statusMessage.value = "Element deleted"
        }
    }

    fun toggleControls() {
        _isControlsVisible.value = !_isControlsVisible.value
    }

    fun setReadingTheme(theme: ReadingTheme) {
        _readingTheme.value = theme
    }

    fun toggleTwoPageSpread() {
        _isTwoPageSpread.value = !_isTwoPageSpread.value
    }

    fun toggleHighlightMode() {
        _isHighlightModeActive.value = !_isHighlightModeActive.value
    }

    fun setSelectedHighlightColor(colorHex: String) {
        _selectedHighlightColor.value = colorHex
    }

    fun initiateAddAnnotation(text: String, topRatio: Float) {
        _pendingHighlightText.value = text
        _pendingHighlightTopRatio.value = topRatio
        _isAddingNoteDialog.value = true
    }

    fun dismissAddNoteDialog() {
        _isAddingNoteDialog.value = false
    }

    fun confirmAddAnnotation(note: String, tag: String, colorHex: String) {
        val doc = _activeDocument.value ?: return
        val page = _currentPageIndex.value
        val text = _pendingHighlightText.value.ifBlank { "Highlighted passage on page ${page + 1}" }

        viewModelScope.launch {
            repository.addAnnotation(
                docId = doc.id,
                page = page,
                text = text,
                colorHex = colorHex,
                note = note,
                tag = tag,
                topRatio = _pendingHighlightTopRatio.value
            )
            _isAddingNoteDialog.value = false
            _statusMessage.value = "Note added & synced locally"
        }
    }

    fun openEditAnnotation(annotation: AnnotationEntity) {
        _editingAnnotation.value = annotation
    }

    fun dismissEditAnnotation() {
        _editingAnnotation.value = null
    }

    fun saveEditedAnnotation(annotation: AnnotationEntity, updatedNote: String, updatedTag: String, updatedColor: String) {
        viewModelScope.launch {
            repository.updateAnnotation(
                annotation.copy(
                    note = updatedNote,
                    tag = updatedTag,
                    colorHex = updatedColor
                )
            )
            _editingAnnotation.value = null
            _statusMessage.value = "Annotation updated"
        }
    }

    fun updateAnnotation(annotation: AnnotationEntity) {
        viewModelScope.launch {
            repository.updateAnnotation(annotation)
            _statusMessage.value = "Annotation updated"
        }
    }

    fun deleteAnnotation(id: String) {
        viewModelScope.launch {
            repository.deleteAnnotation(id)
            _editingAnnotation.value = null
            _statusMessage.value = "Annotation deleted"
        }
    }

    fun openSearch() {
        _isSearchOpen.value = true
    }

    fun closeSearch() {
        _isSearchOpen.value = false
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            val results = pdfEngine.searchFullText(query)
            _searchResults.value = results
            _currentSearchMatchIndex.value = 0
            if (results.isNotEmpty()) {
                goToPage(results[0].pageNumber)
            }
        } else {
            _searchResults.value = emptyList()
        }
    }

    fun nextSearchResult() {
        val results = _searchResults.value
        if (results.isNotEmpty()) {
            val nextIdx = (_currentSearchMatchIndex.value + 1) % results.size
            _currentSearchMatchIndex.value = nextIdx
            goToPage(results[nextIdx].pageNumber)
        }
    }

    fun prevSearchResult() {
        val results = _searchResults.value
        if (results.isNotEmpty()) {
            val prevIdx = if (_currentSearchMatchIndex.value - 1 < 0) results.size - 1 else _currentSearchMatchIndex.value - 1
            _currentSearchMatchIndex.value = prevIdx
            goToPage(results[prevIdx].pageNumber)
        }
    }

    fun toggleReadingRuler() {
        _isReadingRulerEnabled.value = !_isReadingRulerEnabled.value
    }

    fun setReadingRulerPosition(ratio: Float) {
        _readingRulerPositionRatio.value = ratio.coerceIn(0.05f, 0.95f)
    }

    fun setOfflineMode(enabled: Boolean) {
        cloudSyncRepository.setOfflineMode(enabled)
        _statusMessage.value = if (enabled) "Offline Mode Enabled" else "Online Mode Restored"
    }

    fun syncNow() {
        viewModelScope.launch {
            val result = cloudSyncRepository.syncAllNotes()
            result.onSuccess { count ->
                _statusMessage.value = "Synced $count notes across all devices"
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Sync failed"
            }
        }
    }

    fun importLocalPdf(uri: Uri) {
        viewModelScope.launch {
            val doc = repository.importLocalPdf(uri, pdfEngine)
            if (doc != null) {
                openDocument(doc)
                _statusMessage.value = "Opened '${doc.title}'"
            } else {
                _statusMessage.value = "Failed to load PDF file."
            }
        }
    }

    fun downloadCloudFile(cloudFile: CloudFile) {
        viewModelScope.launch {
            val doc = cloudSyncRepository.downloadCloudDocument(cloudFile, pdfEngine)
            if (doc != null) {
                _statusMessage.value = "Downloaded '${doc.title}' for offline reading"
            } else {
                _statusMessage.value = "Download failed."
            }
        }
    }

    fun toggleFavorite(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(doc.id, doc.isFavorite)
        }
    }

    fun toggleOfflineStatus(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.toggleOfflineStatus(doc.id, doc.isOfflineAvailable)
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            if (_activeDocument.value?.id == doc.id) {
                closeDocument()
            }
            repository.deleteDocument(doc)
            _statusMessage.value = "Removed '${doc.title}'"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Gemini Chatbot State
    val geminiChatService = com.example.engine.GeminiChatService()

    // GitHub Release App Update Manager
    val appUpdateManager = com.example.engine.AppUpdateManager(application)
    val latestRelease: StateFlow<com.example.engine.AppReleaseInfo?> = appUpdateManager.latestRelease
    val updateDownloadState: StateFlow<com.example.engine.UpdateDownloadState> = appUpdateManager.downloadState
    val isCheckingForUpdates: StateFlow<Boolean> = appUpdateManager.isChecking
    val updateCheckStatusMessage: StateFlow<String?> = appUpdateManager.checkStatusMessage

    fun checkForAppUpdates(forceCheck: Boolean = false) {
        viewModelScope.launch {
            appUpdateManager.checkForUpdates(forceCheck)
        }
    }

    fun downloadAndInstallUpdate(release: com.example.engine.AppReleaseInfo) {
        viewModelScope.launch {
            appUpdateManager.downloadAndInstallUpdate(release)
        }
    }

    fun dismissUpdateNotification() {
        appUpdateManager.dismissCurrentUpdateNotification()
    }

    fun getGitHubRepo(): String = appUpdateManager.getGitHubRepo()

    fun setGitHubRepo(repo: String) {
        appUpdateManager.setGitHubRepo(repo)
    }

    fun simulateNewRelease() {
        appUpdateManager.simulateNewRelease()
    }

    fun installDownloadedApk(apkFile: java.io.File) {
        appUpdateManager.installApk(apkFile)
    }

    // Voice Reader / Read Aloud Engine (TTS)
    val voiceReaderEngine = com.example.engine.VoiceReaderEngine(application)
    val isVoiceReadingPlaying: StateFlow<Boolean> = voiceReaderEngine.isPlaying
    val voiceReadingSentenceIndex: StateFlow<Int> = voiceReaderEngine.currentSentenceIndex
    val voiceReadingSentenceText: StateFlow<String> = voiceReaderEngine.currentSentenceText
    val voiceReadingTotalSentences: StateFlow<Int> = voiceReaderEngine.totalSentences
    val voiceReadingSpeed: StateFlow<Float> = voiceReaderEngine.playbackSpeed
    val voiceReadingVolume: StateFlow<Float> = voiceReaderEngine.playbackVolume
    val voiceReadingPitch: StateFlow<Float> = voiceReaderEngine.playbackPitch
    val availableVoices = voiceReaderEngine.availableVoices
    val availableEngines = voiceReaderEngine.availableEngines
    val selectedVoiceName = voiceReaderEngine.selectedVoiceName
    val selectedEnginePackage = voiceReaderEngine.selectedEnginePackage
    val selectedLanguageMode = voiceReaderEngine.selectedLanguageMode
    val isReadingBangla = voiceReaderEngine.isReadingBangla
    val isBanglaSupportedOnDevice = voiceReaderEngine.isBanglaSupportedOnDevice

    fun setVoiceReadingPitch(pitch: Float) {
        voiceReaderEngine.setPlaybackPitch(pitch)
    }

    fun setVoiceSelection(voiceName: String?) {
        voiceReaderEngine.setVoiceByName(voiceName)
    }

    fun switchTtsEngine(packageName: String) {
        voiceReaderEngine.switchTtsEngine(packageName)
    }

    fun setVoiceLanguageMode(mode: String) {
        voiceReaderEngine.setLanguageMode(mode)
    }

    fun testVoiceReading(sampleText: String? = null, isBangla: Boolean = false) {
        voiceReaderEngine.testVoice(sampleText, isBangla)
    }

    suspend fun translateText(
        text: String,
        targetLanguage: String = "English",
        sourceLanguage: String? = null
    ): Result<String> {
        return geminiChatService.translateText(text, targetLanguage, sourceLanguage)
    }

    private val _lastPageTurnDelta = MutableStateFlow(1) // +1 for next, -1 for prev
    val lastPageTurnDelta: StateFlow<Int> = _lastPageTurnDelta.asStateFlow()

    fun togglePageFlip() {
        val newVal = !_isPageFlipEnabled.value
        _isPageFlipEnabled.value = newVal
        readerPrefs.edit().putBoolean("pref_page_flip_enabled", newVal).apply()
        _statusMessage.value = if (newVal) "Page Flip Animation: ON" else "Page Flip Animation: OFF"
    }

    fun setPageFlipEnabled(enabled: Boolean) {
        _isPageFlipEnabled.value = enabled
        readerPrefs.edit().putBoolean("pref_page_flip_enabled", enabled).apply()
    }

    fun setVoiceReadingVolume(volume: Float) {
        voiceReaderEngine.setPlaybackVolume(volume)
    }

    init {
        voiceReaderEngine.onPageCompleted = {
            if (_currentPageIndex.value < _totalPages.value - 1) {
                viewModelScope.launch {
                    nextPage()
                }
            } else {
                _statusMessage.value = "Document voice reading completed"
            }
        }
    }

    private val _isVoicePlayerVisible = MutableStateFlow(false)
    val isVoicePlayerVisible: StateFlow<Boolean> = _isVoicePlayerVisible.asStateFlow()

    fun startVoiceReading() {
        val doc = _activeDocument.value
        val title = doc?.title ?: "Document"
        val pageText = pdfEngine.getPageText(_currentPageIndex.value, title, _totalPages.value)
        voiceReaderEngine.loadText(pageText, title, _currentPageIndex.value + 1)
        _isVoicePlayerVisible.value = true
        voiceReaderEngine.play()
    }

    fun openDocumentAndStartVoiceReading(doc: DocumentEntity, startPage: Int = -1) {
        viewModelScope.launch {
            openDocument(doc, startPage)
            var attempts = 0
            while (_activeDocument.value == null && attempts < 10) {
                delay(100)
                attempts++
            }
            startVoiceReading()
        }
    }

    fun toggleVoiceReading() {
        if (!_isVoicePlayerVisible.value) {
            startVoiceReading()
        } else {
            voiceReaderEngine.togglePlayPause()
        }
    }

    fun skipVoiceReadingForward() {
        voiceReaderEngine.skipForward()
    }

    fun skipVoiceReadingBackward() {
        voiceReaderEngine.skipBackward()
    }

    fun seekVoiceReading(sentenceIndex: Int) {
        voiceReaderEngine.seekTo(sentenceIndex)
    }

    fun setVoiceReadingSpeed(speed: Float) {
        voiceReaderEngine.setPlaybackSpeed(speed)
    }

    fun cycleVoiceReadingSpeed() {
        voiceReaderEngine.cycleSpeed()
    }

    fun closeVoiceReader() {
        voiceReaderEngine.stop()
        _isVoicePlayerVisible.value = false
    }

    private val _chatMessages = MutableStateFlow<List<com.example.data.model.ChatMessage>>(
        listOf(
            com.example.data.model.ChatMessage(
                sender = com.example.data.model.MessageSender.AI,
                text = "Greetings! I am Tome AI, your literary companion and reading scholar. You can ask me to analyze chapters, explain difficult passages, summarize ideas, or use Google Search Grounding to verify historical facts and author background. What would you like to explore today?",
                modelUsed = "Gemini 3.5 Flash"
            )
        )
    )
    val chatMessages: StateFlow<List<com.example.data.model.ChatMessage>> = _chatMessages.asStateFlow()

    private val _selectedChatModel = MutableStateFlow(com.example.data.model.GeminiModelOption.GENERAL)
    val selectedChatModel: StateFlow<com.example.data.model.GeminiModelOption> = _selectedChatModel.asStateFlow()

    private val _selectedChatRole = MutableStateFlow(com.example.data.model.ChatbotRole.LITERARY_SCHOLAR)
    val selectedChatRole: StateFlow<com.example.data.model.ChatbotRole> = _selectedChatRole.asStateFlow()

    private val _isSearchGroundingEnabled = MutableStateFlow(true)
    val isSearchGroundingEnabled: StateFlow<Boolean> = _isSearchGroundingEnabled.asStateFlow()

    private val _isGeneratingChatResponse = MutableStateFlow(false)
    val isGeneratingChatResponse: StateFlow<Boolean> = _isGeneratingChatResponse.asStateFlow()

    fun setSelectedChatModel(model: com.example.data.model.GeminiModelOption) {
        _selectedChatModel.value = model
    }

    fun setSelectedChatRole(role: com.example.data.model.ChatbotRole) {
        _selectedChatRole.value = role
    }

    fun toggleSearchGrounding() {
        _isSearchGroundingEnabled.value = !_isSearchGroundingEnabled.value
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            com.example.data.model.ChatMessage(
                sender = com.example.data.model.MessageSender.AI,
                text = "Chat history cleared. How may I assist you with your reading?",
                modelUsed = _selectedChatModel.value.displayName
            )
        )
    }

    fun sendChatMessage(userText: String, includeBookContext: Boolean = true) {
        if (userText.isBlank() || _isGeneratingChatResponse.value) return

        val userMessage = com.example.data.model.ChatMessage(
            sender = com.example.data.model.MessageSender.USER,
            text = userText
        )
        val placeholderLoading = com.example.data.model.ChatMessage(
            sender = com.example.data.model.MessageSender.AI,
            text = "Thinking...",
            isLoading = true
        )

        _chatMessages.value = _chatMessages.value + userMessage + placeholderLoading
        _isGeneratingChatResponse.value = true

        val bookContext = if (includeBookContext && _activeDocument.value != null) {
            val doc = _activeDocument.value!!
            val pageText = pdfEngine.getPageText(_currentPageIndex.value)
            "Book: '${doc.title}' by ${doc.author}. Currently on page ${_currentPageIndex.value + 1} of ${doc.totalPages}.\nPage text excerpt:\n$pageText"
        } else null

        viewModelScope.launch {
            val result = geminiChatService.sendMessage(
                history = _chatMessages.value.dropLast(1), // exclude loading placeholder
                newPrompt = userText,
                modelOption = _selectedChatModel.value,
                role = _selectedChatRole.value,
                enableSearchGrounding = _isSearchGroundingEnabled.value,
                bookContext = bookContext
            )

            result.onSuccess { chatResult ->
                val aiMessage = com.example.data.model.ChatMessage(
                    sender = com.example.data.model.MessageSender.AI,
                    text = chatResult.responseText,
                    modelUsed = chatResult.modelUsed,
                    searchQueries = chatResult.searchQueries,
                    sources = chatResult.sources
                )
                // Replace loading placeholder with actual response
                _chatMessages.value = _chatMessages.value.filter { !it.isLoading } + aiMessage
                _isGeneratingChatResponse.value = false
            }.onFailure { error ->
                val errorMessage = com.example.data.model.ChatMessage(
                    sender = com.example.data.model.MessageSender.AI,
                    text = "I encountered an issue: ${error.message}",
                    isError = true,
                    modelUsed = _selectedChatModel.value.displayName
                )
                _chatMessages.value = _chatMessages.value.filter { !it.isLoading } + errorMessage
                _isGeneratingChatResponse.value = false
            }
        }
    }

    fun getExportMarkdownForCurrentDoc(): String {
        val doc = _activeDocument.value ?: return ""
        return repository.exportNotesAsMarkdown(doc.title, _currentAnnotations.value)
    }

    override fun onCleared() {
        super.onCleared()
        lightSensorManager.stopListening()
        voiceReaderEngine.shutdown()
        pdfEngine.close()
    }
}

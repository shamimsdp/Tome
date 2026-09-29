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
import com.example.data.model.ReadingTheme
import com.example.data.model.SyncLogEntity
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.PdfRepository
import com.example.engine.PdfEngine
import com.example.engine.SearchMatch
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
        syncLogDao = database.syncLogDao()
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

    // Status snackbar message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeSamplesIfNeeded(pdfEngine)
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
            goToPage(targetPage.coerceIn(0, (_totalPages.value - 1).coerceAtLeast(0)))

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
        }
    }

    fun closeDocument() {
        _activeDocument.value = null
        _currentPageBitmap.value = null
        pdfEngine.close()
    }

    fun goToPage(page: Int) {
        val total = _totalPages.value
        val clamped = page.coerceIn(0, (total - 1).coerceAtLeast(0))
        _currentPageIndex.value = clamped

        viewModelScope.launch {
            val bitmap = pdfEngine.renderPage(clamped)
            _currentPageBitmap.value = bitmap
            checkBookmarkStatus()

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
            repository.toggleBookmark(doc.id, page, title)
            checkBookmarkStatus()
        }
    }

    fun deleteBookmark(id: String) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
            checkBookmarkStatus()
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
}

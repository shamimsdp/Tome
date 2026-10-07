package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.DocumentEntity
import com.example.data.model.ReadingTheme
import com.example.data.model.ThemeMode
import com.example.ui.components.AddAnnotationDialog
import com.example.ui.components.AddElementDialog
import com.example.ui.components.BookPageView
import com.example.ui.components.BookmarksPanel
import com.example.ui.components.ContextualSelectionToolbar
import com.example.ui.components.CustomColorPickerDialog
import com.example.ui.components.EditAnnotationDialog
import com.example.ui.components.EditModeFloatingBar
import com.example.ui.components.ExportAnnotationsSheet
import com.example.ui.components.NotesAndBookmarksSheet
import com.example.ui.components.PersistentReaderSearchBar
import com.example.ui.components.ReadingBottomBar
import com.example.ui.components.ReadingSessionSheet
import com.example.ui.components.ReadingTopBar
import com.example.ui.components.SearchOverlay
import com.example.ui.components.TextSelectionHighlightDialog
import com.example.ui.components.ThumbnailGridSheet
import com.example.ui.components.VoiceSettingsSheet
import com.example.ui.components.VoiceReadingBottomBar
import com.example.ui.viewmodel.PdfViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    document: DocumentEntity,
    viewModel: PdfViewModel,
    onBack: () -> Unit
) {
    BackHandler {
        viewModel.closeDocument()
        onBack()
    }

    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val currentPageBitmap by viewModel.currentPageBitmap.collectAsState()
    val readingTheme by viewModel.readingTheme.collectAsState()
    val isControlsVisible by viewModel.isControlsVisible.collectAsState()
    val isBookmarked by viewModel.isCurrentPageBookmarked.collectAsState()
    val isHighlightMode by viewModel.isHighlightModeActive.collectAsState()
    val selectedHighlightColor by viewModel.selectedHighlightColor.collectAsState()
    val annotations by viewModel.currentAnnotations.collectAsState()
    val bookmarks by viewModel.currentBookmarks.collectAsState()

    // Reading Session Timer state
    val sessionDurationSeconds by viewModel.sessionDurationSeconds.collectAsState()
    val isSessionTimerRunning by viewModel.isSessionTimerRunning.collectAsState()
    val pagesReadThisSession by viewModel.pagesReadThisSession.collectAsState()
    val readingGoalMinutes by viewModel.readingGoalMinutes.collectAsState()
    val isReadingSessionSheetOpen by viewModel.isReadingSessionSheetOpen.collectAsState()
    val sessionDurationText = viewModel.formatTimerDisplay(sessionDurationSeconds)

    // Voice Reading state
    val isVoicePlayerVisible by viewModel.isVoicePlayerVisible.collectAsState()
    val isVoicePlaying by viewModel.isVoiceReadingPlaying.collectAsState()
    val currentSentenceIndex by viewModel.voiceReadingSentenceIndex.collectAsState()
    val totalSentences by viewModel.voiceReadingTotalSentences.collectAsState()
    val currentSentenceText by viewModel.voiceReadingSentenceText.collectAsState()
    val voiceSpeed by viewModel.voiceReadingSpeed.collectAsState()
    val voiceVolume by viewModel.voiceReadingVolume.collectAsState()
    val voicePitch by viewModel.voiceReadingPitch.collectAsState()
    val availableVoices by viewModel.availableVoices.collectAsState()
    val availableEngines by viewModel.availableEngines.collectAsState()
    val selectedVoiceName by viewModel.selectedVoiceName.collectAsState()
    val selectedEnginePackage by viewModel.selectedEnginePackage.collectAsState()
    val selectedLanguageMode by viewModel.selectedLanguageMode.collectAsState()
    val isReadingBangla by viewModel.isReadingBangla.collectAsState()
    val isBanglaSupportedOnDevice by viewModel.isBanglaSupportedOnDevice.collectAsState()

    // Page flip animation state
    val isPageFlipEnabled by viewModel.isPageFlipEnabled.collectAsState()
    val lastPageTurnDelta by viewModel.lastPageTurnDelta.collectAsState()
    val isAutoBrightnessEnabled by viewModel.isAutoBrightnessEnabled.collectAsState()
    val currentAppBrightness by viewModel.currentAppBrightness.collectAsState()

    // Search state
    val isSearchOpen by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchMatchIndex by viewModel.currentSearchMatchIndex.collectAsState()

    // Reading ruler
    val isReadingRulerEnabled by viewModel.isReadingRulerEnabled.collectAsState()
    val readingRulerRatio by viewModel.readingRulerPositionRatio.collectAsState()

    // Dialog states
    val isAddNoteDialogOpen by viewModel.isAddingNoteDialog.collectAsState()
    val pendingHighlightText by viewModel.pendingHighlightText.collectAsState()
    val editingAnnotation by viewModel.editingAnnotation.collectAsState()

    // Page Elements & Editing States
    val currentElements by viewModel.currentElements.collectAsState()
    val isBookmarksPanelOpen by viewModel.isBookmarksPanelOpen.collectAsState()
    val isTextSelectionHighlightDialogOpen by viewModel.isTextSelectionHighlightDialogOpen.collectAsState()
    val isEditElementsMode by viewModel.isEditElementsMode.collectAsState()
    val isAddElementDialogOpen by viewModel.isAddElementDialogOpen.collectAsState()
    var showCustomColorPicker by remember { mutableStateOf(false) }

    var showNotesSheet by remember { mutableStateOf(false) }
    var notesDrawerInitialTab by remember { mutableIntStateOf(0) }
    var showAiChatSheet by remember { mutableStateOf(false) }
    var showThumbnailGrid by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showStandaloneVoiceSettingsSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val statusMessage by viewModel.statusMessage.collectAsState()

    val themeMode by viewModel.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    } || (readingTheme == ReadingTheme.CHARCOAL || readingTheme == ReadingTheme.OLED_NIGHT)

    val pageBackground = when {
        readingTheme == ReadingTheme.OLED_NIGHT -> androidx.compose.ui.graphics.Color(0xFF000000)
        readingTheme == ReadingTheme.CHARCOAL -> androidx.compose.ui.graphics.Color(0xFF1E222A)
        isDarkTheme && readingTheme == ReadingTheme.DAY -> androidx.compose.ui.graphics.Color(0xFF121824)
        else -> readingTheme.paperColor
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier
            .fillMaxSize()
            .testTag("reader_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackground)
        ) {
            // Main Book Page View
            BookPageView(
                bitmap = currentPageBitmap,
                pageIndex = currentPageIndex,
                totalPages = totalPages,
                pdfEngine = viewModel.pdfEngine,
                readingTheme = readingTheme,
                isDarkTheme = isDarkTheme,
                isBookmarked = isBookmarked,
                annotations = annotations.filter { it.pageNumber == currentPageIndex },
                searchMatches = searchResults,
                isHighlightMode = isHighlightMode,
                isReadingRulerEnabled = isReadingRulerEnabled,
                readingRulerRatio = readingRulerRatio,
                isPageFlipEnabled = isPageFlipEnabled,
                pageTurnDelta = lastPageTurnDelta,
                pageElements = currentElements,
                isEditElementsMode = isEditElementsMode,
                onTapLeft = { viewModel.prevPage() },
                onTapRight = { viewModel.nextPage() },
                onTapCenter = { viewModel.toggleControls() },
                onAddHighlightAtRatio = { ratio ->
                    val pageText = viewModel.pdfEngine.getPageText(currentPageIndex)
                    val sampleSnippet = if (pageText.isNotBlank()) {
                        val lines = pageText.lines().filter { it.isNotBlank() }
                        val idx = (ratio * lines.size).toInt().coerceIn(0, (lines.size - 1).coerceAtLeast(0))
                        lines.getOrNull(idx) ?: "Selected passage on page ${currentPageIndex + 1}"
                    } else {
                        "Important passage on page ${currentPageIndex + 1}"
                    }
                    viewModel.openTextSelectionHighlightDialog(sampleSnippet, ratio)
                },
                onAnnotationClick = { ann ->
                    viewModel.openEditAnnotation(ann)
                },
                onRulerPositionChange = { newRatio ->
                    viewModel.setReadingRulerPosition(newRatio)
                },
                onUpdateElementPosition = { elem, newX, newY ->
                    viewModel.updateElementPosition(elem, newX, newY)
                },
                onDeleteElement = { id ->
                    viewModel.deletePageElement(id)
                },
                onEditElement = {
                    viewModel.openAddElementDialog()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Bar with animations
            AnimatedVisibility(
                visible = isControlsVisible && !isSearchActive,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            ) {
                ReadingTopBar(
                    title = document.title,
                    author = document.author,
                    currentTheme = readingTheme,
                    isBookmarked = isBookmarked,
                    isHighlightMode = isHighlightMode,
                    bookmarkCount = bookmarks.size,
                    sessionDurationText = sessionDurationText,
                    isSessionTimerRunning = isSessionTimerRunning,
                    isPageFlipEnabled = isPageFlipEnabled,
                    isEditElementsMode = isEditElementsMode,
                    onBack = {
                        viewModel.closeDocument()
                        onBack()
                    },
                    onToggleBookmark = { viewModel.toggleBookmark() },
                    onOpenBookmarksDrawer = {
                        viewModel.openBookmarksPanel()
                    },
                    onToggleHighlightMode = { viewModel.toggleHighlightMode() },
                    onToggleEditElementsMode = { viewModel.toggleEditElementsMode() },
                    onTogglePageFlip = { viewModel.togglePageFlip() },
                    onOpenSearch = { isSearchActive = !isSearchActive },
                    onOpenNotesDrawer = {
                        notesDrawerInitialTab = 0
                        showNotesSheet = true
                    },
                    onOpenThumbnailGrid = { showThumbnailGrid = true },
                    onOpenExportSheet = { showExportSheet = true },
                    onOpenChat = { showAiChatSheet = true },
                    onStartVoiceReading = { viewModel.toggleVoiceReading() },
                    onOpenVoiceSettings = { showStandaloneVoiceSettingsSheet = true },
                    onOpenSessionTimer = { viewModel.openReadingSessionSheet() },
                    onSelectTheme = { theme -> viewModel.setReadingTheme(theme) },
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { viewModel.toggleDarkTheme() },
                    isAutoBrightnessEnabled = isAutoBrightnessEnabled,
                    onToggleAutoBrightness = { viewModel.setAutoBrightnessEnabled(!isAutoBrightnessEnabled) },
                    currentBrightness = currentAppBrightness,
                    onBrightnessChange = { viewModel.setManualBrightness(it) },
                    onResetSystemBrightness = { viewModel.resetToSystemBrightness() }
                )
            }

            // Floating Contextual Highlighter Selection Toolbar (from Image 1 & 2)
            AnimatedVisibility(
                visible = isHighlightMode && !isVoicePlayerVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isControlsVisible) 100.dp else 24.dp)
            ) {
                ContextualSelectionToolbar(
                    selectedColor = selectedHighlightColor,
                    onColorSelected = { colorHex ->
                        viewModel.setSelectedHighlightColor(colorHex)
                    },
                    onOpenCustomColorPicker = {
                        showCustomColorPicker = true
                    },
                    onReadAloud = {
                        viewModel.startVoiceReading()
                    },
                    onAddNote = {
                        viewModel.openTextSelectionHighlightDialog(
                            initialText = "Selected passage on page ${currentPageIndex + 1}",
                            topRatio = 0.5f
                        )
                    },
                    onCopyText = {
                        // handled seamlessly
                    },
                    onOpenEditMode = {
                        viewModel.toggleEditElementsMode()
                    }
                )
            }

            // Floating Edit Mode Toolbar (Add Text, Image, Stamp)
            AnimatedVisibility(
                visible = isEditElementsMode && !isVoicePlayerVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isControlsVisible) 100.dp else 24.dp)
            ) {
                EditModeFloatingBar(
                    onAddText = { viewModel.openAddElementDialog() },
                    onAddImage = { viewModel.openAddElementDialog() },
                    onAddStamp = { viewModel.openAddElementDialog() },
                    onDoneEditing = { viewModel.setEditElementsMode(false) }
                )
            }

            // Persistent Search Bar at top of reader interface
            AnimatedVisibility(
                visible = isSearchActive,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                PersistentReaderSearchBar(
                    query = searchQuery,
                    searchResults = searchResults,
                    currentIndex = searchMatchIndex,
                    currentPageIndex = currentPageIndex,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    onNextMatch = { viewModel.nextSearchResult() },
                    onPrevMatch = { viewModel.prevSearchResult() },
                    onSelectMatchPage = { page -> viewModel.goToPage(page) },
                    onClose = {
                        isSearchActive = false
                        viewModel.closeSearch()
                    }
                )
            }

            // Bottom Scrubber Bar with animations
            AnimatedVisibility(
                visible = isControlsVisible && !isVoicePlayerVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ReadingBottomBar(
                    currentPage = currentPageIndex,
                    totalPages = totalPages,
                    isReadingRulerEnabled = isReadingRulerEnabled,
                    sessionDurationText = sessionDurationText,
                    isPageFlipEnabled = isPageFlipEnabled,
                    onOpenSessionTimer = { viewModel.openReadingSessionSheet() },
                    onTogglePageFlip = { viewModel.togglePageFlip() },
                    onPageChange = { page -> viewModel.goToPage(page) },
                    onPrevPage = { viewModel.prevPage() },
                    onNextPage = { viewModel.nextPage() },
                    onToggleReadingRuler = { viewModel.toggleReadingRuler() },
                    onOpenThumbnailGrid = { showThumbnailGrid = true }
                )
            }

            // Voice Reading Audio Player (Read Aloud from Image 3)
            AnimatedVisibility(
                visible = isVoicePlayerVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                VoiceReadingBottomBar(
                    isPlaying = isVoicePlaying,
                    currentSentenceIndex = currentSentenceIndex,
                    totalSentences = totalSentences,
                    currentSentenceText = currentSentenceText,
                    playbackSpeed = voiceSpeed,
                    playbackVolume = voiceVolume,
                    playbackPitch = voicePitch,
                    availableVoices = availableVoices,
                    availableEngines = availableEngines,
                    selectedVoiceName = selectedVoiceName,
                    selectedEnginePackage = selectedEnginePackage,
                    selectedLanguageMode = selectedLanguageMode,
                    isReadingBangla = isReadingBangla,
                    isBanglaSupported = isBanglaSupportedOnDevice,
                    currentPage = currentPageIndex,
                    totalPages = totalPages,
                    onTogglePlayPause = { viewModel.toggleVoiceReading() },
                    onSkipForward = { viewModel.skipVoiceReadingForward() },
                    onSkipBackward = { viewModel.skipVoiceReadingBackward() },
                    onSeekTo = { sentenceIndex -> viewModel.seekVoiceReading(sentenceIndex) },
                    onSelectSpeed = { speed -> viewModel.setVoiceReadingSpeed(speed) },
                    onVolumeChange = { volume -> viewModel.setVoiceReadingVolume(volume) },
                    onPitchChange = { viewModel.setVoiceReadingPitch(it) },
                    onSelectVoice = { viewModel.setVoiceSelection(it) },
                    onSwitchEngine = { viewModel.switchTtsEngine(it) },
                    onSelectLanguageMode = { viewModel.setVoiceLanguageMode(it) },
                    onTestVoice = { sample, isBangla -> viewModel.testVoiceReading(sample, isBangla) },
                    onCycleSpeed = { viewModel.cycleVoiceReadingSpeed() },
                    onPrevPage = { viewModel.prevPage() },
                    onNextPage = { viewModel.nextPage() },
                    onClose = { viewModel.closeVoiceReader() }
                )
            }
        }
    }

    // Add Highlight & Note Dialog with Gemini Translation
    if (isAddNoteDialogOpen) {
        AddAnnotationDialog(
            initialText = pendingHighlightText,
            onDismiss = { viewModel.dismissAddNoteDialog() },
            onConfirm = { note, tag, colorHex ->
                viewModel.confirmAddAnnotation(note, tag, colorHex)
            },
            onTranslate = { text, targetLang ->
                viewModel.translateText(text, targetLang)
            }
        )
    }

    // Edit Existing Annotation Dialog with Gemini Translation
    editingAnnotation?.let { annotation ->
        EditAnnotationDialog(
            annotation = annotation,
            onDismiss = { viewModel.dismissEditAnnotation() },
            onSave = { updatedNote, updatedTag, updatedColor ->
                viewModel.saveEditedAnnotation(annotation, updatedNote, updatedTag, updatedColor)
            },
            onDelete = { id ->
                viewModel.deleteAnnotation(id)
            },
            onTranslate = { text, targetLang ->
                viewModel.translateText(text, targetLang)
            }
        )
    }

    // Notes and Bookmarks Drawer Sheet with Note-Taking View Translation
    if (showNotesSheet) {
        NotesAndBookmarksSheet(
            bookTitle = document.title,
            author = document.author,
            annotations = annotations,
            bookmarks = bookmarks,
            initialTab = notesDrawerInitialTab,
            onSelectPage = { page ->
                viewModel.goToPage(page)
            },
            onEditAnnotation = { ann ->
                viewModel.openEditAnnotation(ann)
            },
            onDeleteAnnotation = { id ->
                viewModel.deleteAnnotation(id)
            },
            onDeleteBookmark = { id ->
                viewModel.deleteBookmark(id)
            },
            onExportMarkdown = {
                viewModel.getExportMarkdownForCurrentDoc()
            },
            onTranslate = { text, targetLang ->
                viewModel.translateText(text, targetLang)
            },
            onUpdateAnnotation = { ann ->
                viewModel.updateAnnotation(ann)
            },
            onDismiss = { showNotesSheet = false }
        )
    }

    // Standalone Voice & TTS Engine Settings Sheet
    if (showStandaloneVoiceSettingsSheet) {
        VoiceSettingsSheet(
            availableVoices = availableVoices,
            availableEngines = availableEngines,
            selectedVoiceName = selectedVoiceName,
            selectedEnginePackage = selectedEnginePackage,
            selectedLanguageMode = selectedLanguageMode,
            playbackPitch = voicePitch,
            playbackSpeed = voiceSpeed,
            isReadingBangla = isReadingBangla,
            isBanglaSupported = isBanglaSupportedOnDevice,
            onSelectVoice = { viewModel.setVoiceSelection(it) },
            onSwitchEngine = { viewModel.switchTtsEngine(it) },
            onSelectLanguageMode = { viewModel.setVoiceLanguageMode(it) },
            onPitchChange = { viewModel.setVoiceReadingPitch(it) },
            onSpeedChange = { viewModel.setVoiceReadingSpeed(it) },
            onTestVoice = { sample, isBangla -> viewModel.testVoiceReading(sample, isBangla) },
            onDismiss = { showStandaloneVoiceSettingsSheet = false }
        )
    }

    // Thumbnail Grid / Page Browser Sheet
    if (showThumbnailGrid) {
        ThumbnailGridSheet(
            totalPages = totalPages,
            currentPageIndex = currentPageIndex,
            pdfEngine = viewModel.pdfEngine,
            bookmarks = bookmarks,
            annotations = annotations,
            onPageSelected = { page ->
                viewModel.goToPage(page)
            },
            onDismiss = { showThumbnailGrid = false }
        )
    }

    // Export Annotations Dialog / Sheet (PDF or TXT)
    if (showExportSheet) {
        ExportAnnotationsSheet(
            bookTitle = document.title,
            author = document.author,
            annotations = annotations,
            onDismiss = { showExportSheet = false }
        )
    }

    // AI Literary Companion Sheet
    if (showAiChatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAiChatSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                ChatScreen(viewModel = viewModel)
            }
        }
    }

    // Reading Session Timer & Progress Sheet
    if (isReadingSessionSheetOpen) {
        ReadingSessionSheet(
            viewModel = viewModel,
            document = document,
            sessionDurationSeconds = sessionDurationSeconds,
            isRunning = isSessionTimerRunning,
            pagesReadThisSession = pagesReadThisSession,
            readingGoalMinutes = readingGoalMinutes,
            onDismiss = { viewModel.closeReadingSessionSheet() }
        )
    }

    // Dedicated Bookmarks Panel (storing page index in Room database)
    if (isBookmarksPanelOpen) {
        BookmarksPanel(
            documentTitle = document.title,
            currentPageIndex = currentPageIndex,
            totalPages = totalPages,
            bookmarks = bookmarks,
            isCurrentPageBookmarked = isBookmarked,
            onSelectPage = { page ->
                viewModel.goToPage(page)
            },
            onToggleBookmarkCurrentPage = {
                viewModel.toggleBookmark()
            },
            onAddCustomBookmark = { page, title, note ->
                viewModel.addCustomBookmark(page, title, note)
            },
            onUpdateBookmark = { bm ->
                viewModel.updateBookmark(bm)
            },
            onDeleteBookmark = { id ->
                viewModel.deleteBookmark(id)
            },
            onDismiss = { viewModel.closeBookmarksPanel() }
        )
    }

    // Add Elements Dialog (Text with font/color/box, Images, Stamps)
    if (isAddElementDialogOpen) {
        AddElementDialog(
            pageIndex = currentPageIndex,
            onAddTextElement = { text, colorHex, bgHex, fontSize, isBold, isItalic ->
                viewModel.addTextElement(text, colorHex, bgHex, fontSize, isBold, isItalic)
            },
            onAddImageElement = { uriString ->
                viewModel.addImageElement(uriString)
            },
            onAddStampElement = { stampTitle, colorHex ->
                viewModel.addStampElement(stampTitle, colorHex)
            },
            onDismiss = { viewModel.closeAddElementDialog() }
        )
    }

    // Text Selection & Highlighting with Custom Colors Dialog
    if (isTextSelectionHighlightDialogOpen) {
        TextSelectionHighlightDialog(
            pageIndex = currentPageIndex,
            initialText = pendingHighlightText,
            pageTextSegments = viewModel.getPageTextSegments(currentPageIndex),
            initialColorHex = selectedHighlightColor,
            onSaveHighlight = { text, colorHex, note, tag ->
                viewModel.saveHighlightWithDetails(text, colorHex, note, tag)
            },
            onDismiss = { viewModel.closeTextSelectionHighlightDialog() }
        )
    }

    // Custom Color Picker Dialog
    if (showCustomColorPicker) {
        CustomColorPickerDialog(
            initialColorHex = selectedHighlightColor,
            onColorSelected = { hex ->
                viewModel.setSelectedHighlightColor(hex)
                showCustomColorPicker = false
            },
            onDismiss = { showCustomColorPicker = false }
        )
    }
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.DocumentEntity
import com.example.ui.components.AddAnnotationDialog
import com.example.ui.components.BookPageView
import com.example.ui.components.ContextualSelectionToolbar
import com.example.ui.components.EditAnnotationDialog
import com.example.ui.components.NotesAndBookmarksSheet
import com.example.ui.components.ReadingBottomBar
import com.example.ui.components.ReadingTopBar
import com.example.ui.components.SearchOverlay
import com.example.ui.components.VoiceReadingPlayer
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

    // Voice Reading state
    val isVoicePlayerVisible by viewModel.isVoicePlayerVisible.collectAsState()
    val isVoicePlaying by viewModel.isVoiceReadingPlaying.collectAsState()
    val currentSentenceIndex by viewModel.voiceReadingSentenceIndex.collectAsState()
    val totalSentences by viewModel.voiceReadingTotalSentences.collectAsState()
    val currentSentenceText by viewModel.voiceReadingSentenceText.collectAsState()
    val voiceSpeed by viewModel.voiceReadingSpeed.collectAsState()

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

    var showNotesSheet by remember { mutableStateOf(false) }
    var showAiChatSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val statusMessage by viewModel.statusMessage.collectAsState()

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier
            .fillMaxSize()
            .testTag("reader_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(readingTheme.paperColor)
        ) {
            // Main Book Page View
            BookPageView(
                bitmap = currentPageBitmap,
                pageIndex = currentPageIndex,
                totalPages = totalPages,
                readingTheme = readingTheme,
                isBookmarked = isBookmarked,
                annotations = annotations.filter { it.pageNumber == currentPageIndex },
                searchMatches = searchResults,
                isHighlightMode = isHighlightMode,
                isReadingRulerEnabled = isReadingRulerEnabled,
                readingRulerRatio = readingRulerRatio,
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
                    viewModel.initiateAddAnnotation(sampleSnippet, ratio)
                },
                onAnnotationClick = { ann ->
                    viewModel.openEditAnnotation(ann)
                },
                onRulerPositionChange = { newRatio ->
                    viewModel.setReadingRulerPosition(newRatio)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Bar with animations
            AnimatedVisibility(
                visible = isControlsVisible && !isSearchActive,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ReadingTopBar(
                    title = document.title,
                    author = document.author,
                    currentTheme = readingTheme,
                    isBookmarked = isBookmarked,
                    isHighlightMode = isHighlightMode,
                    onBack = {
                        viewModel.closeDocument()
                        onBack()
                    },
                    onToggleBookmark = { viewModel.toggleBookmark() },
                    onToggleHighlightMode = { viewModel.toggleHighlightMode() },
                    onOpenSearch = { isSearchActive = true },
                    onOpenNotesDrawer = { showNotesSheet = true },
                    onOpenChat = { showAiChatSheet = true },
                    onStartVoiceReading = { viewModel.toggleVoiceReading() },
                    onSelectTheme = { theme -> viewModel.setReadingTheme(theme) }
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
                    onReadAloud = {
                        viewModel.startVoiceReading()
                    },
                    onAddNote = {
                        viewModel.initiateAddAnnotation(
                            text = "Selected passage on page ${currentPageIndex + 1}",
                            topRatio = 0.5f
                        )
                    },
                    onCopyText = {
                        // handled seamlessly
                    }
                )
            }

            // Search Bar Overlay
            AnimatedVisibility(
                visible = isSearchActive,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                SearchOverlay(
                    query = searchQuery,
                    searchResults = searchResults,
                    currentIndex = searchMatchIndex,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    onNextMatch = { viewModel.nextSearchResult() },
                    onPrevMatch = { viewModel.prevSearchResult() },
                    onSelectMatch = { page -> viewModel.goToPage(page) },
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
                    onPageChange = { page -> viewModel.goToPage(page) },
                    onPrevPage = { viewModel.prevPage() },
                    onNextPage = { viewModel.nextPage() },
                    onToggleReadingRuler = { viewModel.toggleReadingRuler() }
                )
            }

            // Voice Reading Audio Player (Read Aloud from Image 3)
            AnimatedVisibility(
                visible = isVoicePlayerVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                VoiceReadingPlayer(
                    isPlaying = isVoicePlaying,
                    currentSentenceIndex = currentSentenceIndex,
                    totalSentences = totalSentences,
                    currentSentenceText = currentSentenceText,
                    playbackSpeed = voiceSpeed,
                    onTogglePlayPause = { viewModel.toggleVoiceReading() },
                    onSkipForward = { viewModel.skipVoiceReadingForward() },
                    onSkipBackward = { viewModel.skipVoiceReadingBackward() },
                    onCycleSpeed = { viewModel.cycleVoiceReadingSpeed() },
                    onClose = { viewModel.closeVoiceReader() }
                )
            }
        }
    }

    // Add Highlight & Note Dialog
    if (isAddNoteDialogOpen) {
        AddAnnotationDialog(
            initialText = pendingHighlightText,
            onDismiss = { viewModel.dismissAddNoteDialog() },
            onConfirm = { note, tag, colorHex ->
                viewModel.confirmAddAnnotation(note, tag, colorHex)
            }
        )
    }

    // Edit Existing Annotation Dialog
    editingAnnotation?.let { annotation ->
        EditAnnotationDialog(
            annotation = annotation,
            onDismiss = { viewModel.dismissEditAnnotation() },
            onSave = { updatedNote, updatedTag, updatedColor ->
                viewModel.saveEditedAnnotation(annotation, updatedNote, updatedTag, updatedColor)
            },
            onDelete = { id ->
                viewModel.deleteAnnotation(id)
            }
        )
    }

    // Notes and Bookmarks Drawer Sheet
    if (showNotesSheet) {
        NotesAndBookmarksSheet(
            bookTitle = document.title,
            annotations = annotations,
            bookmarks = bookmarks,
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
            onDismiss = { showNotesSheet = false }
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
}

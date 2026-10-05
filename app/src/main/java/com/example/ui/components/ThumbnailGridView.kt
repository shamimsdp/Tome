package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookmarkEntity
import com.example.engine.DocumentSection
import com.example.engine.PdfEngine
import kotlinx.coroutines.launch

enum class ThumbnailDisplayMode {
    BOTTOM_SHEET,
    SIDE_DRAWER
}

/**
 * Slide-out Drawer / Bottom Sheet in the PDF viewer that displays thumbnail previews
 * of all pages in the document with fast section jumping, direct page navigation,
 * and filtered view tabs (All, Bookmarks, Notes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThumbnailGridSheet(
    totalPages: Int,
    currentPageIndex: Int,
    pdfEngine: PdfEngine,
    bookmarks: List<BookmarkEntity>,
    annotations: List<AnnotationEntity>,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var displayMode by remember { mutableStateOf(ThumbnailDisplayMode.BOTTOM_SHEET) }

    if (displayMode == ThumbnailDisplayMode.BOTTOM_SHEET) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            ThumbnailViewerContent(
                totalPages = totalPages,
                currentPageIndex = currentPageIndex,
                pdfEngine = pdfEngine,
                bookmarks = bookmarks,
                annotations = annotations,
                displayMode = displayMode,
                onToggleDisplayMode = { displayMode = ThumbnailDisplayMode.SIDE_DRAWER },
                onPageSelected = { page ->
                    onPageSelected(page)
                    onDismiss()
                },
                onDismiss = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .padding(horizontal = 16.dp)
            )
        }
    } else {
        // Slide-out Side Drawer overlay
        ThumbnailSideDrawerOverlay(
            totalPages = totalPages,
            currentPageIndex = currentPageIndex,
            pdfEngine = pdfEngine,
            bookmarks = bookmarks,
            annotations = annotations,
            onToggleDisplayMode = { displayMode = ThumbnailDisplayMode.BOTTOM_SHEET },
            onPageSelected = { page ->
                onPageSelected(page)
                onDismiss()
            },
            onDismiss = onDismiss
        )
    }
}

/**
 * Animated Slide-out Navigation Drawer docked to the side of the PDF reader.
 */
@Composable
fun ThumbnailSideDrawerOverlay(
    totalPages: Int,
    currentPageIndex: Int,
    pdfEngine: PdfEngine,
    bookmarks: List<BookmarkEntity>,
    annotations: List<AnnotationEntity>,
    onToggleDisplayMode: () -> Unit,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("thumbnail_side_drawer_overlay")
    ) {
        // Semi-transparent scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss)
        )

        // Slide-out Drawer Panel anchored to the left
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(min = 280.dp, max = 360.dp)
                .fillMaxWidth(0.85f)
                .align(Alignment.CenterStart)
                .shadow(elevation = 16.dp)
                .clickable(enabled = false) {}, // prevent clicks from dismissing scrim
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            ThumbnailViewerContent(
                totalPages = totalPages,
                currentPageIndex = currentPageIndex,
                pdfEngine = pdfEngine,
                bookmarks = bookmarks,
                annotations = annotations,
                displayMode = ThumbnailDisplayMode.SIDE_DRAWER,
                onToggleDisplayMode = onToggleDisplayMode,
                onPageSelected = onPageSelected,
                onDismiss = onDismiss,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Shared rich content for both Bottom Sheet and Side Drawer modes.
 */
@Composable
private fun ThumbnailViewerContent(
    totalPages: Int,
    currentPageIndex: Int,
    pdfEngine: PdfEngine,
    bookmarks: List<BookmarkEntity>,
    annotations: List<AnnotationEntity>,
    displayMode: ThumbnailDisplayMode,
    onToggleDisplayMode: () -> Unit,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: All, 1: Bookmarks, 2: Notes
    var directPageInput by remember { mutableStateOf("") }
    var selectedSectionIndex by remember { mutableIntStateOf(-1) }

    val sections = remember(totalPages) {
        pdfEngine.getDocumentSections(totalPages)
    }

    val gridState = rememberLazyGridState()

    // Determine filtered page indices
    val displayedPageIndices = remember(selectedFilterTab, totalPages, bookmarks, annotations) {
        when (selectedFilterTab) {
            1 -> bookmarks.map { it.pageNumber }.distinct().sorted()
            2 -> annotations.map { it.pageNumber }.distinct().sorted()
            else -> (0 until totalPages).toList()
        }
    }

    // Scroll to current page on open
    LaunchedEffect(currentPageIndex, displayedPageIndices) {
        val targetIdx = displayedPageIndices.indexOf(currentPageIndex)
        if (targetIdx >= 0) {
            val scrollTo = (targetIdx - 2).coerceAtLeast(0)
            gridState.scrollToItem(scrollTo)
        }
    }

    Column(modifier = modifier.testTag("thumbnail_viewer_content")) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (displayMode == ThumbnailDisplayMode.SIDE_DRAWER) Icons.Default.ViewSidebar else Icons.Default.GridView,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (displayMode == ThumbnailDisplayMode.SIDE_DRAWER) "Page Drawer" else "Page Thumbnails",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalPages pages • Tap any thumbnail to jump",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Switch between Drawer and Bottom Sheet
                IconButton(
                    onClick = onToggleDisplayMode,
                    modifier = Modifier.testTag("toggle_thumbnail_layout_mode")
                ) {
                    Icon(
                        imageVector = if (displayMode == ThumbnailDisplayMode.BOTTOM_SHEET) Icons.Default.ViewSidebar else Icons.Default.GridView,
                        contentDescription = if (displayMode == ThumbnailDisplayMode.BOTTOM_SHEET) "Switch to Side Drawer" else "Switch to Bottom Sheet",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_thumbnails_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        // Quick Jump to Section carousel (Chapter/Section Badges)
        if (sections.isNotEmpty()) {
            Text(
                text = "Jump to Section",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("sections_jump_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sections) { sec ->
                    val isSecSelected = currentPageIndex >= sec.pageIndex &&
                            (sections.getOrNull(sections.indexOf(sec) + 1)?.pageIndex?.let { currentPageIndex < it } ?: true)

                    FilterChip(
                        selected = isSecSelected,
                        onClick = {
                            selectedSectionIndex = sec.pageIndex
                            val targetItemIdx = displayedPageIndices.indexOf(sec.pageIndex)
                            if (targetItemIdx >= 0) {
                                coroutineScope.launch {
                                    gridState.animateScrollToItem(targetItemIdx)
                                }
                            }
                            onPageSelected(sec.pageIndex)
                        },
                        label = {
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = sec.title,
                                    fontWeight = if (isSecSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Page ${sec.pageIndex + 1}",
                                    fontSize = 10.sp,
                                    color = if (isSecSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Direct Page Input & Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = directPageInput,
                onValueChange = { directPageInput = it.filter { char -> char.isDigit() }.take(5) },
                label = { Text("Jump to Page (1–$totalPages)", fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        val p = directPageInput.toIntOrNull()
                        if (p != null && p in 1..totalPages) {
                            onPageSelected(p - 1)
                            focusManager.clearFocus()
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("jump_to_page_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    val p = directPageInput.toIntOrNull()
                    if (p != null && p in 1..totalPages) {
                        onPageSelected(p - 1)
                        focusManager.clearFocus()
                    }
                },
                enabled = directPageInput.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (directPageInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("jump_page_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go",
                    tint = if (directPageInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Filter Tabs (All Pages, Bookmarked, Annotated)
        TabRow(
            selectedTabIndex = selectedFilterTab,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth().testTag("thumbnail_filter_tabs")
        ) {
            Tab(
                selected = selectedFilterTab == 0,
                onClick = { selectedFilterTab = 0 },
                text = { Text("All (${totalPages})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedFilterTab == 1,
                onClick = { selectedFilterTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFE11D48))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bookmarks (${bookmarks.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
            Tab(
                selected = selectedFilterTab == 2,
                onClick = { selectedFilterTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.width(4.dp))
                        val distinctNotePages = annotations.map { it.pageNumber }.distinct().size
                        Text("Notes ($distinctNotePages)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Thumbnail Previews Grid / Vertical Strip
        if (displayedPageIndices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedFilterTab == 1) "No bookmarks in this document yet." else "No notes on any pages yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val minGridCellSize = if (displayMode == ThumbnailDisplayMode.SIDE_DRAWER) 100.dp else 115.dp

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = minGridCellSize),
                state = gridState,
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("thumbnail_grid_list")
            ) {
                items(displayedPageIndices, key = { it }) { pageIdx ->
                    val isCurrent = pageIdx == currentPageIndex
                    val hasBookmark = bookmarks.any { it.pageNumber == pageIdx }
                    val pageAnnotationCount = annotations.count { it.pageNumber == pageIdx }
                    val sectionForPage = sections.firstOrNull { it.pageIndex == pageIdx }

                    ThumbnailPageItem(
                        pageIndex = pageIdx,
                        isCurrentPage = isCurrent,
                        hasBookmark = hasBookmark,
                        annotationCount = pageAnnotationCount,
                        sectionTitle = sectionForPage?.title,
                        pdfEngine = pdfEngine,
                        onClick = { onPageSelected(pageIdx) }
                    )
                }
            }
        }
    }
}

/**
 * Individual Page Surface Sheet rendering the crisp PDF thumbnail preview,
 * with current page highlight, bookmark indicator, and section marker.
 */
@Composable
fun ThumbnailPageItem(
    pageIndex: Int,
    isCurrentPage: Boolean,
    hasBookmark: Boolean,
    annotationCount: Int,
    sectionTitle: String? = null,
    pdfEngine: PdfEngine,
    onClick: () -> Unit
) {
    val cachedThumb = remember(pageIndex) { pdfEngine.getCachedThumbnail(pageIndex) }
    val thumbnailBitmap by produceState<Bitmap?>(initialValue = cachedThumb, pageIndex) {
        if (cachedThumb == null) {
            value = pdfEngine.renderThumbnail(pageIndex, 180, 250)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("thumbnail_item_$pageIndex")
    ) {
        // Section header pill if this page begins a section
        if (sectionTitle != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isCurrentPage) 8.dp else 2.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .border(
                    width = if (isCurrentPage) 2.5.dp else 0.5.dp,
                    color = if (isCurrentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = "Page ${pageIndex + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }

                // Bookmark indicator on top-right
                if (hasBookmark) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE11D48)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Bookmarked",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Annotations / Notes count indicator on top-left
                if (annotationCount > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF59E0B)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "$annotationCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                // Current page indicator overlay chip
                if (isCurrentPage) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "CURRENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Page ${pageIndex + 1}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isCurrentPage) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

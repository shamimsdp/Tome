package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookmarkEntity
import com.example.engine.PdfEngine

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val gridState = rememberLazyGridState()

    // Auto-scroll to the currently active page when sheet is displayed
    LaunchedEffect(currentPageIndex) {
        if (totalPages > 0 && currentPageIndex in 0 until totalPages) {
            gridState.scrollToItem((currentPageIndex - 2).coerceAtLeast(0))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
                .testTag("thumbnail_grid_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Page Thumbnails",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalPages pages • Tap any page to jump",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_thumbnails_button")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Grid of Thumbnails
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 105.dp),
                state = gridState,
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("thumbnail_grid_list")
            ) {
                items(totalPages, key = { it }) { pageIdx ->
                    val isCurrent = pageIdx == currentPageIndex
                    val hasBookmark = bookmarks.any { it.pageNumber == pageIdx }
                    val pageAnnotationCount = annotations.count { it.pageNumber == pageIdx }

                    ThumbnailPageItem(
                        pageIndex = pageIdx,
                        isCurrentPage = isCurrent,
                        hasBookmark = hasBookmark,
                        annotationCount = pageAnnotationCount,
                        pdfEngine = pdfEngine,
                        onClick = {
                            onPageSelected(pageIdx)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ThumbnailPageItem(
    pageIndex: Int,
    isCurrentPage: Boolean,
    hasBookmark: Boolean,
    annotationCount: Int,
    pdfEngine: PdfEngine,
    onClick: () -> Unit
) {
    val thumbnailBitmap by produceState<Bitmap?>(initialValue = null, pageIndex) {
        value = pdfEngine.renderThumbnail(pageIndex, 160, 220)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("thumbnail_item_$pageIndex")
    ) {
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

                // Annotations indicator on top-left
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

package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnotationEntity
import com.example.data.model.ReadingTheme
import com.example.engine.SearchMatch

@Composable
fun BookPageView(
    bitmap: Bitmap?,
    pageIndex: Int,
    totalPages: Int,
    readingTheme: ReadingTheme,
    isBookmarked: Boolean,
    annotations: List<AnnotationEntity>,
    searchMatches: List<SearchMatch>,
    isHighlightMode: Boolean,
    isReadingRulerEnabled: Boolean,
    readingRulerRatio: Float,
    onTapLeft: () -> Unit,
    onTapRight: () -> Unit,
    onTapCenter: () -> Unit,
    onAddHighlightAtRatio: (Float) -> Unit,
    onAnnotationClick: (AnnotationEntity) -> Unit,
    onRulerPositionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(readingTheme.paperColor)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("book_page_container"),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        // Physical book page sheet with realistic rounded edge and soft depth shadow
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(4.dp),
                    clip = false
                ),
            shape = RoundedCornerShape(4.dp),
            color = readingTheme.paperColor
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (bitmap != null) {
                    // Bitmap Page Rendering with Reading Theme Color Tint
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "PDF Page ${pageIndex + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                drawContent()

                                // Theme overlay tint (for Sepia, Sage, Charcoal, OLED)
                                if (readingTheme != ReadingTheme.DAY) {
                                    val blendColor = when (readingTheme) {
                                        ReadingTheme.SEPIA -> Color(0x28D4A373)
                                        ReadingTheme.SAGE -> Color(0x2052796F)
                                        ReadingTheme.CHARCOAL -> Color(0xD01E222A)
                                        ReadingTheme.OLED_NIGHT -> Color(0xE8000000)
                                        else -> Color.Transparent
                                    }
                                    drawRect(blendColor)
                                }
                            }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = readingTheme.accentColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Realistic book spine shadow in the left gutter (or center when in two-page)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(28.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    readingTheme.spineShadowColor,
                                    readingTheme.spineShadowColor.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Soft right edge page curl gradient
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(14.dp)
                        .align(Alignment.CenterEnd)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    readingTheme.spineShadowColor.copy(alpha = 0.04f),
                                    readingTheme.spineShadowColor.copy(alpha = 0.12f)
                                )
                            )
                        )
                )

                // Highlighting & Annotation Overlays
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Draw all annotations for this page
                    for (ann in annotations) {
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(ann.colorHex))
                        } catch (_: Exception) {
                            Color(0xFFFFEB3B)
                        }

                        val top = ann.topRatio * canvasHeight
                        val height = ann.heightRatio * canvasHeight
                        val left = ann.leftRatio * canvasWidth
                        val width = ann.widthRatio * canvasWidth

                        // Draw soft marker highlighter rectangle
                        drawRoundRect(
                            color = parsedColor.copy(alpha = 0.42f),
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        // Draw small indicator bar on margin
                        drawRoundRect(
                            color = parsedColor,
                            topLeft = Offset(left - 8f, top),
                            size = Size(4f, height),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }

                    // Search Matches highlight on page
                    for (match in searchMatches) {
                        if (match.pageNumber == pageIndex) {
                            val top = match.verticalRatio * canvasHeight
                            drawRoundRect(
                                color = Color(0xFFFF9800).copy(alpha = 0.65f),
                                topLeft = Offset(canvasWidth * 0.08f, top),
                                size = Size(canvasWidth * 0.84f, 28f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                }

                // Interactive Annotation Note Badges
                annotations.forEach { annotation ->
                    val topOffset = (containerHeight.value * annotation.topRatio).dp
                    Box(
                        modifier = Modifier
                            .offset(y = topOffset)
                            .padding(start = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(annotation.colorHex)).copy(alpha = 0.95f)
                                } catch (_: Exception) {
                                    Color(0xFFFFEB3B)
                                }
                            )
                            .clickable { onAnnotationClick(annotation) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("annotation_badge_${annotation.id}")
                    ) {
                        Text(
                            text = if (annotation.note.isNotBlank()) "✎ ${annotation.tag}" else "Highlight",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Dog-Ear Ribbon Bookmark in top-right corner
                AnimatedVisibility(
                    visible = isBookmarked,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("dog_ear_bookmark"),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val path = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(w, 0f)
                                lineTo(w, h)
                                close()
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFFE11D48) // Crimson bookmark ribbon
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Bookmarked",
                            tint = Color.White,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(top = 4.dp, end = 4.dp)
                        )
                    }
                }

                // Reading Ruler Guide Overlay
                if (isReadingRulerEnabled) {
                    val rulerY = (containerHeight.value * readingRulerRatio).dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = rulerY)
                            .height(34.dp)
                            .background(Color(0x303B82F6))
                            .border(1.5.dp, Color(0xFF3B82F6).copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val newRatio = (rulerY.toPx() + dragAmount.y) / size.height
                                    onRulerPositionChange(newRatio)
                                }
                            }
                            .testTag("reading_ruler_overlay")
                    )
                }

                // Touch & Tap Zones Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isHighlightMode) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val xRatio = offset.x / size.width
                                    val yRatio = offset.y / size.height

                                    if (isHighlightMode) {
                                        onAddHighlightAtRatio(yRatio)
                                    } else {
                                        when {
                                            xRatio < 0.22f -> onTapLeft()
                                            xRatio > 0.78f -> onTapRight()
                                            else -> onTapCenter()
                                        }
                                    }
                                }
                            )
                        }
                )
            }
        }
    }
}

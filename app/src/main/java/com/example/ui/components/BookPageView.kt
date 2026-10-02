package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnotationEntity
import com.example.data.model.ReadingTheme
import com.example.engine.PdfEngine
import com.example.engine.SearchMatch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

enum class FlipDirection {
    NEXT,
    PREV
}

/**
 * Authentic 3D Book Page View with Unified Dual-Slot Persistent Architecture.
 * Eliminates all post-flip flickering by keeping the base page slot persistent in the layout tree,
 * preserving hardware textures and pre-cached bitmaps across all page transitions.
 */
@Composable
fun BookPageView(
    bitmap: Bitmap?,
    pageIndex: Int,
    totalPages: Int,
    pdfEngine: PdfEngine,
    readingTheme: ReadingTheme,
    isBookmarked: Boolean,
    annotations: List<AnnotationEntity>,
    searchMatches: List<SearchMatch>,
    isHighlightMode: Boolean,
    isReadingRulerEnabled: Boolean,
    readingRulerRatio: Float,
    isPageFlipEnabled: Boolean = true,
    pageTurnDelta: Int = 1,
    onTapLeft: () -> Unit,
    onTapRight: () -> Unit,
    onTapCenter: () -> Unit,
    onAddHighlightAtRatio: (Float) -> Unit,
    onAnnotationClick: (AnnotationEntity) -> Unit,
    onRulerPositionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val flipProgress = remember { Animatable(0f) }
    var isFlipping by remember { mutableStateOf(false) }
    var flipDirection by remember { mutableStateOf(FlipDirection.NEXT) }

    // Target page waiting to be acknowledged by the ViewModel
    var pendingTargetPage by remember { mutableStateOf<Int?>(null) }

    // Persistent in-memory bitmap cache for seamless instant page switching
    val localBitmapCache = remember { mutableMapOf<Int, Bitmap>() }

    // Update local cache whenever bitmap or pageIndex updates
    if (bitmap != null) {
        localBitmapCache[pageIndex] = bitmap
    }
    pdfEngine.getCachedBitmap(pageIndex)?.let {
        localBitmapCache[pageIndex] = it
    }

    // Synchronize flip completion with ViewModel's pageIndex emission
    LaunchedEffect(pageIndex) {
        if (pendingTargetPage != null && pageIndex == pendingTargetPage) {
            pendingTargetPage = null
            flipProgress.snapTo(0f)
            isFlipping = false
        } else if (pendingTargetPage == null && !isFlipping) {
            flipProgress.snapTo(0f)
        }
    }

    // Proactively pre-render adjacent pages into memory so they are guaranteed ready
    LaunchedEffect(pageIndex, totalPages) {
        val nextIdx = pageIndex + 1
        if (nextIdx < totalPages && !localBitmapCache.containsKey(nextIdx)) {
            val b = pdfEngine.getCachedBitmap(nextIdx) ?: pdfEngine.renderPage(nextIdx)
            if (b != null) localBitmapCache[nextIdx] = b
        }
        val prevIdx = pageIndex - 1
        if (prevIdx >= 0 && !localBitmapCache.containsKey(prevIdx)) {
            val b = pdfEngine.getCachedBitmap(prevIdx) ?: pdfEngine.renderPage(prevIdx)
            if (b != null) localBitmapCache[prevIdx] = b
        }
    }

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(4.dp),
                    clip = false
                )
        ) {
            // =========================================================================
            // 1. BASE LAYER (ALWAYS MOUNTED & PERSISTENT — ZERO RE-ALLOCATION FLICKER)
            // =========================================================================
            // During flip next: Base layer displays next page (pageIndex + 1).
            // When flip completes: Base layer continues displaying that page as active.
            // During flip prev: Base layer displays current page (pageIndex).
            // At resting: Base layer displays current page (pageIndex).
            val basePage = when {
                isFlipping && flipDirection == FlipDirection.NEXT && pageIndex + 1 < totalPages -> pageIndex + 1
                else -> pageIndex
            }

            val baseBitmap = localBitmapCache[basePage]
                ?: pdfEngine.getCachedBitmap(basePage)
                ?: if (basePage == pageIndex) bitmap else null

            SinglePageSheet(
                pageIndex = basePage,
                renderedBitmap = baseBitmap,
                readingTheme = readingTheme,
                isBookmarked = isBookmarked && basePage == pageIndex,
                annotations = annotations,
                searchMatches = searchMatches,
                containerHeight = containerHeight,
                isReadingRulerEnabled = isReadingRulerEnabled && !isFlipping,
                readingRulerRatio = readingRulerRatio,
                onAnnotationClick = onAnnotationClick,
                onRulerPositionChange = onRulerPositionChange,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
            )

            // Dynamic drop shadow cast on the base layer during flip
            if (isFlipping) {
                val progress = flipProgress.value.coerceIn(0f, 1f)
                val shadowAlpha = sin(progress * PI.toFloat()) * 0.40f
                val shadowWidth = if (flipDirection == FlipDirection.NEXT) {
                    (containerWidth.value * (1f - progress) * 0.40f).dp.coerceAtLeast(4.dp)
                } else {
                    (containerWidth.value * progress * 0.40f).dp.coerceAtLeast(4.dp)
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(shadowWidth)
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = shadowAlpha),
                                    Color.Black.copy(alpha = shadowAlpha * 0.35f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // =========================================================================
            // 2. TURNING LAYER (3D Rotating Sheet anchored at left spine)
            // =========================================================================
            if (isFlipping) {
                val progress = flipProgress.value.coerceIn(0f, 1f)
                val rotationY = if (flipDirection == FlipDirection.NEXT) {
                    -180f * progress
                } else {
                    -180f * (1f - progress)
                }
                val turningPage = if (flipDirection == FlipDirection.NEXT) pageIndex else pageIndex - 1
                val turningBitmap = localBitmapCache[turningPage]
                    ?: pdfEngine.getCachedBitmap(turningPage)
                    ?: if (turningPage == pageIndex) bitmap else null
                val isFrontFace = rotationY >= -90f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.rotationY = rotationY
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            cameraDistance = 28f * density
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                ) {
                    if (isFrontFace) {
                        SinglePageSheet(
                            pageIndex = turningPage,
                            renderedBitmap = turningBitmap,
                            readingTheme = readingTheme,
                            isBookmarked = isBookmarked && turningPage == pageIndex,
                            annotations = annotations,
                            searchMatches = searchMatches,
                            containerHeight = containerHeight,
                            modifier = Modifier.fillMaxSize()
                        )

                        // 3D curl lighting: concave crease shadow + convex specular fold highlight
                        val curlIntensity = sin(progress * PI.toFloat())
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        0.0f to Color.Transparent,
                                        (0.35f + progress * 0.3f).coerceIn(0f, 1f) to Color.Black.copy(alpha = curlIntensity * 0.25f),
                                        (0.55f + progress * 0.3f).coerceIn(0f, 1f) to Color.White.copy(alpha = curlIntensity * 0.30f),
                                        1.0f to Color.Black.copy(alpha = curlIntensity * 0.16f)
                                    )
                                )
                        )
                    } else {
                        // Verso (backside of page turning over)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    this.rotationY = 180f
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                        ) {
                            PageVersoSheet(
                                readingTheme = readingTheme,
                                frontBitmap = turningBitmap,
                                modifier = Modifier.fillMaxSize()
                            )

                            val curlIntensity = sin(progress * PI.toFloat())
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            0.0f to Color.Black.copy(alpha = curlIntensity * 0.15f),
                                            0.4f to Color.White.copy(alpha = curlIntensity * 0.20f),
                                            1.0f to Color.Black.copy(alpha = curlIntensity * 0.20f)
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 3. INTERACTIVE GESTURE DETECTOR
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pageIndex, isHighlightMode, isPageFlipEnabled, totalPages) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var totalDragX = 0f
                            var isDrag = false
                            val touchSlop = viewConfiguration.touchSlop

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (!change.pressed) {
                                    // Pointer released
                                    if (isDrag && isFlipping) {
                                        change.consume()
                                        val currentP = flipProgress.value
                                        coroutineScope.launch {
                                            if (currentP > 0.22f) {
                                                // Complete the flip smoothly with natural physics
                                                val remainingDuration = (220 * (1f - currentP)).toInt().coerceIn(80, 220)
                                                flipProgress.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = tween(
                                                        durationMillis = remainingDuration,
                                                        easing = FastOutSlowInEasing
                                                    )
                                                )

                                                // Record pending target page to synchronize handoff
                                                val targetPage = if (flipDirection == FlipDirection.NEXT) pageIndex + 1 else pageIndex - 1
                                                pendingTargetPage = targetPage

                                                // Advance in ViewModel
                                                if (flipDirection == FlipDirection.NEXT) {
                                                    onTapRight()
                                                } else {
                                                    onTapLeft()
                                                }

                                                // Safety timeout so UI never remains in flipping state
                                                launch {
                                                    delay(350)
                                                    if (pendingTargetPage != null) {
                                                        pendingTargetPage = null
                                                        flipProgress.snapTo(0f)
                                                        isFlipping = false
                                                    }
                                                }
                                            } else {
                                                // Cancel flip: elastic snap back to resting position
                                                flipProgress.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                )
                                                isFlipping = false
                                            }
                                        }
                                    } else if (!isDrag) {
                                        // Tap gesture handling
                                        val xRatio = down.position.x / size.width
                                        val yRatio = down.position.y / size.height

                                        if (isHighlightMode) {
                                            onAddHighlightAtRatio(yRatio)
                                        } else {
                                            when {
                                                xRatio < 0.25f -> {
                                                    // Left edge tap: Previous Page
                                                    if (isPageFlipEnabled && pageIndex > 0) {
                                                        coroutineScope.launch {
                                                            isFlipping = true
                                                            flipDirection = FlipDirection.PREV
                                                            pendingTargetPage = pageIndex - 1
                                                            flipProgress.snapTo(0f)
                                                            flipProgress.animateTo(
                                                                targetValue = 1f,
                                                                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                                                            )
                                                            onTapLeft()
                                                            launch {
                                                                delay(350)
                                                                if (pendingTargetPage != null) {
                                                                    pendingTargetPage = null
                                                                    flipProgress.snapTo(0f)
                                                                    isFlipping = false
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        onTapLeft()
                                                    }
                                                }
                                                xRatio > 0.75f -> {
                                                    // Right edge tap: Next Page
                                                    if (isPageFlipEnabled && pageIndex < totalPages - 1) {
                                                        coroutineScope.launch {
                                                            isFlipping = true
                                                            flipDirection = FlipDirection.NEXT
                                                            pendingTargetPage = pageIndex + 1
                                                            flipProgress.snapTo(0f)
                                                            flipProgress.animateTo(
                                                                targetValue = 1f,
                                                                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                                                            )
                                                            onTapRight()
                                                            launch {
                                                                delay(350)
                                                                if (pendingTargetPage != null) {
                                                                    pendingTargetPage = null
                                                                    flipProgress.snapTo(0f)
                                                                    isFlipping = false
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        onTapRight()
                                                    }
                                                }
                                                else -> onTapCenter()
                                            }
                                        }
                                    }
                                    break
                                } else {
                                    // Pointer dragged
                                    val dragDelta = change.position.x - change.previousPosition.x
                                    totalDragX += dragDelta

                                    if (!isDrag && abs(totalDragX) > touchSlop) {
                                        isDrag = true
                                        if (isPageFlipEnabled) {
                                            if (totalDragX < 0 && pageIndex < totalPages - 1) {
                                                isFlipping = true
                                                flipDirection = FlipDirection.NEXT
                                            } else if (totalDragX > 0 && pageIndex > 0) {
                                                isFlipping = true
                                                flipDirection = FlipDirection.PREV
                                            }
                                        }
                                    }

                                    if (isDrag) {
                                        change.consume()
                                        if (isFlipping) {
                                            val p = if (flipDirection == FlipDirection.NEXT) {
                                                (-totalDragX / size.width).coerceIn(0f, 1f)
                                            } else {
                                                (totalDragX / size.width).coerceIn(0f, 1f)
                                            }
                                            coroutineScope.launch {
                                                flipProgress.snapTo(p)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            )
        }
    }
}

/**
 * Individual Page Surface Sheet rendering the PDF bitmap, themes, annotations, and bookmarks.
 * Uses cached ImageBitmap to eliminate texture re-allocations and GC stutter.
 */
@Composable
private fun SinglePageSheet(
    pageIndex: Int,
    renderedBitmap: Bitmap?,
    readingTheme: ReadingTheme,
    isBookmarked: Boolean,
    annotations: List<AnnotationEntity>,
    searchMatches: List<SearchMatch>,
    containerHeight: Dp,
    isReadingRulerEnabled: Boolean = false,
    readingRulerRatio: Float = 0.3f,
    onAnnotationClick: ((AnnotationEntity) -> Unit)? = null,
    onRulerPositionChange: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(renderedBitmap) {
        renderedBitmap?.asImageBitmap()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            },
        shape = RoundedCornerShape(4.dp),
        color = readingTheme.paperColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageBitmap != null) {
                // Bitmap Page Rendering with Reading Theme Color Tint
                Image(
                    bitmap = imageBitmap,
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

            // Realistic book spine shadow in the left gutter (bound book effect)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(26.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                readingTheme.spineShadowColor.copy(alpha = 0.32f),
                                readingTheme.spineShadowColor.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Soft right edge page curl gradient (simulates paper stack depth)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(14.dp)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                readingTheme.spineShadowColor.copy(alpha = 0.05f),
                                readingTheme.spineShadowColor.copy(alpha = 0.14f)
                            )
                        )
                    )
            )

            // Highlighting & Annotation Overlays
            if (annotations.isNotEmpty() || searchMatches.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Draw all annotations for this page
                    for (ann in annotations) {
                        if (ann.pageNumber == pageIndex) {
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

                            // Draw indicator bar on margin
                            drawRoundRect(
                                color = parsedColor,
                                topLeft = Offset(left - 8f, top),
                                size = Size(4f, height),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                        }
                    }

                    // Search Matches highlight on page with amber highlight & margin indicator
                    for (match in searchMatches) {
                        if (match.pageNumber == pageIndex) {
                            val top = match.verticalRatio * canvasHeight
                            // Highlight strip
                            drawRoundRect(
                                color = Color(0xFFFFB300).copy(alpha = 0.50f),
                                topLeft = Offset(canvasWidth * 0.08f, top),
                                size = Size(canvasWidth * 0.84f, 32f),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            // Left margin amber search pin
                            drawRoundRect(
                                color = Color(0xFFF59E0B),
                                topLeft = Offset(canvasWidth * 0.05f, top),
                                size = Size(6f, 32f),
                                cornerRadius = CornerRadius(3f, 3f)
                            )
                        }
                    }
                }
            }

            // Interactive Annotation Note Badges
            annotations.filter { it.pageNumber == pageIndex }.forEach { annotation ->
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
                        .clickable { onAnnotationClick?.invoke(annotation) }
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
                                onRulerPositionChange?.invoke(newRatio)
                            }
                        }
                        .testTag("reading_ruler_overlay")
                )
            }
        }
    }
}

/**
 * Backside (Verso) of a turning physical paper page.
 * Replicates realistic book paper with slight translucent text bleed-through and spine crease.
 */
@Composable
private fun PageVersoSheet(
    readingTheme: ReadingTheme,
    frontBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val versoImageBitmap = remember(frontBitmap) {
        frontBitmap?.asImageBitmap()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            },
        shape = RoundedCornerShape(4.dp),
        color = readingTheme.paperColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Faint mirrored bleed-through of front print
            if (versoImageBitmap != null) {
                Image(
                    bitmap = versoImageBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = -1f
                            alpha = 0.10f
                        }
                )
            }

            // Spine shadow on the right edge (as verso is on the left side of the book)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(24.dp)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                readingTheme.spineShadowColor.copy(alpha = 0.12f),
                                readingTheme.spineShadowColor.copy(alpha = 0.30f)
                            )
                        )
                    )
            )

            // Paper grain / subtle tone
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.04f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.05f)
                            )
                        )
                    )
            )
        }
    }
}

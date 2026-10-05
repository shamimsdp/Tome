package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnnotationEntity
import com.example.data.model.PageElementEntity
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
    isDarkTheme: Boolean = false,
    isBookmarked: Boolean,
    annotations: List<AnnotationEntity>,
    searchMatches: List<SearchMatch>,
    isHighlightMode: Boolean,
    isReadingRulerEnabled: Boolean,
    readingRulerRatio: Float,
    isPageFlipEnabled: Boolean = true,
    pageTurnDelta: Int = 1,
    pageElements: List<PageElementEntity> = emptyList(),
    isEditElementsMode: Boolean = false,
    onTapLeft: () -> Unit,
    onTapRight: () -> Unit,
    onTapCenter: () -> Unit,
    onAddHighlightAtRatio: (Float) -> Unit,
    onAnnotationClick: (AnnotationEntity) -> Unit,
    onRulerPositionChange: (Float) -> Unit,
    onUpdateElementPosition: ((PageElementEntity, Float, Float) -> Unit)? = null,
    onDeleteElement: ((String) -> Unit)? = null,
    onEditElement: ((PageElementEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val flipProgress = remember { Animatable(0f) }
    var isFlipping by remember { mutableStateOf(false) }
    var flipDirection by remember { mutableStateOf(FlipDirection.NEXT) }
    var flipSourcePage by remember { mutableIntStateOf(pageIndex) }
    var flipTargetPage by remember { mutableIntStateOf(pageIndex) }

    // Pinch-to-zoom & Pan state
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Helper to keep pan offset within visible scaled page boundaries
    fun clampPan(offset: Offset, scale: Float, width: Float, height: Float): Offset {
        if (scale <= 1.0f) return Offset.Zero
        val maxPanX = ((width * scale) - width) / 2f
        val maxPanY = ((height * scale) - height) / 2f
        return Offset(
            x = offset.x.coerceIn(-maxPanX, maxPanX),
            y = offset.y.coerceIn(-maxPanY, maxPanY)
        )
    }

    // Reset zoom and pan whenever user navigates to a new page
    LaunchedEffect(pageIndex) {
        zoomScale = 1.0f
        panOffset = Offset.Zero
    }

    // Proactively pre-render adjacent pages into memory so they are guaranteed ready for flip
    LaunchedEffect(pageIndex, totalPages) {
        val nextIdx = pageIndex + 1
        if (nextIdx < totalPages && !pdfEngine.isPageCached(nextIdx)) {
            pdfEngine.renderPage(nextIdx)
        }
        val prevIdx = pageIndex - 1
        if (prevIdx >= 0 && !pdfEngine.isPageCached(prevIdx)) {
            pdfEngine.renderPage(prevIdx)
        }
        val nextNextIdx = pageIndex + 2
        if (nextNextIdx < totalPages && !pdfEngine.isPageCached(nextNextIdx)) {
            pdfEngine.renderPage(nextNextIdx)
        }
    }

    // Direct cache query: never substitute an incorrect page's bitmap
    fun getPageBitmap(p: Int): Bitmap? {
        val cached = pdfEngine.getCachedBitmap(p)
        if (cached != null) return cached
        if (p == pageIndex && bitmap != null) return bitmap
        return null
    }

    val pageBackground = when {
        readingTheme == ReadingTheme.OLED_NIGHT -> Color(0xFF000000)
        readingTheme == ReadingTheme.CHARCOAL -> Color(0xFF1E222A)
        isDarkTheme && readingTheme == ReadingTheme.DAY -> Color(0xFF121824)
        else -> readingTheme.paperColor
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(pageBackground)
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
            // During flip next: Base layer displays target page (pageIndex + 1).
            // During flip prev: Base layer displays current source page (pageIndex).
            // At resting: Base layer displays current page (pageIndex).
            val basePage = when {
                isFlipping && flipDirection == FlipDirection.NEXT -> flipTargetPage
                isFlipping && flipDirection == FlipDirection.PREV -> flipSourcePage
                else -> pageIndex
            }

            val baseBitmap = getPageBitmap(basePage)

            // Zoom & Pan Container for inspecting fine details
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
            ) {
                SinglePageSheet(
                    pageIndex = basePage,
                    renderedBitmap = baseBitmap,
                    pdfEngine = pdfEngine,
                    readingTheme = readingTheme,
                    isDarkTheme = isDarkTheme,
                    isBookmarked = isBookmarked && basePage == pageIndex,
                    annotations = if (basePage == pageIndex) annotations else emptyList(),
                    searchMatches = searchMatches,
                    containerHeight = containerHeight,
                    isReadingRulerEnabled = isReadingRulerEnabled && !isFlipping && zoomScale <= 1.05f,
                    readingRulerRatio = readingRulerRatio,
                    pageElements = if (basePage == pageIndex) pageElements else emptyList(),
                    isEditElementsMode = isEditElementsMode && !isFlipping && zoomScale <= 1.05f,
                    onAnnotationClick = onAnnotationClick,
                    onRulerPositionChange = onRulerPositionChange,
                    onUpdateElementPosition = onUpdateElementPosition,
                    onDeleteElement = onDeleteElement,
                    onEditElement = onEditElement,
                    modifier = Modifier.fillMaxSize()
                )
            }

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
                val turningPage = if (flipDirection == FlipDirection.NEXT) flipSourcePage else flipTargetPage
                val turningBitmap = getPageBitmap(turningPage)
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
                            pdfEngine = pdfEngine,
                            readingTheme = readingTheme,
                            isDarkTheme = isDarkTheme,
                            isBookmarked = isBookmarked && turningPage == pageIndex,
                            annotations = if (turningPage == pageIndex) annotations else emptyList(),
                            searchMatches = searchMatches,
                            containerHeight = containerHeight,
                            pageElements = if (turningPage == pageIndex) pageElements else emptyList(),
                            isEditElementsMode = false,
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
                                pageIndex = turningPage,
                                readingTheme = readingTheme,
                                frontBitmap = turningBitmap,
                                pdfEngine = pdfEngine,
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
            // 3. INTERACTIVE GESTURE DETECTOR (PINCH-TO-ZOOM, PAN, AND 3D PAGE FLIP)
            // =========================================================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pageIndex, isHighlightMode, isEditElementsMode, isPageFlipEnabled, totalPages, zoomScale) {
                        awaitEachGesture {
                            if (isEditElementsMode) return@awaitEachGesture
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var totalDragX = 0f
                            var isDrag = false
                            val touchSlop = viewConfiguration.touchSlop
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressedPointers = event.changes.filter { it.pressed }

                                if (pressedPointers.size >= 2) {
                                    // MULTI-TOUCH: Pinch-to-Zoom & Multi-touch Pan
                                    if (isFlipping) {
                                        coroutineScope.launch {
                                            flipProgress.snapTo(0f)
                                            isFlipping = false
                                        }
                                    }

                                    val zoomFactor = event.calculateZoom()
                                    val panDelta = event.calculatePan()

                                    val newScale = (zoomScale * zoomFactor).coerceIn(1.0f, 5.0f)
                                    zoomScale = newScale

                                    if (newScale > 1.0f) {
                                        panOffset = clampPan(panOffset + panDelta, newScale, w, h)
                                    } else {
                                        panOffset = Offset.Zero
                                    }

                                    event.changes.forEach { it.consume() }
                                } else if (pressedPointers.size == 1) {
                                    val change = pressedPointers.first()

                                    if (zoomScale > 1.05f) {
                                        // ZOOMED IN: 1-finger drag pans around the zoomed page smoothly!
                                        val dragDelta = change.position - change.previousPosition
                                        panOffset = clampPan(panOffset + dragDelta, zoomScale, w, h)
                                        change.consume()
                                    } else {
                                        // 1X NORMAL: Horizontal swipe initiates 3D page flip
                                        val dragDelta = change.position.x - change.previousPosition.x
                                        totalDragX += dragDelta

                                        if (!isDrag && abs(totalDragX) > touchSlop) {
                                            isDrag = true
                                            if (isPageFlipEnabled && !isFlipping) {
                                                if (totalDragX < 0 && pageIndex < totalPages - 1) {
                                                    flipSourcePage = pageIndex
                                                    flipTargetPage = pageIndex + 1
                                                    flipDirection = FlipDirection.NEXT
                                                    isFlipping = true
                                                } else if (totalDragX > 0 && pageIndex > 0) {
                                                    flipSourcePage = pageIndex
                                                    flipTargetPage = pageIndex - 1
                                                    flipDirection = FlipDirection.PREV
                                                    isFlipping = true
                                                }
                                            }
                                        }

                                        if (isDrag) {
                                            change.consume()
                                            if (isFlipping) {
                                                val p = if (flipDirection == FlipDirection.NEXT) {
                                                    (-totalDragX / w).coerceIn(0f, 1f)
                                                } else {
                                                    (totalDragX / w).coerceIn(0f, 1f)
                                                }
                                                coroutineScope.launch {
                                                    flipProgress.snapTo(p)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Pointer released
                                    val change = event.changes.firstOrNull() ?: break

                                    if (zoomScale > 1.05f) {
                                        panOffset = clampPan(panOffset, zoomScale, w, h)
                                    } else if (isDrag && isFlipping) {
                                        change.consume()
                                        val currentP = flipProgress.value
                                        coroutineScope.launch {
                                            if (currentP > 0.22f) {
                                                val remainingDuration = (220 * (1f - currentP)).toInt().coerceIn(80, 220)
                                                flipProgress.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = tween(
                                                        durationMillis = remainingDuration,
                                                        easing = FastOutSlowInEasing
                                                    )
                                                )

                                                isFlipping = false
                                                flipProgress.snapTo(0f)

                                                if (flipDirection == FlipDirection.NEXT) {
                                                    onTapRight()
                                                } else {
                                                    onTapLeft()
                                                }
                                            } else {
                                                flipProgress.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                )
                                                isFlipping = false
                                            }
                                        }
                                    } else if (!isDrag) {
                                        // Tap gesture handling
                                        val xRatio = down.position.x / w
                                        val yRatio = down.position.y / h

                                        if (isHighlightMode) {
                                            onAddHighlightAtRatio(yRatio)
                                        } else if (zoomScale > 1.05f) {
                                            // When inspecting zoomed details, tap toggles controls without accidental page flip
                                            onTapCenter()
                                        } else {
                                            when {
                                                xRatio < 0.25f -> {
                                                    // Left edge tap: Previous Page
                                                    if (isPageFlipEnabled && pageIndex > 0 && !isFlipping) {
                                                        coroutineScope.launch {
                                                            flipSourcePage = pageIndex
                                                            flipTargetPage = pageIndex - 1
                                                            flipDirection = FlipDirection.PREV
                                                            isFlipping = true
                                                            flipProgress.snapTo(0f)
                                                            flipProgress.animateTo(
                                                                targetValue = 1f,
                                                                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                                                            )
                                                            isFlipping = false
                                                            flipProgress.snapTo(0f)
                                                            onTapLeft()
                                                        }
                                                    } else if (!isFlipping) {
                                                        onTapLeft()
                                                    }
                                                }
                                                xRatio > 0.75f -> {
                                                    // Right edge tap: Next Page
                                                    if (isPageFlipEnabled && pageIndex < totalPages - 1 && !isFlipping) {
                                                        coroutineScope.launch {
                                                            flipSourcePage = pageIndex
                                                            flipTargetPage = pageIndex + 1
                                                            flipDirection = FlipDirection.NEXT
                                                            isFlipping = true
                                                            flipProgress.snapTo(0f)
                                                            flipProgress.animateTo(
                                                                targetValue = 1f,
                                                                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                                                            )
                                                            isFlipping = false
                                                            flipProgress.snapTo(0f)
                                                            onTapRight()
                                                        }
                                                    } else if (!isFlipping) {
                                                        onTapRight()
                                                    }
                                                }
                                                else -> onTapCenter()
                                            }
                                        }
                                    }
                                    break
                                }
                            }
                        }
                    }
            )

            // =========================================================================
            // 4. FLOATING PINCH-TO-ZOOM INSPECTION CONTROLS OVERLAY
            // =========================================================================
            AnimatedVisibility(
                visible = zoomScale > 1.05f,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("pinch_zoom_overlay")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val newScale = (zoomScale - 0.5f).coerceAtLeast(1.0f)
                                zoomScale = newScale
                                panOffset = if (newScale <= 1.0f) Offset.Zero else clampPan(panOffset, newScale, containerWidth.value, containerHeight.value)
                            },
                            modifier = Modifier.size(30.dp).testTag("zoom_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Zoom Out",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "${(zoomScale * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 4.dp).testTag("zoom_level_text")
                        )

                        IconButton(
                            onClick = {
                                val newScale = (zoomScale + 0.5f).coerceAtMost(5.0f)
                                zoomScale = newScale
                                panOffset = clampPan(panOffset, newScale, containerWidth.value, containerHeight.value)
                            },
                            modifier = Modifier.size(30.dp).testTag("zoom_in_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Zoom In",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    zoomScale = 1.0f
                                    panOffset = Offset.Zero
                                }
                                .testTag("reset_zoom_button")
                        ) {
                            Text(
                                text = "Reset",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
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
    pdfEngine: PdfEngine,
    readingTheme: ReadingTheme,
    isDarkTheme: Boolean = false,
    isBookmarked: Boolean,
    annotations: List<AnnotationEntity>,
    searchMatches: List<SearchMatch>,
    containerHeight: Dp,
    isReadingRulerEnabled: Boolean = false,
    readingRulerRatio: Float = 0.3f,
    pageElements: List<PageElementEntity> = emptyList(),
    isEditElementsMode: Boolean = false,
    onAnnotationClick: ((AnnotationEntity) -> Unit)? = null,
    onRulerPositionChange: ((Float) -> Unit)? = null,
    onUpdateElementPosition: ((PageElementEntity, Float, Float) -> Unit)? = null,
    onDeleteElement: ((String) -> Unit)? = null,
    onEditElement: ((PageElementEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var pageBitmap by remember(pageIndex, renderedBitmap) {
        mutableStateOf(renderedBitmap ?: pdfEngine.getCachedBitmap(pageIndex))
    }

    LaunchedEffect(pageIndex, renderedBitmap) {
        if (renderedBitmap != null) {
            pageBitmap = renderedBitmap
        } else {
            val cached = pdfEngine.getCachedBitmap(pageIndex)
            if (cached != null) {
                pageBitmap = cached
            } else {
                val b = pdfEngine.renderPage(pageIndex)
                if (b != null) {
                    pageBitmap = b
                }
            }
        }
    }

    val imageBitmap = remember(pageBitmap) {
        pageBitmap?.asImageBitmap()
    }

    val pageColor = when {
        readingTheme == ReadingTheme.OLED_NIGHT -> Color(0xFF000000)
        readingTheme == ReadingTheme.CHARCOAL -> Color(0xFF1E222A)
        isDarkTheme && readingTheme == ReadingTheme.DAY -> Color(0xFF121824)
        else -> readingTheme.paperColor
    }

    // High-contrast low-light color filter for comfortable nighttime reading
    val pageColorFilter = remember(readingTheme, isDarkTheme) {
        when {
            readingTheme == ReadingTheme.OLED_NIGHT -> {
                // True AMOLED pure black inversion
                ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                    -1f,  0f,  0f, 0f, 255f,
                     0f, -1f,  0f, 0f, 255f,
                     0f,  0f, -1f, 0f, 255f,
                     0f,  0f,  0f, 1f,   0f
                )))
            }
            readingTheme == ReadingTheme.CHARCOAL || (isDarkTheme && readingTheme == ReadingTheme.DAY) -> {
                // Soft charcoal low-light matrix (protects eyes from bright white pages)
                ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                    -0.85f,  0f,     0f,    0f, 225f,
                     0f,    -0.85f,  0f,    0f, 225f,
                     0f,     0f,    -0.80f, 0f, 225f,
                     0f,     0f,     0f,    1f,   0f
                )))
            }
            else -> null
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            },
        shape = RoundedCornerShape(4.dp),
        color = pageColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageBitmap != null) {
                // Bitmap Page Rendering with Reading Theme Color Tint and Low-Light ColorFilter
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "PDF Page ${pageIndex + 1}",
                    colorFilter = pageColorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()

                            // Theme overlay tint (for Sepia, Sage)
                            if (readingTheme == ReadingTheme.SEPIA) {
                                drawRect(Color(0x28D4A373))
                            } else if (readingTheme == ReadingTheme.SAGE) {
                                drawRect(Color(0x2052796F))
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

            // User-added Page Elements (Text, Images, Stamps)
            PageElementsOverlay(
                elements = pageElements,
                pageIndex = pageIndex,
                isEditMode = isEditElementsMode,
                onUpdateElementPosition = { elem, newX, newY ->
                    onUpdateElementPosition?.invoke(elem, newX, newY)
                },
                onDeleteElement = { id ->
                    onDeleteElement?.invoke(id)
                },
                onEditElement = { elem ->
                    onEditElement?.invoke(elem)
                }
            )
        }
    }
}

/**
 * Backside (Verso) of a turning physical paper page.
 * Replicates realistic book paper with slight translucent text bleed-through and spine crease.
 */
@Composable
private fun PageVersoSheet(
    pageIndex: Int,
    readingTheme: ReadingTheme,
    frontBitmap: Bitmap?,
    pdfEngine: PdfEngine,
    modifier: Modifier = Modifier
) {
    var versoBitmap by remember(frontBitmap, pageIndex) {
        mutableStateOf(frontBitmap ?: pdfEngine.getCachedBitmap(pageIndex))
    }

    LaunchedEffect(pageIndex, frontBitmap) {
        if (frontBitmap != null) {
            versoBitmap = frontBitmap
        } else {
            val cached = pdfEngine.getCachedBitmap(pageIndex) ?: pdfEngine.renderPage(pageIndex)
            if (cached != null) {
                versoBitmap = cached
            }
        }
    }

    val versoImageBitmap = remember(versoBitmap) {
        versoBitmap?.asImageBitmap()
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

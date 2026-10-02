package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PageElementEntity
import kotlin.math.roundToInt

@Composable
fun PageElementsOverlay(
    elements: List<PageElementEntity>,
    pageIndex: Int,
    isEditMode: Boolean,
    onUpdateElementPosition: (PageElementEntity, newXRatio: Float, newYRatio: Float) -> Unit,
    onDeleteElement: (String) -> Unit,
    onEditElement: (PageElementEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val pageElements = remember(elements, pageIndex) {
        elements.filter { it.pageNumber == pageIndex }
    }

    if (pageElements.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        for (element in pageElements) {
            RenderSinglePageElement(
                element = element,
                containerWidthPx = widthPx,
                containerHeightPx = heightPx,
                isEditMode = isEditMode,
                onUpdatePosition = { newX, newY ->
                    onUpdateElementPosition(element, newX, newY)
                },
                onDelete = { onDeleteElement(element.id) },
                onEdit = { onEditElement(element) }
            )
        }
    }
}

@Composable
private fun RenderSinglePageElement(
    element: PageElementEntity,
    containerWidthPx: Float,
    containerHeightPx: Float,
    isEditMode: Boolean,
    onUpdatePosition: (Float, Float) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var offsetX by remember(element.xRatio, containerWidthPx) {
        mutableFloatStateOf(element.xRatio * containerWidthPx)
    }
    var offsetY by remember(element.yRatio, containerHeightPx) {
        mutableFloatStateOf(element.yRatio * containerHeightPx)
    }

    val dragModifier = if (isEditMode) {
        Modifier.pointerInput(element.id) {
            detectDragGestures(
                onDrag = { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(0f, containerWidthPx - 60f)
                    offsetY = (offsetY + dragAmount.y).coerceIn(0f, containerHeightPx - 60f)
                },
                onDragEnd = {
                    val finalXRatio = (offsetX / containerWidthPx).coerceIn(0f, 0.95f)
                    val finalYRatio = (offsetY / containerHeightPx).coerceIn(0f, 0.95f)
                    onUpdatePosition(finalXRatio, finalYRatio)
                }
            )
        }
    } else Modifier

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .then(dragModifier)
            .testTag("page_element_${element.id}")
    ) {
        when (element.elementType) {
            "TEXT" -> {
                val textColor = remember(element.colorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(element.colorHex))
                    } catch (_: Exception) {
                        Color.Black
                    }
                }
                val bgColor = remember(element.backgroundColorHex) {
                    try {
                        if (element.backgroundColorHex == "#00000000") Color.Transparent
                        else Color(android.graphics.Color.parseColor(element.backgroundColorHex))
                    } catch (_: Exception) {
                        Color.Transparent
                    }
                }

                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isEditMode) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else if (bgColor != Color.Transparent) androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
                    else null,
                    shadowElevation = if (bgColor != Color.Transparent) 2.dp else 0.dp,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clickable(enabled = isEditMode, onClick = onEdit)
                ) {
                    Text(
                        text = element.content,
                        color = textColor,
                        fontSize = element.fontSize.sp,
                        fontWeight = if (element.isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (element.isItalic) FontStyle.Italic else FontStyle.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            "IMAGE" -> {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    border = if (isEditMode) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isEditMode, onClick = onEdit)
                ) {
                    AsyncImage(
                        model = element.content,
                        contentDescription = "PDF Page Element Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            "STAMP" -> {
                val stampColor = remember(element.colorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(element.colorHex))
                    } catch (_: Exception) {
                        Color(0xFF059669)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = stampColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, stampColor),
                    modifier = Modifier.clickable(enabled = isEditMode, onClick = onEdit)
                ) {
                    Text(
                        text = element.content,
                        color = stampColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Delete button in edit mode
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Element",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

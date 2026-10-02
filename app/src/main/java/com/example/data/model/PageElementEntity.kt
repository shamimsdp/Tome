package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "page_elements")
data class PageElementEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val pageNumber: Int,
    val elementType: String, // "TEXT", "IMAGE", "STAMP"
    val content: String, // Text content OR image Uri string OR stamp identifier
    val xRatio: Float = 0.5f, // 0.0 to 1.0 (relative to page width)
    val yRatio: Float = 0.5f, // 0.0 to 1.0 (relative to page height)
    val widthRatio: Float = 0.45f,
    val heightRatio: Float = 0.12f,
    val colorHex: String = "#1E293B",
    val backgroundColorHex: String = "#FFFBEB",
    val fontSize: Int = 16,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

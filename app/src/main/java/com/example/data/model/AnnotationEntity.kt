package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val pageNumber: Int,
    val highlightedText: String,
    val colorHex: String = "#FFEB3B", // Default Amber/Yellow highlighter
    val note: String = "",
    val tag: String = "Key Idea", // "Key Idea", "Important", "Question", "Quote", "Vocabulary"
    val topRatio: Float = 0.2f, // 0.0 to 1.0 relative page coordinates
    val heightRatio: Float = 0.04f,
    val leftRatio: Float = 0.1f,
    val widthRatio: Float = 0.8f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true
)

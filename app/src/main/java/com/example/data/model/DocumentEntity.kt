package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val author: String = "Unknown Author",
    val filePath: String,
    val sourceType: String = SOURCE_LOCAL, // BUILT_IN, LOCAL_SAF, GOOGLE_DRIVE, CLOUD
    val totalPages: Int = 1,
    val currentPage: Int = 0,
    val progressPercent: Float = 0f,
    val fileSizeBytes: Long = 0L,
    val isOfflineAvailable: Boolean = true,
    val isCloudSynced: Boolean = true,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val description: String = ""
) {
    companion object {
        const val SOURCE_BUILT_IN = "BUILT_IN"
        const val SOURCE_LOCAL = "LOCAL_SAF"
        const val SOURCE_GOOGLE_DRIVE = "GOOGLE_DRIVE"
        const val SOURCE_CLOUD = "CLOUD"
    }
}

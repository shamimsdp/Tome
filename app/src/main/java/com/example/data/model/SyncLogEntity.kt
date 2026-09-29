package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val action: String, // "UPLOAD_NOTES", "DOWNLOAD_NOTES", "OFFLINE_CACHE"
    val status: String, // "SUCCESS", "PENDING", "OFFLINE"
    val details: String
)

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY lastReadTimestamp DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isOfflineAvailable = 1 ORDER BY lastReadTimestamp DESC")
    fun getOfflineDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    fun observeDocumentById(id: String): Flow<DocumentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("UPDATE documents SET currentPage = :page, progressPercent = :percent, lastReadTimestamp = :timestamp WHERE id = :id")
    suspend fun updateProgress(id: String, page: Int, percent: Float, timestamp: Long)

    @Query("UPDATE documents SET totalReadingTimeSeconds = totalReadingTimeSeconds + :additionalSeconds, lastReadTimestamp = :timestamp WHERE id = :id")
    suspend fun incrementReadingTime(id: String, additionalSeconds: Long, timestamp: Long)

    @Query("UPDATE documents SET isOfflineAvailable = :isOffline WHERE id = :id")
    suspend fun updateOfflineStatus(id: String, isOffline: Boolean)

    @Query("UPDATE documents SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PageElementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageElementDao {
    @Query("SELECT * FROM page_elements WHERE documentId = :documentId ORDER BY createdAt ASC")
    fun getElementsForDocument(documentId: String): Flow<List<PageElementEntity>>

    @Query("SELECT * FROM page_elements WHERE documentId = :documentId AND pageNumber = :page ORDER BY createdAt ASC")
    fun getElementsForPage(documentId: String, page: Int): Flow<List<PageElementEntity>>

    @Query("SELECT * FROM page_elements WHERE id = :id LIMIT 1")
    suspend fun getElementById(id: String): PageElementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElement(element: PageElementEntity)

    @Update
    suspend fun updateElement(element: PageElementEntity)

    @Query("DELETE FROM page_elements WHERE id = :id")
    suspend fun deleteElementById(id: String)

    @Query("DELETE FROM page_elements WHERE documentId = :documentId")
    suspend fun deleteElementsForDocument(documentId: String)
}

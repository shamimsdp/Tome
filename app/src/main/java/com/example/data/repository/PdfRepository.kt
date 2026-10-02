package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AnnotationDao
import com.example.data.local.BookmarkDao
import com.example.data.local.DocumentDao
import com.example.data.local.PageElementDao
import com.example.data.local.SyncLogDao
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.DocumentEntity
import com.example.data.model.PageElementEntity
import com.example.data.model.SyncLogEntity
import com.example.engine.PdfEngine
import com.example.engine.SamplePdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class PdfRepository(
    private val context: Context,
    private val documentDao: DocumentDao,
    private val bookmarkDao: BookmarkDao,
    private val annotationDao: AnnotationDao,
    private val syncLogDao: SyncLogDao,
    private val pageElementDao: PageElementDao
) {
    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
    val offlineDocuments: Flow<List<DocumentEntity>> = documentDao.getOfflineDocuments()
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
    val allAnnotations: Flow<List<AnnotationEntity>> = annotationDao.getAllAnnotations()
    val recentSyncLogs: Flow<List<SyncLogEntity>> = syncLogDao.getRecentLogs()

    suspend fun initializeSamplesIfNeeded(pdfEngine: PdfEngine) = withContext(Dispatchers.IO) {
        val existing = allDocuments.first()
        val sampleFiles = SamplePdfGenerator.ensureSampleBooksExist(context)
        val sampleEntities = mutableListOf<DocumentEntity>()

        for ((index, book) in SamplePdfGenerator.sampleBooks.withIndex()) {
            val bookId = "sample_${book.filename.removeSuffix(".pdf")}"
            if (existing.none { it.id == bookId }) {
                val file = sampleFiles.getOrNull(index) ?: continue
                val totalPages = pdfEngine.openFile(file)
                val doc = DocumentEntity(
                    id = bookId,
                    title = book.title,
                    author = book.author,
                    filePath = file.absolutePath,
                    sourceType = DocumentEntity.SOURCE_BUILT_IN,
                    totalPages = if (totalPages > 0) totalPages else book.pagesText.size,
                    currentPage = 0,
                    progressPercent = 0f,
                    fileSizeBytes = file.length(),
                    isOfflineAvailable = true,
                    isCloudSynced = true,
                    lastReadTimestamp = System.currentTimeMillis() - (index * 3600000L),
                    isFavorite = index == 0,
                    description = book.description
                )
                sampleEntities.add(doc)
            }
        }

        if (sampleEntities.isNotEmpty()) {
            documentDao.insertDocuments(sampleEntities)
        }

            // Seed initial bookmark and highlight note for the first book so the user sees notes right away
            val firstDocId = sampleEntities.firstOrNull()?.id
            if (firstDocId != null) {
                bookmarkDao.insertBookmark(
                    BookmarkEntity(
                        documentId = firstDocId,
                        pageNumber = 0,
                        title = "Chapter I: Dimensions of Reading",
                        note = "Crucial distinction between reading for information vs understanding."
                    )
                )

                annotationDao.insertAnnotation(
                    AnnotationEntity(
                        documentId = firstDocId,
                        pageNumber = 0,
                        highlightedText = "When we read for understanding, our mind is stretched, challenged, and elevated.",
                        colorHex = "#FFEB3B", // Amber Gold
                        note = "Core definition of analytical reading. Remember this when skimming articles.",
                        tag = "Key Takeaway",
                        topRatio = 0.23f,
                        heightRatio = 0.045f,
                        leftRatio = 0.09f,
                        widthRatio = 0.82f
                    )
                )

                annotationDao.insertAnnotation(
                    AnnotationEntity(
                        documentId = firstDocId,
                        pageNumber = 0,
                        highlightedText = "You have not really read a book until you have written between its lines.",
                        colorHex = "#4CAF50", // Mint Green
                        note = "Mortimer Adler quote: Annotating books makes the ideas truly yours.",
                        tag = "Quote",
                        topRatio = 0.36f,
                        heightRatio = 0.045f,
                        leftRatio = 0.09f,
                        widthRatio = 0.82f
                    )
                )

                annotationDao.insertAnnotation(
                    AnnotationEntity(
                        documentId = firstDocId,
                        pageNumber = 2,
                        highlightedText = "Organize your thoughts with colors: gold for principal theses, green for actionable insights.",
                        colorHex = "#03A9F4", // Sky Blue
                        note = "Color coding scheme to adopt across all reading notes!",
                        tag = "Important",
                        topRatio = 0.34f,
                        heightRatio = 0.045f,
                        leftRatio = 0.09f,
                        widthRatio = 0.82f
                    )
                )
            }

            syncLogDao.insertLog(
                SyncLogEntity(
                    action = "INITIAL_SETUP",
                    status = "SUCCESS",
                    details = "Initialized ${sampleEntities.size} classic books with sample annotations."
                )
            )
        }

    suspend fun importLocalPdf(uri: Uri, pdfEngine: PdfEngine): DocumentEntity? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var filename = "Document_${System.currentTimeMillis()}.pdf"

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    val resolvedName = cursor.getString(nameIndex)
                    if (!resolvedName.isNullOrBlank()) {
                        filename = resolvedName
                    }
                }
            }

            val destination = File(context.filesDir, "imported_${UUID.randomUUID()}_$filename")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            val pages = pdfEngine.openFile(destination)
            val doc = DocumentEntity(
                id = UUID.randomUUID().toString(),
                title = filename.removeSuffix(".pdf"),
                author = "Local Import",
                filePath = destination.absolutePath,
                sourceType = DocumentEntity.SOURCE_LOCAL,
                totalPages = if (pages > 0) pages else 1,
                currentPage = 0,
                progressPercent = 0f,
                fileSizeBytes = destination.length(),
                isOfflineAvailable = true,
                isCloudSynced = false,
                lastReadTimestamp = System.currentTimeMillis()
            )

            documentDao.insertDocument(doc)
            syncLogDao.insertLog(
                SyncLogEntity(
                    action = "LOCAL_IMPORT",
                    status = "SUCCESS",
                    details = "Imported '${doc.title}' (${doc.totalPages} pages) for offline access."
                )
            )
            return@withContext doc
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun getDocumentById(id: String): DocumentEntity? = documentDao.getDocumentById(id)

    fun observeDocument(id: String): Flow<DocumentEntity?> = documentDao.observeDocumentById(id)

    suspend fun updateReadingProgress(docId: String, page: Int, totalPages: Int) {
        val percent = if (totalPages > 0) ((page + 1).toFloat() / totalPages.toFloat()) * 100f else 0f
        documentDao.updateProgress(docId, page, percent, System.currentTimeMillis())
    }

    suspend fun incrementReadingTime(docId: String, additionalSeconds: Long) {
        if (additionalSeconds > 0) {
            documentDao.incrementReadingTime(docId, additionalSeconds, System.currentTimeMillis())
        }
    }

    suspend fun toggleFavorite(docId: String, currentFavorite: Boolean) {
        documentDao.updateFavorite(docId, !currentFavorite)
    }

    suspend fun toggleOfflineStatus(docId: String, currentOffline: Boolean) {
        documentDao.updateOfflineStatus(docId, !currentOffline)
        syncLogDao.insertLog(
            SyncLogEntity(
                action = if (!currentOffline) "DOWNLOAD_OFFLINE" else "REMOVE_OFFLINE",
                status = "SUCCESS",
                details = "Updated offline mode state for document $docId"
            )
        )
    }

    suspend fun deleteDocument(doc: DocumentEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(doc.filePath)
            if (file.exists() && doc.sourceType != DocumentEntity.SOURCE_BUILT_IN) {
                file.delete()
            }
        } catch (_: Exception) {}
        documentDao.deleteDocumentById(doc.id)
        annotationDao.deleteAnnotationsForDocument(doc.id)
    }

    // Bookmarks
    fun getBookmarksForDocument(docId: String): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarksForDocument(docId)

    suspend fun isPageBookmarked(docId: String, page: Int): Boolean =
        bookmarkDao.getBookmarkForPage(docId, page) != null

    suspend fun toggleBookmark(docId: String, page: Int, title: String, note: String = "") {
        val existing = bookmarkDao.getBookmarkForPage(docId, page)
        if (existing != null) {
            bookmarkDao.deleteBookmarkById(existing.id)
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    documentId = docId,
                    pageNumber = page,
                    title = title,
                    note = note
                )
            )
        }
    }

    suspend fun deleteBookmark(id: String) = bookmarkDao.deleteBookmarkById(id)

    // Annotations (Highlights & Notes)
    fun getAnnotationsForDocument(docId: String): Flow<List<AnnotationEntity>> =
        annotationDao.getAnnotationsForDocument(docId)

    fun getAnnotationsForPage(docId: String, page: Int): Flow<List<AnnotationEntity>> =
        annotationDao.getAnnotationsForPage(docId, page)

    suspend fun addAnnotation(
        docId: String,
        page: Int,
        text: String,
        colorHex: String,
        note: String,
        tag: String,
        topRatio: Float,
        leftRatio: Float = 0.08f,
        widthRatio: Float = 0.84f,
        heightRatio: Float = 0.045f
    ) {
        val annotation = AnnotationEntity(
            documentId = docId,
            pageNumber = page,
            highlightedText = text,
            colorHex = colorHex,
            note = note,
            tag = tag,
            topRatio = topRatio,
            leftRatio = leftRatio,
            widthRatio = widthRatio,
            heightRatio = heightRatio,
            isSynced = false // marked for cloud sync
        )
        annotationDao.insertAnnotation(annotation)
    }

    suspend fun updateAnnotation(annotation: AnnotationEntity) {
        annotationDao.updateAnnotation(annotation.copy(updatedAt = System.currentTimeMillis(), isSynced = false))
    }

    suspend fun deleteAnnotation(id: String) {
        annotationDao.deleteAnnotationById(id)
    }

    fun exportNotesAsMarkdown(docTitle: String, annotations: List<AnnotationEntity>): String {
        val sb = StringBuilder()
        sb.append("# Reading Notes: $docTitle\n\n")
        sb.append("*Generated by Tome PDF Reader on ${java.text.SimpleDateFormat.getDateInstance().format(java.util.Date())}*\n\n")
        sb.append("---\n\n")

        val grouped = annotations.groupBy { it.pageNumber }
        grouped.toSortedMap().forEach { (page, pageAnnotations) ->
            sb.append("## Page ${page + 1}\n\n")
            pageAnnotations.forEach { ann ->
                sb.append("> \"${ann.highlightedText}\"\n\n")
                if (ann.note.isNotBlank()) {
                    sb.append("**Note (${ann.tag}):** ${ann.note}\n\n")
                }
                sb.append("---\n\n")
            }
        }
        return sb.toString()
    }

    // Page Elements (Text editing, image/stamp elements)
    fun getElementsForDocument(docId: String): Flow<List<PageElementEntity>> =
        pageElementDao.getElementsForDocument(docId)

    fun getElementsForPage(docId: String, page: Int): Flow<List<PageElementEntity>> =
        pageElementDao.getElementsForPage(docId, page)

    suspend fun addPageElement(element: PageElementEntity) {
        pageElementDao.insertElement(element)
    }

    suspend fun updatePageElement(element: PageElementEntity) {
        pageElementDao.updateElement(element)
    }

    suspend fun deletePageElement(id: String) {
        pageElementDao.deleteElementById(id)
    }

    suspend fun addBookmark(docId: String, page: Int, title: String, note: String = "") {
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                documentId = docId,
                pageNumber = page,
                title = title.ifBlank { "Page ${page + 1} Bookmark" },
                note = note
            )
        )
    }

    suspend fun updateBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.updateBookmark(bookmark)
    }
}

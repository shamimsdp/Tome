package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

data class SearchMatch(
    val pageNumber: Int,
    val snippet: String,
    val matchedTerm: String,
    val verticalRatio: Float = 0.3f
)

class PdfEngine {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var currentFile: File? = null

    private val mutex = Mutex()

    // High-capacity bitmap cache (64MB+ or 25% of app heap) to hold 15-20 pre-rendered pages simultaneously
    private val maxCacheSize = (Runtime.getRuntime().maxMemory() / 1024 / 4).toInt().coerceAtLeast(64 * 1024)
    private val bitmapCache = object : LruCache<String, Bitmap>(maxCacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    // In-memory text index for full-text search
    private val textIndexByPage = mutableMapOf<Int, String>()

    fun getCachedBitmap(pageIndex: Int): Bitmap? {
        val cacheKey = "${currentFile?.absolutePath}_$pageIndex"
        return bitmapCache.get(cacheKey)
    }

    fun isPageCached(pageIndex: Int): Boolean {
        val cacheKey = "${currentFile?.absolutePath}_$pageIndex"
        return bitmapCache.get(cacheKey) != null
    }

    suspend fun openFile(file: File): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            close()
            currentFile = file
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pfd = fileDescriptor ?: return@withContext 0
            val newRenderer = PdfRenderer(pfd)
            renderer = newRenderer

            // Populate text index if this is a sample book, or default textual hints
            buildSearchIndex(file, newRenderer.pageCount)

            return@withContext newRenderer.pageCount
        }
    }

    private fun buildSearchIndex(file: File, pageCount: Int) {
        textIndexByPage.clear()
        val sampleBook = SamplePdfGenerator.sampleBooks.find { it.filename == file.name }
        if (sampleBook != null) {
            sampleBook.pagesText.forEachIndexed { index, page ->
                val sb = StringBuilder()
                sb.append(page.chapterTitle).append("\n")
                sb.append(page.subtitle).append("\n")
                page.paragraphs.forEach { sb.append(it).append("\n") }
                if (page.quote != null) sb.append(page.quote).append("\n")
                textIndexByPage[index] = sb.toString()
            }
        } else {
            try {
                val extracted = PdfTextExtractor.extractTextPerPage(file, pageCount)
                extracted.forEach { (page, text) ->
                    if (text.isNotBlank()) {
                        textIndexByPage[page] = text
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun renderPage(pageIndex: Int, targetDensityDpi: Int = 320): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${currentFile?.absolutePath}_$pageIndex"
        bitmapCache.get(cacheKey)?.let { return@withContext it }

        mutex.withLock {
            bitmapCache.get(cacheKey)?.let { return@withContext it }
            val r = renderer ?: return@withContext null
            if (pageIndex < 0 || pageIndex >= r.pageCount) return@withContext null

            var page: PdfRenderer.Page? = null
            try {
                page = r.openPage(pageIndex)
                // High-fidelity rendering scale
                val baseWidth = page.width
                val baseHeight = page.height

                // Render at high crisp resolution (1.6x base scale, capped at 1200x1800) for sharp text without massive memory pressure
                val scale = 1.6f
                val renderWidth = (baseWidth * scale).toInt().coerceIn(720, 1200)
                val renderHeight = (baseHeight * scale).toInt().coerceIn(1000, 1800)

                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                // Initialize background to white to prevent transparent artifacting
                bitmap.eraseColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmapCache.put(cacheKey, bitmap)
                return@withContext bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            } finally {
                page?.close()
            }
        }
    }

    suspend fun renderThumbnail(pageIndex: Int, thumbWidth: Int = 180, thumbHeight: Int = 250): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${currentFile?.absolutePath}_thumb_$pageIndex"
        bitmapCache.get(cacheKey)?.let { return@withContext it }

        mutex.withLock {
            val r = renderer ?: return@withContext null
            if (pageIndex < 0 || pageIndex >= r.pageCount) return@withContext null

            var page: PdfRenderer.Page? = null
            try {
                page = r.openPage(pageIndex)
                val bitmap = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmapCache.put(cacheKey, bitmap)
                return@withContext bitmap
            } catch (e: Exception) {
                return@withContext null
            } finally {
                page?.close()
            }
        }
    }

    fun searchFullText(query: String): List<SearchMatch> {
        if (query.isBlank() || query.length < 2) return emptyList()
        val results = mutableListOf<SearchMatch>()
        val lowerQuery = query.trim().lowercase()

        textIndexByPage.forEach { (pageIdx, content) ->
            val lines = content.lines()
            lines.forEachIndexed { lineIdx, line ->
                if (line.lowercase().contains(lowerQuery)) {
                    val verticalRatio = (lineIdx.toFloat() / lines.size.coerceAtLeast(1).toFloat()).coerceIn(0.15f, 0.85f)
                    results.add(
                        SearchMatch(
                            pageNumber = pageIdx,
                            snippet = line.trim(),
                            matchedTerm = query.trim(),
                            verticalRatio = verticalRatio
                        )
                    )
                }
            }
        }
        return results
    }

    fun getPageText(pageIndex: Int, defaultTitle: String = "", totalPages: Int = 1): String {
        val indexed = textIndexByPage[pageIndex]
        if (!indexed.isNullOrBlank()) {
            return indexed
        }
        val title = defaultTitle.ifBlank { currentFile?.nameWithoutExtension ?: "Document" }
        return "$title. Page ${pageIndex + 1} of $totalPages."
    }

    fun close() {
        try {
            renderer?.close()
        } catch (_: Exception) {}
        renderer = null

        try {
            fileDescriptor?.close()
        } catch (_: Exception) {}
        fileDescriptor = null
        currentFile = null
    }
}

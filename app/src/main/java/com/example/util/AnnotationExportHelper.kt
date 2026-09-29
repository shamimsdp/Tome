package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.AnnotationEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AnnotationExportHelper {

    /**
     * Exports all user highlights and notes as a formatted text/markdown file (.txt).
     */
    fun exportAsTextFile(
        context: Context,
        bookTitle: String,
        author: String,
        annotations: List<AnnotationEntity>
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedTitle = bookTitle.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)
        val file = File(exportDir, "${sanitizedTitle}_Annotations.txt")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val exportDate = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.append("===================================================\n")
        sb.append("TOME READER - ANNOTATIONS & NOTES BACKUP\n")
        sb.append("===================================================\n\n")
        sb.append("Document: $bookTitle\n")
        if (author.isNotBlank()) sb.append("Author: $author\n")
        sb.append("Export Date: $exportDate\n")
        sb.append("Total Annotations: ${annotations.size}\n")
        sb.append("---------------------------------------------------\n\n")

        val sorted = annotations.sortedBy { it.pageNumber }
        var currentPage = -1

        sorted.forEach { ann ->
            if (ann.pageNumber != currentPage) {
                currentPage = ann.pageNumber
                sb.append("\n[ PAGE ${currentPage + 1} ]\n")
                sb.append("---------------------------------------------------\n")
            }

            sb.append("• [${ann.tag.uppercase()}] (${ann.colorHex})\n")
            if (ann.selectedText.isNotBlank()) {
                sb.append("  Quote: \"${ann.selectedText}\"\n")
            }
            if (ann.note.isNotBlank()) {
                sb.append("  Note:  ${ann.note}\n")
            }
            sb.append("  Created: ${dateFormat.format(Date(ann.createdAt))}\n\n")
        }

        file.writeText(sb.toString())
        return file
    }

    /**
     * Exports all user highlights and notes as a formatted PDF summary document (.pdf).
     */
    fun exportAsPdfDocument(
        context: Context,
        bookTitle: String,
        author: String,
        annotations: List<AnnotationEntity>
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedTitle = bookTitle.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)
        val file = File(exportDir, "${sanitizedTitle}_Annotations.pdf")

        val pdfDoc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNum = 1

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val exportDate = dateFormat.format(Date())

        // Paints
        val titlePaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 11f
            isAntiAlias = true
        }
        val headerBarPaint = Paint().apply {
            color = Color.rgb(79, 70, 229) // Indigo brand color
        }
        val pageHeaderPaint = Paint().apply {
            color = Color.rgb(79, 70, 229)
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val quotePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            isAntiAlias = true
        }
        val notePaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val cardBgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val tagBgPaint = Paint()

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var currentPage = pdfDoc.startPage(pageInfo)
        var canvas = currentPage.canvas

        fun drawHeader(c: Canvas) {
            c.drawRect(0f, 0f, pageWidth.toFloat(), 6f, headerBarPaint)
            c.drawText("TOME READER — ANNOTATIONS BACKUP", 40f, 35f, subtitlePaint)
            c.drawText(bookTitle.take(45), 40f, 60f, titlePaint)
            val sub = if (author.isNotBlank()) "By $author • Exported on $exportDate • ${annotations.size} Highlights" else "Exported on $exportDate • ${annotations.size} Highlights"
            c.drawText(sub, 40f, 78f, subtitlePaint)
            c.drawLine(40f, 92f, (pageWidth - 40).toFloat(), 92f, borderPaint)
        }

        drawHeader(canvas)
        var yPos = 115f

        val sorted = annotations.sortedBy { it.pageNumber }

        sorted.forEach { ann ->
            val neededHeight = 70f
            if (yPos + neededHeight > pageHeight - 50) {
                // Next page
                pdfDoc.finishPage(currentPage)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                currentPage = pdfDoc.startPage(pageInfo)
                canvas = currentPage.canvas
                drawHeader(canvas)
                yPos = 115f
            }

            // Draw Annotation Card
            val cardRect = RectF(40f, yPos, (pageWidth - 40).toFloat(), yPos + 62f)
            canvas.drawRoundRect(cardRect, 6f, 6f, cardBgPaint)
            canvas.drawRoundRect(cardRect, 6f, 6f, borderPaint)

            // Accent strip on card left
            val stripColor = try {
                Color.parseColor(ann.colorHex)
            } catch (_: Exception) {
                Color.rgb(250, 204, 21)
            }
            tagBgPaint.color = stripColor
            canvas.drawRoundRect(RectF(40f, yPos, 46f, yPos + 62f), 3f, 3f, tagBgPaint)

            // Page & Tag
            canvas.drawText("Page ${ann.pageNumber + 1}  •  [${ann.tag.uppercase()}]", 55f, yPos + 18f, pageHeaderPaint)

            // Quote snippet (1 line)
            val quoteText = if (ann.selectedText.isNotBlank()) "\"${ann.selectedText.take(85)}\"" else "Highlight on page ${ann.pageNumber + 1}"
            canvas.drawText(quoteText, 55f, yPos + 34f, quotePaint)

            // User Note if available
            if (ann.note.isNotBlank()) {
                canvas.drawText("✎ ${ann.note.take(80)}", 55f, yPos + 50f, notePaint)
            } else {
                canvas.drawText("No note attached", 55f, yPos + 50f, subtitlePaint)
            }

            yPos += 72f
        }

        // Footer page numbering
        canvas.drawText("Page $pageNum", (pageWidth / 2 - 20).toFloat(), (pageHeight - 20).toFloat(), subtitlePaint)

        pdfDoc.finishPage(currentPage)

        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        return file
    }

    /**
     * Launches standard Android Share Intent with FileProvider URI.
     */
    fun shareExportedFile(
        context: Context,
        file: File,
        mimeType: String,
        title: String
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Export Annotations")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

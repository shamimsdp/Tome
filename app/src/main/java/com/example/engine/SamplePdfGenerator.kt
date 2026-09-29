package com.example.engine

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

data class SampleBookInfo(
    val title: String,
    val author: String,
    val description: String,
    val filename: String,
    val pagesText: List<PageContent>
)

data class PageContent(
    val chapterTitle: String,
    val subtitle: String,
    val paragraphs: List<String>,
    val quote: String? = null
)

object SamplePdfGenerator {

    val sampleBooks = listOf(
        SampleBookInfo(
            title = "The Art of Reading & Focus",
            author = "Mortimer J. Adler & Charles Van Doren",
            description = "A classic guide to intelligent reading, active contemplation, and marking books.",
            filename = "art_of_reading.pdf",
            pagesText = listOf(
                PageContent(
                    chapterTitle = "Chapter I: The Dimensions of Reading",
                    subtitle = "Active vs. Passive Reading",
                    paragraphs = listOf(
                        "Reading is a multi-layered activity. To read for information is one thing; to read for understanding is quite another. When we read for understanding, our mind is stretched, challenged, and elevated.",
                        "The active reader is never a passive sponge. He questions the author, challenges premises, underlines vital passages, and scribbles notes in the margins. A book that is read actively is a book that has been conquered.",
                        "Mortimer Adler once wrote: 'You have not really read a book until you have written between its lines.' Marking a book is not an act of mutilation, but of love and respect."
                    ),
                    quote = "To read for understanding is to be stretched beyond one's present capacity."
                ),
                PageContent(
                    chapterTitle = "Chapter II: The Four Levels of Reading",
                    subtitle = "From Elementary to Syntopical",
                    paragraphs = listOf(
                        "There are four distinct levels of reading, each building upon the previous one:",
                        "1. Elementary Reading: The basic literacy required to decipher words and sentences on the printed page.",
                        "2. Inspectional Reading: The art of skimming systematically to grasp the architecture of the work within a limited timeframe.",
                        "3. Analytical Reading: Thorough, unrestricted reading that seeks to answer what the book is about, what is being said in detail, and whether it is true.",
                        "4. Syntopical Reading: Reading many books on the same subject to synthesize multiple perspectives."
                    ),
                    quote = "Inspectional reading teaches you what the book has to give before you decide to invest your soul."
                ),
                PageContent(
                    chapterTitle = "Chapter III: How to Make a Book Your Own",
                    subtitle = "The Value of Annotation and Highlighting",
                    paragraphs = listOf(
                        "Why should you write in a book? First, it keeps you awake—not merely conscious, but actively engaged. Second, reading, if it is active, is thinking, and thinking tends to express itself in words.",
                        "Highlighting is your personal trail of breadcrumbs. When you return to a book years later, your highlights reveal not only the book's core arguments, but who you were when you first encountered them.",
                        "Organize your thoughts with colors: gold for principal theses, green for actionable insights, and blue for memorable quotes."
                    ),
                    quote = "Marking a book keeps you awake and enters you into conversation with the author."
                ),
                PageContent(
                    chapterTitle = "Chapter IV: The Habit of Deep Focus",
                    subtitle = "Overcoming Distraction in the Digital Age",
                    paragraphs = listOf(
                        "In an era characterized by fragmented attention and notification bombardment, sustained reading is an act of rebellion. Deep reading engages the brain in contemplative empathy and critical reasoning.",
                        "When reading on a digital device, choose distraction-free environments. Dim ambient lights, engage warm sepia paper tones, and allow your thoughts to slow down to the cadence of the author's voice.",
                        "Reading is communion across centuries. Great books are conversations between minds separated by time and space."
                    ),
                    quote = "Deep reading is an oasis of silence in a world consumed by noise."
                )
            )
        ),
        SampleBookInfo(
            title = "Pride and Prejudice",
            author = "Jane Austen",
            description = "The timeless romantic masterpiece exploring manners, upbringing, and matrimonial expectations.",
            filename = "pride_and_prejudice.pdf",
            pagesText = listOf(
                PageContent(
                    chapterTitle = "Chapter 1: A Truth Universally Acknowledged",
                    subtitle = "Longbourn Estate",
                    paragraphs = listOf(
                        "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.",
                        "However little known the feelings or views of such a man may be on his first entering a neighbourhood, this truth is so well fixed in the minds of the surrounding families, that he is considered the rightful property of some one or other of their daughters.",
                        "'My dear Mr. Bennet,' said his lady to him one day, 'have you heard that Netherfield Park is let at last?' Mr. Bennet replied that he had not."
                    ),
                    quote = "A single man in possession of a good fortune, must be in want of a wife."
                ),
                PageContent(
                    chapterTitle = "Chapter 2: The Assembly at Meryton",
                    subtitle = "First Impressions and Pride",
                    paragraphs = listOf(
                        "Mr. Bingley was good-looking and gentlemanlike; he had a pleasant countenance, and easy, unaffected manners. His sisters were fine women, with an air of decided fashion.",
                        "His friend Mr. Darcy soon drew the attention of the room by his fine, tall person, handsome features, noble mien, and the report which was in general circulation within five minutes after his entrance, of his having ten thousand a year.",
                        "He was looked at with great admiration for about half the evening, till his manners gave a disgust which turned the tide of his popularity; for he was discovered to be proud, to be above his company, and above being pleased."
                    ),
                    quote = "He was discovered to be proud, to be above his company, and above being pleased."
                ),
                PageContent(
                    chapterTitle = "Chapter 3: Elizabeth's Wit",
                    subtitle = "A Lively Mind",
                    paragraphs = listOf(
                        "Elizabeth Bennet had lively, playful manners, which delighted in anything ridiculous. She could not accept the stiff formality of aristocratic society without finding occasion for miret.",
                        "Darcy had at first scarcely allowed her to be pretty; he had looked at her without admiration at the ball; and when they next met, he looked at her only to criticise. But no sooner had he made it clear to himself and his friends that she hardly had a good feature in her face, than he began to find it was rendered uncommonly intelligent by the beautiful expression of her dark eyes."
                    ),
                    quote = "She was rendered uncommonly intelligent by the beautiful expression of her dark eyes."
                )
            )
        ),
        SampleBookInfo(
            title = "Modern Architecture & Kotlin Guide",
            author = "Software Engineering Institute",
            description = "Principles of robust mobile architecture, reactive state, and clean domain boundaries.",
            filename = "kotlin_architecture.pdf",
            pagesText = listOf(
                PageContent(
                    chapterTitle = "Section I: Unidirectional Data Flow",
                    subtitle = "State Holders & Immutability",
                    paragraphs = listOf(
                        "In modern reactive mobile development, State flows downward while Events flow upward. This fundamental tenet ensures predictable state transitions and eliminates race conditions.",
                        "A ViewModel exposes a single, immutable StateFlow that reflects the entire state of the screen. User interactions, such as tapping a page or saving an annotation, emit events that the ViewModel processes to produce new state.",
                        "By isolating state in pure data classes, UI testing becomes deterministic and straightforward."
                    ),
                    quote = "State flows down, events flow up: the cornerstone of predictable user interfaces."
                ),
                PageContent(
                    chapterTitle = "Section II: Offline-First Data Caching",
                    subtitle = "Local Room Database as Single Source of Truth",
                    paragraphs = listOf(
                        "In an offline-first architecture, the local database (Room) serves as the single source of truth for the UI. Network or cloud sync operations update the database, and the UI reacts seamlessly.",
                        "Users can highlight passages, attach annotations, and flip through pages deep in an airplane flight without network connectivity. When connectivity is restored, sync queues synchronize pending records."
                    ),
                    quote = "Offline is not an error state; it is a first-class user scenario."
                ),
                PageContent(
                    chapterTitle = "Section III: Hardware-Accelerated PDF Rendering",
                    subtitle = "Native Graphics and Texture Management",
                    paragraphs = listOf(
                        "Rendering vector document formats requires precision. Android's native PdfRenderer leverages hardware acceleration to rasterize vector glyphs onto crisp Bitmaps.",
                        "To maintain silky-smooth 60fps page turns, bitmap recycling and memory caches are utilized. The page curl effect simulates realistic physical book physics through gradient shadows and perspective matrices."
                    ),
                    quote = "Performance is the invisible canvas upon which delightful design is painted."
                )
            )
        )
    )

    fun ensureSampleBooksExist(context: Context): List<File> {
        val createdFiles = mutableListOf<File>()
        for (book in sampleBooks) {
            val file = File(context.filesDir, book.filename)
            if (!file.exists() || file.length() == 0L) {
                generatePdfFile(file, book)
            }
            createdFiles.add(file)
        }
        return createdFiles
    }

    fun generatePdfFile(outputFile: File, book: SampleBookInfo) {
        val document = PdfDocument()

        val pageWidth = 595 // A4 standard pt width
        val pageHeight = 842 // A4 standard pt height

        val titlePaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 20f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 11f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val quotePaint = Paint().apply {
            color = Color.rgb(180, 83, 9)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            isAntiAlias = true
        }

        val rulePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
            isAntiAlias = true
        }

        val pageNumberPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        book.pagesText.forEachIndexed { pageIndex, pageContent ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Background paper color
            canvas.drawColor(Color.rgb(253, 252, 248))

            // Header banner line
            canvas.drawLine(50f, 60f, (pageWidth - 50).toFloat(), 60f, rulePaint)
            canvas.drawText("${book.title} — ${book.author}", 50f, 52f, pageNumberPaint)

            var y = 100f

            // Chapter Title
            canvas.drawText(pageContent.chapterTitle, 50f, y, titlePaint)
            y += 24f

            // Subtitle
            canvas.drawText(pageContent.subtitle, 50f, y, subtitlePaint)
            y += 24f
            canvas.drawLine(50f, y, (pageWidth - 50).toFloat(), y, rulePaint)
            y += 30f

            // Paragraphs
            for (paragraph in pageContent.paragraphs) {
                val words = paragraph.split(" ")
                var currentLine = StringBuilder()
                val maxWidth = pageWidth - 100f

                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    val measure = bodyPaint.measureText(testLine)
                    if (measure > maxWidth) {
                        canvas.drawText(currentLine.toString(), 50f, y, bodyPaint)
                        y += 18f
                        currentLine = StringBuilder(word)
                    } else {
                        currentLine = StringBuilder(testLine)
                    }
                }
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine.toString(), 50f, y, bodyPaint)
                    y += 18f
                }
                y += 14f // paragraph spacing
            }

            // Quote callout box
            if (pageContent.quote != null) {
                y += 10f
                val quoteBgPaint = Paint().apply {
                    color = Color.rgb(254, 243, 199)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(50f, y, (pageWidth - 50).toFloat(), y + 42f, quoteBgPaint)
                val quoteBarPaint = Paint().apply {
                    color = Color.rgb(217, 119, 6)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(50f, y, 54f, y + 42f, quoteBarPaint)
                canvas.drawText("“${pageContent.quote}”", 65f, y + 26f, quotePaint)
            }

            // Footer
            val footerY = pageHeight - 45f
            canvas.drawLine(50f, footerY - 15f, (pageWidth - 50).toFloat(), footerY - 15f, rulePaint)
            canvas.drawText("Page ${pageIndex + 1} of ${book.pagesText.size}", 50f, footerY, pageNumberPaint)
            canvas.drawText("PageCraft Book Reader", (pageWidth - 160).toFloat(), footerY, pageNumberPaint)

            document.finishPage(page)
        }

        try {
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
        } finally {
            document.close()
        }
    }
}

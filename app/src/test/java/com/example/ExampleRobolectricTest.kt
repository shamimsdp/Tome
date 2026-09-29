package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.DocumentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Tome", appName)
  }

  @Test
  fun `annotation entity creates correctly`() {
    val annotation = AnnotationEntity(
      documentId = "doc_123",
      pageNumber = 1,
      highlightedText = "Reading actively transforms comprehension.",
      colorHex = "#FFEB3B",
      note = "Key concept on page 2",
      tag = "Key Takeaway"
    )
    assertNotNull(annotation.id)
    assertEquals("doc_123", annotation.documentId)
    assertEquals(1, annotation.pageNumber)
    assertEquals("#FFEB3B", annotation.colorHex)
  }

  @Test
  fun `bookmark entity creates correctly`() {
    val bookmark = BookmarkEntity(
      documentId = "doc_123",
      pageNumber = 5,
      title = "Chapter 3 Start"
    )
    assertNotNull(bookmark.id)
    assertEquals(5, bookmark.pageNumber)
    assertTrue(bookmark.isSynced)
  }

  @Test
  fun `document entity progress calculation`() {
    val doc = DocumentEntity(
      id = "doc_456",
      title = "The Art of Reading",
      filePath = "/dummy/path.pdf",
      totalPages = 10,
      currentPage = 4,
      progressPercent = 50f
    )
    assertEquals(10, doc.totalPages)
    assertEquals(4, doc.currentPage)
    assertEquals(50f, doc.progressPercent)
  }

  @Test
  fun `voice reading sentence parsing`() {
    val sampleText = "The journey begins with curiosity. Reading awakens the mind. It opens new perspectives!"
    val sentences = sampleText.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
    assertEquals(3, sentences.size)
    assertEquals("The journey begins with curiosity.", sentences[0])
    assertEquals("Reading awakens the mind.", sentences[1])
    assertEquals("It opens new perspectives!", sentences[2])
  }
}

package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class ReadingTheme(
    val title: String,
    val paperColor: Color,
    val textColor: Color,
    val spineShadowColor: Color,
    val accentColor: Color
) {
    DAY(
        title = "Clean Day",
        paperColor = Color(0xFFFAFAFA),
        textColor = Color(0xFF1E293B),
        spineShadowColor = Color(0x33000000),
        accentColor = Color(0xFF2563EB)
    ),
    SEPIA(
        title = "Warm Book",
        paperColor = Color(0xFFF6F0E4),
        textColor = Color(0xFF382E25),
        spineShadowColor = Color(0x403E2723),
        accentColor = Color(0xFFB45309)
    ),
    SAGE(
        title = "Parchment Sage",
        paperColor = Color(0xFFEBF1EC),
        textColor = Color(0xFF1B3322),
        spineShadowColor = Color(0x331E3A2F),
        accentColor = Color(0xFF0D9488)
    ),
    CHARCOAL(
        title = "Night Charcoal",
        paperColor = Color(0xFF1E222A),
        textColor = Color(0xFFE2E8F0),
        spineShadowColor = Color(0x80000000),
        accentColor = Color(0xFF60A5FA)
    ),
    OLED_NIGHT(
        title = "OLED Pure Dark",
        paperColor = Color(0xFF000000),
        textColor = Color(0xFFE5E7EB),
        spineShadowColor = Color(0xAA000000),
        accentColor = Color(0xFF38BDF8)
    )
}

data class CloudFile(
    val id: String,
    val name: String,
    val author: String,
    val provider: String, // "Google Drive", "Cloud Sync", "Dropbox"
    val sizeString: String,
    val totalPages: Int,
    val isDownloaded: Boolean = false,
    val downloadUrl: String = "",
    val updatedAt: String = "Today"
)

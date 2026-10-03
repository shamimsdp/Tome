package com.example.data.model

/**
 * System-wide theme mode setting for the PDF Reader.
 * Supports:
 * - SYSTEM: Automatically follows Android device system dark/light theme setting
 * - LIGHT: Forces light editorial theme with crisp paper backgrounds
 * - DARK: Forces dark theme with high-contrast low-light readability to prevent eye fatigue
 */
enum class ThemeMode(
    val label: String,
    val description: String
) {
    SYSTEM(
        label = "System Default",
        description = "Matches device dark/light theme setting"
    ),
    LIGHT(
        label = "Light Mode",
        description = "Crisp paper and bright contrast"
    ),
    DARK(
        label = "Dark Theme",
        description = "Low-light night mode for comfortable reading"
    )
}

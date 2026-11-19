package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppThemeColor(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val onPrimaryColor: Color = Color.White
) {
    GREY("grey", "Classic Grey", Color(0xFF5F6368)),
    SLATE("slate", "Deep Slate", Color(0xFF0F172A)),
    INDIGO("indigo", "Electric Indigo", Color(0xFF4F46E5)),
    VIOLET("violet", "Royal Purple", Color(0xFF7C3AED)),
    AMBER("amber", "Warm Amber", Color(0xFFD97706)),
    CYAN("cyan", "Ocean Cyan", Color(0xFF0891B2));

    companion object {
        val DEFAULT = GREY

        fun fromId(id: String?): AppThemeColor {
            if (id.isNullOrBlank()) return DEFAULT
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}

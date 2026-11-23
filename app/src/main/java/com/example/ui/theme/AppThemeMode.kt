package com.example.ui.theme

enum class AppThemeMode(
    val id: String,
    val displayName: String,
    val description: String
) {
    LIGHT("light", "Light", "Bright clean daylight look"),
    DARK("dark", "Dark", "Deep slate dark appearance"),
    BLACK("black", "Black", "Pure AMOLED true black theme"),
    SYSTEM("system", "System Default", "Follow device appearance");

    companion object {
        val DEFAULT = SYSTEM

        fun fromId(id: String?): AppThemeMode {
            if (id.isNullOrBlank()) return DEFAULT
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}

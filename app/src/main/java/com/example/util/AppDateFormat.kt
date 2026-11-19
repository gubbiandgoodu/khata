package com.example.util

enum class AppDateFormat(
    val pattern: String,
    val sample: String,
    val label: String
) {
    DD_MMM_YYYY("dd MMM yyyy", "29 Sep 2026", "29 Sep 2026 (Default)"),
    DD_MM_YYYY("dd/MM/yyyy", "29/09/2026", "29/09/2026 (DD/MM/YYYY)"),
    YYYY_MM_DD("yyyy-MM-dd", "2026-09-29", "2026-09-29 (YYYY-MM-DD)"),
    MM_DD_YYYY("MM/dd/yyyy", "09/29/2026", "09/29/2026 (MM/DD/YYYY)"),
    DD_DASH_MM_YYYY("dd-MM-yyyy", "29-09-2026", "29-09-2026 (DD-MM-YYYY)");

    companion object {
        val DEFAULT = DD_MMM_YYYY

        fun fromPattern(pattern: String?): AppDateFormat {
            if (pattern.isNullOrBlank()) return DEFAULT
            return entries.firstOrNull { it.pattern == pattern } ?: DEFAULT
        }
    }
}

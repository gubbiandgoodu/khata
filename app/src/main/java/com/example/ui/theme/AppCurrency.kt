package com.example.ui.theme

import java.text.NumberFormat
import java.util.Locale

enum class AppCurrency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val locale: Locale
) {
    INR("INR", "₹", "Indian Rupee (₹)", Locale("en", "IN")),
    USD("USD", "$", "US Dollar ($)", Locale.US),
    EUR("EUR", "€", "Euro (€)", Locale.GERMANY),
    GBP("GBP", "£", "British Pound (£)", Locale.UK),
    JPY("JPY", "¥", "Japanese Yen (¥)", Locale.JAPAN),
    AED("AED", "AED", "UAE Dirham (AED)", Locale("ar", "AE")),
    CAD("CAD", "C$", "Canadian Dollar (C$)", Locale.CANADA),
    AUD("AUD", "A$", "Australian Dollar (A$)", Locale("en", "AU")),
    SGD("SGD", "S$", "Singapore Dollar (S$)", Locale("en", "SG"));

    fun format(amount: Double): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(locale).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
            }
            format.format(amount)
        } catch (_: Exception) {
            "$symbol%.2f".format(amount)
        }
    }

    companion object {
        val DEFAULT = INR

        fun fromCode(code: String?): AppCurrency {
            if (code.isNullOrBlank()) return DEFAULT
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DEFAULT
        }
    }
}

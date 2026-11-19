package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.data.model.Expense
import com.example.ui.viewmodel.StatsTimeRange

object DateUtils {

    private const val ISO_DATE_FORMAT = "yyyy-MM-dd"
    private const val DISPLAY_DATE_FORMAT = "dd MMM yyyy"
    private const val FULL_DAY_FORMAT = "EEE, dd MMM yyyy"

    private fun getIsoFormat(): SimpleDateFormat {
        val sdf = SimpleDateFormat(ISO_DATE_FORMAT, Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf
    }

    private fun getDisplayFormat(): SimpleDateFormat {
        val sdf = SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf
    }

    private fun getFullDayFormat(): SimpleDateFormat {
        val sdf = SimpleDateFormat(FULL_DAY_FORMAT, Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf
    }

    /**
     * Returns today's date in "yyyy-MM-dd" format.
     */
    fun getTodayDateString(): String {
        return getIsoFormat().format(Date())
    }

    /**
     * Returns today's date formatted as "yyyyMMdd" (e.g. 20261005) for date tagging backup files.
     */
    fun getBackupDateTag(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date())
    }

    /**
     * Returns yesterday's date in "yyyy-MM-dd" format.
     */
    fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return getIsoFormat().format(cal.time)
    }

    /**
     * Normalize calendar to start of day (00:00:00.000) so no time of day is included.
     */
    fun normalizeToStartOfDay(year: Int, monthZeroIndexed: Int, dayOfMonth: Int): Pair<String, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dateString = getIsoFormat().format(cal.time)
        val millis = cal.timeInMillis
        return Pair(dateString, millis)
    }

    /**
     * Converts a "yyyy-MM-dd" string to normalized start-of-day millis.
     */
    fun dateStringToStartOfDayMillis(dateStr: String): Long {
        return try {
            val date = getIsoFormat().parse(dateStr) ?: Date()
            val cal = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    /**
     * Friendly display for headers or card:
     * - "Today, 29 Sep 2026"
     * - "Yesterday, 28 Sep 2026"
     * - "Tue, 29 Sep 2026"
     */
    fun formatDisplayDate(dateStr: String, appDateFormat: AppDateFormat = AppDateFormat.DEFAULT): String {
        val todayStr = getTodayDateString()
        val yesterdayStr = getYesterdayDateString()

        val parsedDate = try {
            getIsoFormat().parse(dateStr)
        } catch (_: Exception) {
            null
        } ?: return dateStr

        val customFormatter = SimpleDateFormat(appDateFormat.pattern, Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        val displayFormatted = customFormatter.format(parsedDate)

        return when (dateStr) {
            todayStr -> "Today, $displayFormatted"
            yesterdayStr -> "Yesterday, $displayFormatted"
            else -> {
                val dayOfWeek = SimpleDateFormat("EEE, ", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }.format(parsedDate)
                "$dayOfWeek$displayFormatted"
            }
        }
    }

    /**
     * Short display according to user's selected date format.
     */
    fun formatShortDate(dateStr: String, appDateFormat: AppDateFormat = AppDateFormat.DEFAULT): String {
        return try {
            val date = getIsoFormat().parse(dateStr) ?: return dateStr
            val customFormatter = SimpleDateFormat(appDateFormat.pattern, Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            customFormatter.format(date)
        } catch (_: Exception) {
            dateStr
        }
    }

    /**
     * Returns components [year, monthZeroIndexed, dayOfMonth] for pre-populating DatePicker.
     */
    fun parseDateComponents(dateStr: String): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance()
        try {
            val date = getIsoFormat().parse(dateStr)
            if (date != null) cal.time = date
        } catch (_: Exception) {
            // Keep current calendar
        }
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Extracts the day of the month as a number string (e.g. "29", "5").
     */
    fun getDayOfMonth(dateStr: String): String {
        val parts = dateStr.split("-")
        return if (parts.size >= 3) {
            val dayInt = parts[2].toIntOrNull()
            dayInt?.toString() ?: parts[2]
        } else {
            ""
        }
    }

    /**
     * Extracts the short uppercase month (e.g. "SEP", "OCT").
     */
    fun getShortMonth(dateStr: String): String {
        val parts = dateStr.split("-")
        return if (parts.size >= 2) {
            when (parts[1].toIntOrNull() ?: 1) {
                1 -> "JAN"
                2 -> "FEB"
                3 -> "MAR"
                4 -> "APR"
                5 -> "MAY"
                6 -> "JUN"
                7 -> "JUL"
                8 -> "AUG"
                9 -> "SEP"
                10 -> "OCT"
                11 -> "NOV"
                12 -> "DEC"
                else -> ""
            }
        } else {
            ""
        }
    }

    /**
     * Filters a list of expenses strictly according to the StatsTimeRange.
     */
    fun filterExpensesByTimeRange(expenses: List<Expense>, timeRange: StatsTimeRange): List<Expense> {
        return when (timeRange) {
            StatsTimeRange.ALL_TIME -> expenses
            StatsTimeRange.THIS_MONTH -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfMonth = cal.timeInMillis
                expenses.filter { it.dateMillis >= startOfMonth }
            }
            StatsTimeRange.THIS_WEEK -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfWeek = cal.timeInMillis
                expenses.filter { it.dateMillis >= startOfWeek }
            }
        }
    }
}

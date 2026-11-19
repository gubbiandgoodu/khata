package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMode
import com.example.ui.theme.AppThemeColor
import com.example.util.BackupUtils
import com.example.util.DateUtils
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
        assertEquals("Khata", appName)
    }

    @Test
    fun `date utils generates strictly date only format`() {
        val todayStr = DateUtils.getTodayDateString()
        assertEquals(10, todayStr.length)
        assertTrue(todayStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))

        val (year, month, day) = DateUtils.parseDateComponents(todayStr)
        assertTrue(year >= 2024)
        assertTrue(month in 0..11)
        assertTrue(day in 1..31)

        val (normDate, normMillis) = DateUtils.normalizeToStartOfDay(year, month, day)
        assertEquals(todayStr, normDate)
        assertTrue(normMillis > 0)
    }

    @Test
    fun `payment mode values`() {
        assertEquals("Cash", PaymentMode.CASH.label)
        assertEquals("UPI", PaymentMode.UPI.label)
        assertEquals(PaymentMode.CASH, PaymentMode.valueOf("CASH"))
        assertEquals(PaymentMode.UPI, PaymentMode.valueOf("UPI"))
    }

    @Test
    fun `expense categories are populated`() {
        assertTrue(ExpenseCategory.entries.isNotEmpty())
        assertEquals(ExpenseCategory.FOOD, ExpenseCategory.fromId("food"))
        assertEquals(ExpenseCategory.GROCERIES, ExpenseCategory.fromId("groceries"))
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.fromId("unknown_category"))
    }

    @Test
    fun `default theme color is grey and red or green are removed`() {
        assertEquals(AppThemeColor.GREY, AppThemeColor.DEFAULT)
        assertEquals(AppThemeColor.GREY, AppThemeColor.fromId("grey"))
        assertEquals(AppThemeColor.INDIGO, AppThemeColor.fromId("indigo"))
        assertEquals(AppThemeColor.GREY, AppThemeColor.fromId("red"))
        assertEquals(AppThemeColor.GREY, AppThemeColor.fromId("emerald"))
        assertEquals(AppThemeColor.GREY, AppThemeColor.fromId("unknown"))
    }

    @Test
    fun `backup and restore json serialization`() {
        val testExpenses = listOf(
            Expense(
                id = 1,
                amount = 250.0,
                dateString = "2026-09-29",
                dateMillis = 1727568000000L,
                paymentMode = PaymentMode.UPI,
                category = "food",
                note = "Team Lunch"
            ),
            Expense(
                id = 2,
                amount = 120.0,
                dateString = "2026-09-29",
                dateMillis = 1727568000000L,
                paymentMode = PaymentMode.CASH,
                category = "travel",
                note = "Auto fare"
            )
        )

        val json = BackupUtils.createBackupJson(testExpenses)
        assertTrue(json.contains("Khata"))
        assertTrue(json.contains("Team Lunch"))
        assertTrue(json.contains("Auto fare"))

        val parseResult = BackupUtils.parseBackupJson(json)
        assertTrue(parseResult.isSuccess)
        val restored = parseResult.getOrThrow()
        assertEquals(2, restored.size)
        assertEquals(250.0, restored[0].amount, 0.001)
        assertEquals(PaymentMode.UPI, restored[0].paymentMode)
        assertEquals("Team Lunch", restored[0].note)
        assertEquals(false, restored[0].isIncome)
        assertEquals(120.0, restored[1].amount, 0.001)
        assertEquals(PaymentMode.CASH, restored[1].paymentMode)
        assertEquals(false, restored[1].isIncome)
    }

    @Test
    fun `income transactions serialize and resolve categories properly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val categories = com.example.data.model.CategoryRegistry.getAllCategories(context)

        val salaryCat = com.example.data.model.CategoryRegistry.resolveCategory("inc_salary", categories)
        assertEquals("Salary", salaryCat.title)

        val entries = listOf(
            Expense(
                id = 10,
                amount = 50000.0,
                dateString = "2026-11-19",
                dateMillis = 1795046400000L,
                paymentMode = PaymentMode.UPI,
                category = "inc_salary",
                note = "Monthly Salary",
                isIncome = true
            ),
            Expense(
                id = 11,
                amount = 350.0,
                dateString = "2026-11-19",
                dateMillis = 1795046400000L,
                paymentMode = PaymentMode.CASH,
                category = "food",
                note = "Lunch",
                isIncome = false
            )
        )

        val json = BackupUtils.createBackupJson(entries)
        val parsed = BackupUtils.parseBackupJson(json).getOrThrow()
        assertEquals(2, parsed.size)
        assertEquals(true, parsed[0].isIncome)
        assertEquals("inc_salary", parsed[0].category)
        assertEquals(false, parsed[1].isIncome)
    }

    @Test
    fun `backup frequency values`() {
        assertEquals("Daily", com.example.ui.viewmodel.BackupFrequency.DAILY.title)
        assertEquals("Monthly", com.example.ui.viewmodel.BackupFrequency.MONTHLY.title)
        assertEquals("Manual", com.example.ui.viewmodel.BackupFrequency.MANUAL.title)
        assertEquals(3, com.example.ui.viewmodel.BackupFrequency.entries.size)
    }

    @Test
    fun `date components for transaction row`() {
        val testDate = "2026-09-29"
        assertEquals("29", DateUtils.getDayOfMonth(testDate))
        assertEquals("SEP", DateUtils.getShortMonth(testDate))

        val singleDigitDate = "2026-01-05"
        assertEquals("5", DateUtils.getDayOfMonth(singleDigitDate))
        assertEquals("JAN", DateUtils.getShortMonth(singleDigitDate))
    }

    @Test
    fun `category management adds and deletes categories`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initialCategories = com.example.data.model.CategoryRegistry.getAllCategories(context)
        val initialCount = initialCategories.size

        val newCat = com.example.data.model.CategoryRegistry.addCategory(
            context,
            "Gym & Fitness",
            0xFF10B981,
            "fitness_center"
        )
        assertEquals("Gym & Fitness", newCat.title)
        assertTrue(newCat.isCustom)

        val updated = com.example.data.model.CategoryRegistry.getAllCategories(context)
        assertEquals(initialCount + 1, updated.size)

        com.example.data.model.CategoryRegistry.deleteCategory(context, newCat.id)
        val afterDelete = com.example.data.model.CategoryRegistry.getAllCategories(context)
        assertEquals(initialCount, afterDelete.size)
    }

    @Test
    fun `local backup helper state and daily check`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(com.example.util.LocalBackupHelper.getFolderName(context))
        assertTrue(com.example.util.LocalBackupHelper.isDailyBackupDueToday(context))
    }

    @Test
    fun `currency options and formatting`() {
        val inr = com.example.ui.theme.AppCurrency.INR
        assertEquals("₹", inr.symbol)
        assertEquals("INR", inr.code)
        assertTrue(inr.format(500.0).contains("₹") || inr.format(500.0).contains("500"))

        val usd = com.example.ui.theme.AppCurrency.USD
        assertEquals("$", usd.symbol)
        assertTrue(usd.format(25.5).contains("$"))

        assertEquals(com.example.ui.theme.AppCurrency.EUR, com.example.ui.theme.AppCurrency.fromCode("EUR"))
        assertEquals(com.example.ui.theme.AppCurrency.INR, com.example.ui.theme.AppCurrency.fromCode("unknown"))
    }

    @Test
    fun `date format options and display formatting`() {
        val testDate = "2026-09-29"
        val ddmmyyyy = com.example.util.AppDateFormat.DD_MM_YYYY
        assertEquals("29/09/2026", DateUtils.formatShortDate(testDate, ddmmyyyy))

        val yyyymmdd = com.example.util.AppDateFormat.YYYY_MM_DD
        assertEquals("2026-09-29", DateUtils.formatShortDate(testDate, yyyymmdd))

        val defaultFormat = com.example.util.AppDateFormat.DEFAULT
        assertEquals("29 Sep 2026", DateUtils.formatShortDate(testDate, defaultFormat))
    }

    @Test
    fun `payment mode registry adds and deletes modes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initialModes = com.example.data.model.PaymentModeRegistry.getAllModes(context)
        val initialCount = initialModes.size

        val added = com.example.data.model.PaymentModeRegistry.addMode(
            context = context,
            label = "Credit Card",
            iconName = "credit_card",
            colorHex = 0xFF3B82F6
        )
        assertEquals("Credit Card", added.label)
        assertTrue(added.isCustom)

        val updatedModes = com.example.data.model.PaymentModeRegistry.getAllModes(context)
        assertEquals(initialCount + 1, updatedModes.size)

        val deleted = com.example.data.model.PaymentModeRegistry.deleteMode(context, added.name)
        assertTrue(deleted)
        val finalModes = com.example.data.model.PaymentModeRegistry.getAllModes(context)
        assertEquals(initialCount, finalModes.size)
    }

    @Test
    fun `backup date tag format is strictly yyyymmdd`() {
        val tag = DateUtils.getBackupDateTag()
        assertEquals(8, tag.length)
        assertTrue(tag.all { it.isDigit() })
        // Starts with year (20xx)
        assertTrue(tag.startsWith("20"))
    }

    @Test
    fun `category resolution and time range filtering for statistics`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val categories = com.example.data.model.CategoryRegistry.getAllCategories(context)

        // Resolve by ID
        val foodById = com.example.data.model.CategoryRegistry.resolveCategory("food", categories)
        assertEquals("Food & Dining", foodById.title)

        // Resolve by Title
        val foodByTitle = com.example.data.model.CategoryRegistry.resolveCategory("Food & Dining", categories)
        assertEquals("food", foodByTitle.id)

        // Resolve custom category
        val customCat = com.example.data.model.CategoryItem("custom_1", "Gym", 0xFF10B981, "fitness_center", isCustom = true)
        val resolvedCustom = com.example.data.model.CategoryRegistry.resolveCategory("custom_1", categories + customCat)
        assertEquals("Gym", resolvedCustom.title)

        // Test time range filtering
        val testExpenses = listOf(
            Expense(id = 1, amount = 100.0, dateString = "2026-10-01", dateMillis = 1727740800000L, paymentMode = PaymentMode.UPI, category = "food", note = ""),
            Expense(id = 2, amount = 200.0, dateString = "2026-10-02", dateMillis = 1727827200000L, paymentMode = PaymentMode.CASH, category = "transport", note = "")
        )
        val allTime = DateUtils.filterExpensesByTimeRange(testExpenses, com.example.ui.viewmodel.StatsTimeRange.ALL_TIME)
        assertEquals(2, allTime.size)
    }
}

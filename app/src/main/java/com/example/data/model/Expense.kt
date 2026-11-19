package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single transaction record (expense or income) in Khata.
 * Strict constraint: Date only, no time.
 * Includes amount, payment mode (Cash/UPI), category, date, optional note, and income flag.
 */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val dateString: String, // Format "yyyy-MM-dd", guarantees strictly date only with no time component
    val dateMillis: Long,   // Epoch millis at start of day (00:00:00.000) for sorting
    val paymentMode: PaymentMode, // CASH or UPI
    val category: String = ExpenseCategory.OTHER.id,
    val note: String = "",
    val isIncome: Boolean = false
)

package com.example.util

import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMode
import org.json.JSONArray
import org.json.JSONObject

object BackupUtils {

    fun createBackupJson(expenses: List<Expense>): String {
        val root = JSONObject()
        root.put("appName", "Khata")
        root.put("version", 2)
        root.put("backupDate", DateUtils.getTodayDateString())
        root.put("timestamp", System.currentTimeMillis())
        root.put("totalExpenses", expenses.count { !it.isIncome })
        root.put("totalIncomeCount", expenses.count { it.isIncome })
        root.put("totalAmount", expenses.filter { !it.isIncome }.sumOf { it.amount })
        root.put("totalIncome", expenses.filter { it.isIncome }.sumOf { it.amount })

        val array = JSONArray()
        for (expense in expenses) {
            val obj = JSONObject()
            obj.put("id", expense.id)
            obj.put("amount", expense.amount)
            obj.put("dateString", expense.dateString)
            obj.put("dateMillis", expense.dateMillis)
            obj.put("paymentMode", expense.paymentMode.name)
            obj.put("category", expense.category)
            obj.put("note", expense.note)
            obj.put("isIncome", expense.isIncome)
            array.put(obj)
        }
        root.put("expenses", array)
        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): Result<List<Expense>> {
        return try {
            val root = JSONObject(jsonString)
            val array = root.optJSONArray("expenses") ?: return Result.failure(IllegalArgumentException("No 'expenses' found in backup file"))
            val list = mutableListOf<Expense>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val amount = obj.optDouble("amount", 0.0)
                if (amount <= 0) continue

                val dateString = obj.optString("dateString", DateUtils.getTodayDateString())
                val dateMillis = if (obj.has("dateMillis")) {
                    obj.getLong("dateMillis")
                } else {
                    DateUtils.dateStringToStartOfDayMillis(dateString)
                }

                val modeStr = obj.optString("paymentMode", PaymentMode.CASH.name)
                val paymentMode = try {
                    PaymentMode.valueOf(modeStr.uppercase())
                } catch (_: Exception) {
                    PaymentMode.CASH
                }

                val category = obj.optString("category", ExpenseCategory.OTHER.id)
                val note = obj.optString("note", "")
                val isIncome = obj.optBoolean("isIncome", false)

                list.add(
                    Expense(
                        amount = amount,
                        dateString = dateString,
                        dateMillis = dateMillis,
                        paymentMode = paymentMode,
                        category = category,
                        note = note,
                        isIncome = isIncome
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

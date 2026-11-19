package com.example.data.repository

import com.example.data.local.ExpenseDao
import com.example.data.model.Expense
import com.example.data.model.PaymentMode
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesByMode(mode: PaymentMode): Flow<List<Expense>> =
        expenseDao.getExpensesByMode(mode)

    suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

    suspend fun insert(expense: Expense): Long = expenseDao.insertExpense(expense)

    suspend fun insertAll(expenses: List<Expense>) = expenseDao.insertExpenses(expenses)

    suspend fun update(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun delete(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun deleteById(id: Long) = expenseDao.deleteExpenseById(id)

    suspend fun deleteAll() = expenseDao.deleteAllExpenses()
}

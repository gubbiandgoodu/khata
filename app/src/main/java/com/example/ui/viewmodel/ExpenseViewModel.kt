package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CategoryItem
import com.example.data.model.CategoryRegistry
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMode
import com.example.data.model.PaymentModeRegistry
import com.example.data.repository.ExpenseRepository
import com.example.ui.theme.AppCurrency
import com.example.ui.theme.AppThemeColor
import com.example.ui.theme.AppThemeMode
import com.example.util.AppDateFormat
import com.example.util.BackupUtils
import com.example.util.DateUtils
import com.example.util.LocalBackupHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ExpenseFilter {
    ALL,
    UPI,
    CASH
}

enum class TransactionTypeFilter(val label: String) {
    ALL("All"),
    EXPENSE("Expenses"),
    INCOME("Income")
}

enum class StatsTimeRange(val label: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    THIS_WEEK("This Week")
}

enum class BackupFrequency(val id: String, val title: String, val subtitle: String) {
    DAILY("daily", "Daily", "Automatically backup once every day"),
    MONTHLY("monthly", "Monthly", "Automatically backup once every month"),
    MANUAL("manual", "Manual", "Only backup when you tap backup")
}

data class CategoryStat(
    val category: ExpenseCategory,
    val totalAmount: Double,
    val count: Int,
    val percentage: Float
)

data class StatisticsData(
    val timeRange: StatsTimeRange = StatsTimeRange.ALL_TIME,
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netBalance: Double = 0.0,
    val incomeCount: Int = 0,
    val cashTotal: Double = 0.0,
    val upiTotal: Double = 0.0,
    val cashPercentage: Float = 0f,
    val upiPercentage: Float = 0f,
    val transactionCount: Int = 0,
    val categoryStats: List<CategoryStat> = emptyList(),
    val topCategory: ExpenseCategory? = null,
    val timeRangeExpenses: List<Expense> = emptyList()
)

private data class FilterParams(
    val filter: ExpenseFilter,
    val typeFilter: TransactionTypeFilter,
    val catFilter: String?,
    val modeFilter: String?,
    val query: String
)

private data class SheetStateParams(
    val isOpen: Boolean,
    val editingExpense: Expense?,
    val isAddingIncome: Boolean
)

private data class ExpenseCalculatedData(
    val expenses: List<Expense>,
    val filteredExpenses: List<Expense>,
    val groupedExpenses: Map<String, List<Expense>>,
    val selectedFilter: ExpenseFilter,
    val selectedTypeFilter: TransactionTypeFilter,
    val selectedCategoryFilter: String?,
    val selectedPaymentModeFilter: String?,
    val searchQuery: String,
    val totalSpent: Double,
    val totalIncome: Double,
    val netBalance: Double,
    val cashTotal: Double,
    val upiTotal: Double,
    val statsData: StatisticsData
)

private data class CombinedPrefs(
    val lastDeleted: Expense?,
    val currency: AppCurrency,
    val dateFormat: AppDateFormat,
    val paymentModes: List<PaymentMode>,
    val categories: List<CategoryItem>,
    val incomeCategories: List<CategoryItem>
)

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList(),
    val groupedExpenses: Map<String, List<Expense>> = emptyMap(),
    val selectedFilter: ExpenseFilter = ExpenseFilter.ALL,
    val selectedTypeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val selectedCategoryFilter: String? = null,
    val selectedPaymentModeFilter: String? = null,
    val searchQuery: String = "",
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netBalance: Double = 0.0,
    val cashTotal: Double = 0.0,
    val upiTotal: Double = 0.0,
    val statsTimeRange: StatsTimeRange = StatsTimeRange.ALL_TIME,
    val statsData: StatisticsData = StatisticsData(),
    val isAddEditSheetOpen: Boolean = false,
    val isAddingIncome: Boolean = false,
    val editingExpense: Expense? = null,
    val lastDeletedExpense: Expense? = null,
    val currency: AppCurrency = AppCurrency.DEFAULT,
    val dateFormat: AppDateFormat = AppDateFormat.DEFAULT,
    val paymentModes: List<PaymentMode> = emptyList(),
    val categories: List<CategoryItem> = emptyList(),
    val incomeCategories: List<CategoryItem> = emptyList()
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    private val prefs = application.getSharedPreferences("kharchu_prefs", Context.MODE_PRIVATE)

    private val _selectedThemeColor = MutableStateFlow(
        AppThemeColor.fromId(prefs.getString("theme_color_id", AppThemeColor.DEFAULT.id))
    )
    val selectedThemeColor: StateFlow<AppThemeColor> = _selectedThemeColor.asStateFlow()

    private val _selectedThemeMode = MutableStateFlow(
        AppThemeMode.fromId(prefs.getString("theme_mode_id", AppThemeMode.DEFAULT.id))
    )
    val selectedThemeMode: StateFlow<AppThemeMode> = _selectedThemeMode.asStateFlow()

    private val _defaultCategoryId = MutableStateFlow(
        prefs.getString("default_category_id", ExpenseCategory.FOOD.id) ?: ExpenseCategory.FOOD.id
    )
    val defaultCategoryId: StateFlow<String> = _defaultCategoryId.asStateFlow()

    private val _defaultIncomeCategoryId = MutableStateFlow(
        prefs.getString("default_income_category_id", CategoryItem.INCOME_DEFAULT.id) ?: CategoryItem.INCOME_DEFAULT.id
    )
    val defaultIncomeCategoryId: StateFlow<String> = _defaultIncomeCategoryId.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(prefs.getString("last_backup_time", null))
    val lastBackupTime: StateFlow<String?> = _lastBackupTime.asStateFlow()

    private val _isBackupInProgress = MutableStateFlow(false)
    val isBackupInProgress: StateFlow<Boolean> = _isBackupInProgress.asStateFlow()

    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage: StateFlow<String?> = _backupStatusMessage.asStateFlow()

    private val _promptBackupOnOpen = MutableStateFlow(
        prefs.getBoolean("prompt_backup_on_open", true)
    )
    val promptBackupOnOpen: StateFlow<Boolean> = _promptBackupOnOpen.asStateFlow()

    fun setPromptBackupOnOpen(enabled: Boolean) {
        _promptBackupOnOpen.value = enabled
        prefs.edit().putBoolean("prompt_backup_on_open", enabled).apply()
    }

    private val _backupFrequency = MutableStateFlow(
        try {
            BackupFrequency.valueOf(prefs.getString("backup_frequency", BackupFrequency.DAILY.name) ?: BackupFrequency.DAILY.name)
        } catch (_: Exception) {
            BackupFrequency.DAILY
        }
    )
    val backupFrequency: StateFlow<BackupFrequency> = _backupFrequency.asStateFlow()

    private val _categories = MutableStateFlow(CategoryRegistry.getAllCategories(application))
    val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()

    private val _incomeCategories = MutableStateFlow(CategoryRegistry.getAllIncomeCategories(application))
    val incomeCategories: StateFlow<List<CategoryItem>> = _incomeCategories.asStateFlow()

    private val _localBackupFolderName = MutableStateFlow(LocalBackupHelper.getFolderName(application))
    val localBackupFolderName: StateFlow<String> = _localBackupFolderName.asStateFlow()

    private val _lastLocalBackupTime = MutableStateFlow(LocalBackupHelper.getLastLocalBackupTime(application))
    val lastLocalBackupTime: StateFlow<String?> = _lastLocalBackupTime.asStateFlow()

    private val _isLocalFolderConfigured = MutableStateFlow(LocalBackupHelper.getLocalBackupFolderUri(application) != null)
    val isLocalFolderConfigured: StateFlow<Boolean> = _isLocalFolderConfigured.asStateFlow()

    private val _selectedCurrency = MutableStateFlow(
        AppCurrency.fromCode(prefs.getString("selected_currency_code", AppCurrency.DEFAULT.code))
    )
    val selectedCurrency: StateFlow<AppCurrency> = _selectedCurrency.asStateFlow()

    private val _selectedDateFormat = MutableStateFlow(
        AppDateFormat.fromPattern(prefs.getString("selected_date_format_pattern", AppDateFormat.DEFAULT.pattern))
    )
    val selectedDateFormat: StateFlow<AppDateFormat> = _selectedDateFormat.asStateFlow()

    private val _paymentModes = MutableStateFlow(PaymentModeRegistry.getAllModes(application))
    val paymentModes: StateFlow<List<PaymentMode>> = _paymentModes.asStateFlow()

    private val _selectedModeNameFilter = MutableStateFlow<String?>(null)

    init {
        PaymentModeRegistry.init(application)
        val database = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(database.expenseDao())
    }

    fun setCurrency(currency: AppCurrency) {
        _selectedCurrency.value = currency
        prefs.edit().putString("selected_currency_code", currency.code).apply()
    }

    fun setDateFormat(dateFormat: AppDateFormat) {
        _selectedDateFormat.value = dateFormat
        prefs.edit().putString("selected_date_format_pattern", dateFormat.pattern).apply()
    }

    fun addPaymentMode(label: String, iconName: String = "payments", colorHex: Long = 0xFF10B981) {
        PaymentModeRegistry.addMode(getApplication(), label, iconName, colorHex)
        _paymentModes.value = PaymentModeRegistry.getAllModes(getApplication())
    }

    fun editPaymentMode(name: String, label: String, iconName: String, colorHex: Long) {
        PaymentModeRegistry.updateMode(getApplication(), name, label, iconName, colorHex)
        _paymentModes.value = PaymentModeRegistry.getAllModes(getApplication())
    }

    private val _defaultPaymentModeName = MutableStateFlow(
        prefs.getString("default_payment_mode_name", PaymentMode.UPI.name) ?: PaymentMode.UPI.name
    )
    val defaultPaymentModeName: StateFlow<String> = _defaultPaymentModeName.asStateFlow()

    fun setDefaultPaymentMode(name: String) {
        _defaultPaymentModeName.value = name
        prefs.edit().putString("default_payment_mode_name", name).apply()
    }

    fun deletePaymentMode(name: String) {
        PaymentModeRegistry.deleteMode(getApplication(), name)
        _paymentModes.value = PaymentModeRegistry.getAllModes(getApplication())
        if (_defaultPaymentModeName.value == name) {
            val first = _paymentModes.value.firstOrNull()?.name ?: PaymentMode.UPI.name
            setDefaultPaymentMode(first)
        }
        if (_selectedModeNameFilter.value.equals(name, ignoreCase = true)) {
            setPaymentModeFilter(null)
        }
    }

    private val _selectedFilter = MutableStateFlow(ExpenseFilter.ALL)
    private val _selectedTypeFilter = MutableStateFlow(TransactionTypeFilter.ALL)
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _statsTimeRange = MutableStateFlow(StatsTimeRange.ALL_TIME)
    private val _isAddEditSheetOpen = MutableStateFlow(false)
    private val _isAddingIncome = MutableStateFlow(false)
    private val _editingExpense = MutableStateFlow<Expense?>(null)
    private val _lastDeletedExpense = MutableStateFlow<Expense?>(null)

    fun setPaymentModeFilter(modeName: String?) {
        _selectedModeNameFilter.value = modeName
        if (modeName == null) {
            _selectedFilter.value = ExpenseFilter.ALL
        } else if (modeName.equals("UPI", ignoreCase = true)) {
            _selectedFilter.value = ExpenseFilter.UPI
        } else if (modeName.equals("CASH", ignoreCase = true)) {
            _selectedFilter.value = ExpenseFilter.CASH
        }
    }

    fun setThemeColor(themeColor: AppThemeColor) {
        _selectedThemeColor.value = themeColor
        prefs.edit().putString("theme_color_id", themeColor.id).apply()
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        _selectedThemeMode.value = themeMode
        prefs.edit().putString("theme_mode_id", themeMode.id).apply()
    }

    fun setDefaultCategory(categoryId: String) {
        _defaultCategoryId.value = categoryId
        prefs.edit().putString("default_category_id", categoryId).apply()
    }

    fun setDefaultIncomeCategory(categoryId: String) {
        _defaultIncomeCategoryId.value = categoryId
        prefs.edit().putString("default_income_category_id", categoryId).apply()
    }

    private val _filterParams = combine(
        _selectedFilter,
        _selectedTypeFilter,
        _selectedCategoryFilter,
        _selectedModeNameFilter,
        _searchQuery
    ) { filter, typeFilter, catFilter, modeFilter, query ->
        FilterParams(filter, typeFilter, catFilter, modeFilter, query)
    }

    private val _calculatedData = combine(
        repository.allExpenses,
        _filterParams,
        _statsTimeRange,
        _categories,
        _incomeCategories
    ) { allExpenses: List<Expense>, fp: FilterParams, timeRange: StatsTimeRange, currentCategories: List<CategoryItem>, currentIncomeCategories: List<CategoryItem> ->
        val onlyExpenses = allExpenses.filter { !it.isIncome }
        val onlyIncomes = allExpenses.filter { it.isIncome }

        val cashTotal = onlyExpenses.filter { it.paymentMode.name == "CASH" }.sumOf { it.amount }
        val upiTotal = onlyExpenses.filter { it.paymentMode.name == "UPI" }.sumOf { it.amount }
        val totalSpent = onlyExpenses.sumOf { it.amount }
        val totalIncome = onlyIncomes.sumOf { it.amount }
        val netBalance = totalIncome - totalSpent

        // Filter for transactions page
        val filtered = allExpenses.filter { expense ->
            val matchesType = when (fp.typeFilter) {
                TransactionTypeFilter.ALL -> true
                TransactionTypeFilter.EXPENSE -> !expense.isIncome
                TransactionTypeFilter.INCOME -> expense.isIncome
            }
            val matchesFilter = if (fp.modeFilter != null) {
                expense.paymentMode.name.equals(fp.modeFilter, ignoreCase = true)
            } else {
                when (fp.filter) {
                    ExpenseFilter.ALL -> true
                    ExpenseFilter.UPI -> expense.paymentMode.name == "UPI"
                    ExpenseFilter.CASH -> expense.paymentMode.name == "CASH"
                }
            }
            val resolvedCat = CategoryRegistry.resolveCategory(expense.category, currentCategories, currentIncomeCategories)
            val matchesCategory = fp.catFilter == null ||
                    expense.category.equals(fp.catFilter, ignoreCase = true) ||
                    resolvedCat.id.equals(fp.catFilter, ignoreCase = true) ||
                    resolvedCat.title.equals(fp.catFilter, ignoreCase = true)

            val matchesQuery = if (fp.query.isBlank()) {
                true
            } else {
                val q = fp.query.trim().lowercase()
                val catTitle = resolvedCat.title.lowercase()
                val typeText = if (expense.isIncome) "income" else "expense"
                expense.note.lowercase().contains(q) ||
                        catTitle.contains(q) ||
                        typeText.contains(q) ||
                        expense.amount.toString().contains(q) ||
                        expense.paymentMode.label.lowercase().contains(q) ||
                        expense.dateString.contains(q)
            }
            matchesType && matchesFilter && matchesCategory && matchesQuery
        }

        val grouped = filtered.groupBy { it.dateString }

        // Calculate statistics based on time range
        val timeRangeAll = DateUtils.filterExpensesByTimeRange(allExpenses, timeRange)
        val statsExpenses = timeRangeAll.filter { !it.isIncome }
        val statsIncomes = timeRangeAll.filter { it.isIncome }

        val statsTotal = statsExpenses.sumOf { it.amount }
        val statsIncomeTotal = statsIncomes.sumOf { it.amount }
        val statsNetBalance = statsIncomeTotal - statsTotal
        val statsCash = statsExpenses.filter { it.paymentMode.name == "CASH" }.sumOf { it.amount }
        val statsUpi = statsExpenses.filter { it.paymentMode.name == "UPI" }.sumOf { it.amount }

        val cashPct = if (statsTotal > 0) ((statsCash / statsTotal) * 100).toFloat() else 0f
        val upiPct = if (statsTotal > 0) ((statsUpi / statsTotal) * 100).toFloat() else 0f

        // Category breakdown (strictly expenses so spending stats are never distorted by income)
        val categoryGroups = statsExpenses.groupBy { exp ->
            CategoryRegistry.resolveCategory(exp.category, currentCategories).id
        }
        val categoryStats = categoryGroups.map { (catId, list) ->
            val cat = currentCategories.firstOrNull { it.id.equals(catId, ignoreCase = true) }
                ?: CategoryRegistry.resolveCategory(catId, currentCategories)
            val sum = list.sumOf { it.amount }
            val pct = if (statsTotal > 0) ((sum / statsTotal) * 100).toFloat() else 0f
            CategoryStat(
                category = cat,
                totalAmount = sum,
                count = list.size,
                percentage = pct
            )
        }.sortedByDescending { it.totalAmount }

        val topCategory = categoryStats.firstOrNull()?.category

        val statisticsData = StatisticsData(
            timeRange = timeRange,
            totalSpent = statsTotal,
            totalIncome = statsIncomeTotal,
            netBalance = statsNetBalance,
            incomeCount = statsIncomes.size,
            cashTotal = statsCash,
            upiTotal = statsUpi,
            cashPercentage = cashPct,
            upiPercentage = upiPct,
            transactionCount = statsExpenses.size,
            categoryStats = categoryStats,
            topCategory = topCategory,
            timeRangeExpenses = statsExpenses
        )

        ExpenseCalculatedData(
            expenses = allExpenses,
            filteredExpenses = filtered,
            groupedExpenses = grouped,
            selectedFilter = fp.filter,
            selectedTypeFilter = fp.typeFilter,
            selectedCategoryFilter = fp.catFilter,
            selectedPaymentModeFilter = fp.modeFilter,
            searchQuery = fp.query,
            totalSpent = totalSpent,
            totalIncome = totalIncome,
            netBalance = netBalance,
            cashTotal = cashTotal,
            upiTotal = upiTotal,
            statsData = statisticsData
        )
    }

    private val _sheetState = combine(
        _isAddEditSheetOpen,
        _editingExpense,
        _isAddingIncome
    ) { isOpen, editing, isIncome ->
        SheetStateParams(isOpen, editing, isIncome)
    }

    private val _allCategoryLists = combine(_categories, _incomeCategories) { cats, incCats ->
        cats to incCats
    }

    val uiState: StateFlow<ExpenseUiState> = combine(
        _calculatedData,
        _statsTimeRange,
        _sheetState,
        combine(_lastDeletedExpense, _selectedCurrency, _selectedDateFormat, _paymentModes, _allCategoryLists) { deleted, curr, df, modes, catPair ->
            CombinedPrefs(deleted, curr, df, modes, catPair.first, catPair.second)
        }
    ) { data, timeRange, sheet, prefs ->
        ExpenseUiState(
            expenses = data.expenses,
            filteredExpenses = data.filteredExpenses,
            groupedExpenses = data.groupedExpenses,
            selectedFilter = data.selectedFilter,
            selectedTypeFilter = data.selectedTypeFilter,
            selectedCategoryFilter = data.selectedCategoryFilter,
            selectedPaymentModeFilter = data.selectedPaymentModeFilter,
            searchQuery = data.searchQuery,
            totalSpent = data.totalSpent,
            totalIncome = data.totalIncome,
            netBalance = data.netBalance,
            cashTotal = data.cashTotal,
            upiTotal = data.upiTotal,
            statsTimeRange = timeRange,
            statsData = data.statsData,
            isAddEditSheetOpen = sheet.isOpen,
            isAddingIncome = sheet.isAddingIncome,
            editingExpense = sheet.editingExpense,
            lastDeletedExpense = prefs.lastDeleted,
            currency = prefs.currency,
            dateFormat = prefs.dateFormat,
            paymentModes = prefs.paymentModes,
            categories = prefs.categories,
            incomeCategories = prefs.incomeCategories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseUiState()
    )

    fun setFilter(filter: ExpenseFilter) {
        _selectedFilter.value = filter
    }

    fun setTypeFilter(typeFilter: TransactionTypeFilter) {
        _selectedTypeFilter.value = typeFilter
    }

    fun setCategoryFilter(categoryId: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == categoryId) null else categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatsTimeRange(range: StatsTimeRange) {
        _statsTimeRange.value = range
    }

    fun openAddSheet() {
        _editingExpense.value = null
        _isAddingIncome.value = false
        _isAddEditSheetOpen.value = true
    }

    fun openAddIncomeSheet() {
        _editingExpense.value = null
        _isAddingIncome.value = true
        _isAddEditSheetOpen.value = true
    }

    fun openEditSheet(expense: Expense) {
        _editingExpense.value = expense
        _isAddingIncome.value = expense.isIncome
        _isAddEditSheetOpen.value = true
    }

    fun closeSheet() {
        _isAddEditSheetOpen.value = false
        _isAddingIncome.value = false
        _editingExpense.value = null
    }

    fun saveExpense(
        amount: Double,
        dateString: String,
        paymentMode: PaymentMode,
        category: String,
        note: String,
        isIncome: Boolean = false
    ) {
        viewModelScope.launch {
            val dateMillis = DateUtils.dateStringToStartOfDayMillis(dateString)
            val currentEditing = _editingExpense.value
            if (currentEditing != null) {
                repository.update(
                    currentEditing.copy(
                        amount = amount,
                        dateString = dateString,
                        dateMillis = dateMillis,
                        paymentMode = paymentMode,
                        category = category,
                        note = note.trim(),
                        isIncome = isIncome
                    )
                )
            } else {
                repository.insert(
                    Expense(
                        amount = amount,
                        dateString = dateString,
                        dateMillis = dateMillis,
                        paymentMode = paymentMode,
                        category = category,
                        note = note.trim(),
                        isIncome = isIncome
                    )
                )
            }
            closeSheet()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            _lastDeletedExpense.value = expense
            repository.delete(expense)
        }
    }

    fun undoDelete() {
        val toRestore = _lastDeletedExpense.value ?: return
        viewModelScope.launch {
            repository.insert(toRestore.copy(id = 0))
            _lastDeletedExpense.value = null
        }
    }

    fun clearLastDeleted() {
        _lastDeletedExpense.value = null
    }

    fun deleteAll() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun populateSampleData() {
        viewModelScope.launch {
            val today = DateUtils.getTodayDateString()
            val yesterday = DateUtils.getYesterdayDateString()
            val samples = listOf(
                Expense(
                    amount = 180.0,
                    dateString = today,
                    dateMillis = DateUtils.dateStringToStartOfDayMillis(today),
                    paymentMode = PaymentMode.UPI,
                    category = ExpenseCategory.FOOD.id,
                    note = "Lunch Thali"
                ),
                Expense(
                    amount = 60.0,
                    dateString = today,
                    dateMillis = DateUtils.dateStringToStartOfDayMillis(today),
                    paymentMode = PaymentMode.CASH,
                    category = ExpenseCategory.TRANSPORT.id,
                    note = "Auto fare"
                ),
                Expense(
                    amount = 450.0,
                    dateString = yesterday,
                    dateMillis = DateUtils.dateStringToStartOfDayMillis(yesterday),
                    paymentMode = PaymentMode.UPI,
                    category = ExpenseCategory.GROCERIES.id,
                    note = "Milk, Fruits & Vegetables"
                ),
                Expense(
                    amount = 120.0,
                    dateString = yesterday,
                    dateMillis = DateUtils.dateStringToStartOfDayMillis(yesterday),
                    paymentMode = PaymentMode.CASH,
                    category = ExpenseCategory.FOOD.id,
                    note = "Chai and snacks"
                ),
                Expense(
                    amount = 899.0,
                    dateString = yesterday,
                    dateMillis = DateUtils.dateStringToStartOfDayMillis(yesterday),
                    paymentMode = PaymentMode.UPI,
                    category = ExpenseCategory.SHOPPING.id,
                    note = "T-shirt online"
                ),
                Expense(
                    amount = 650.0,
                    dateString = today,
                    dateMillis = DateUtils.dateStringToStartOfDayMillis(today),
                    paymentMode = PaymentMode.UPI,
                    category = ExpenseCategory.BILLS.id,
                    note = "Broadband recharge"
                )
            )
            samples.forEach { repository.insert(it) }
        }
    }

    fun clearBackupStatusMessage() {
        _backupStatusMessage.value = null
    }

    fun generateBackupJson(): String {
        return BackupUtils.createBackupJson(uiState.value.expenses)
    }

    fun restoreBackupJson(jsonString: String, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val parseResult = BackupUtils.parseBackupJson(jsonString)
            if (parseResult.isSuccess) {
                val expenses = parseResult.getOrThrow()
                repository.insertAll(expenses)
                _backupStatusMessage.value = "Successfully restored ${expenses.size} expenses"
                onComplete(Result.success(expenses.size))
            } else {
                val errorMsg = parseResult.exceptionOrNull()?.localizedMessage ?: "Invalid backup file"
                _backupStatusMessage.value = "Restore failed: $errorMsg"
                onComplete(Result.failure(Exception(errorMsg)))
            }
        }
    }

    fun updateLastBackupTime(timeStr: String) {
        _lastBackupTime.value = timeStr
        prefs.edit().putString("last_backup_time", timeStr).apply()
    }

    fun setBackupFrequency(frequency: BackupFrequency) {
        _backupFrequency.value = frequency
        prefs.edit().putString("backup_frequency", frequency.name).apply()
    }

    fun addCategory(title: String, colorHex: Long, iconName: String) {
        CategoryRegistry.addCategory(getApplication(), title, colorHex, iconName)
        _categories.value = CategoryRegistry.getAllCategories(getApplication())
    }

    fun editCategory(id: String, title: String, colorHex: Long, iconName: String) {
        CategoryRegistry.updateCategory(getApplication(), id, title, colorHex, iconName)
        _categories.value = CategoryRegistry.getAllCategories(getApplication())
    }

    fun deleteCategory(id: String) {
        CategoryRegistry.deleteCategory(getApplication(), id)
        _categories.value = CategoryRegistry.getAllCategories(getApplication())
        if (_defaultCategoryId.value == id) {
            val first = _categories.value.firstOrNull()?.id ?: ExpenseCategory.OTHER.id
            setDefaultCategory(first)
        }
    }

    fun addIncomeCategory(title: String, colorHex: Long, iconName: String) {
        CategoryRegistry.addIncomeCategory(getApplication(), title, colorHex, iconName)
        _incomeCategories.value = CategoryRegistry.getAllIncomeCategories(getApplication())
    }

    fun editIncomeCategory(id: String, title: String, colorHex: Long, iconName: String) {
        CategoryRegistry.updateIncomeCategory(getApplication(), id, title, colorHex, iconName)
        _incomeCategories.value = CategoryRegistry.getAllIncomeCategories(getApplication())
    }

    fun deleteIncomeCategory(id: String) {
        CategoryRegistry.deleteIncomeCategory(getApplication(), id)
        _incomeCategories.value = CategoryRegistry.getAllIncomeCategories(getApplication())
        if (_defaultIncomeCategoryId.value == id) {
            val first = _incomeCategories.value.firstOrNull()?.id ?: CategoryItem.INCOME_DEFAULT.id
            setDefaultIncomeCategory(first)
        }
    }

    fun setLocalBackupFolder(uri: Uri) {
        val result = LocalBackupHelper.setLocalBackupFolder(getApplication(), uri)
        if (result.isSuccess) {
            _localBackupFolderName.value = result.getOrThrow()
            _isLocalFolderConfigured.value = true
            performLocalBackup()
        }
    }

    fun performLocalBackup(onComplete: ((Result<String>) -> Unit)? = null) {
        viewModelScope.launch {
            _isBackupInProgress.value = true
            val json = generateBackupJson()
            val result = LocalBackupHelper.performLocalBackup(getApplication(), json)
            _isBackupInProgress.value = false
            if (result.isSuccess) {
                _lastLocalBackupTime.value = LocalBackupHelper.getLastLocalBackupTime(getApplication())
                _backupStatusMessage.value = "Local backup saved to folder: ${result.getOrNull()}"
            } else {
                _backupStatusMessage.value = "Local backup failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
            onComplete?.invoke(result)
        }
    }

    fun checkAndPerformAutoBackup(context: Context) {
        // Daily untouched local backup check
        if (LocalBackupHelper.getLocalBackupFolderUri(context) != null && LocalBackupHelper.isDailyBackupDueToday(context)) {
            performLocalBackup()
        }
    }
}

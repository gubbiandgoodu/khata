package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryItem
import com.example.data.model.CategoryRegistry
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMode
import com.example.data.model.PaymentModeRegistry
import com.example.ui.components.ExpenseItemRow
import com.example.ui.theme.CashGreenAccent
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.UpiIndigoAccent
import com.example.ui.viewmodel.ExpenseFilter
import com.example.ui.viewmodel.ExpenseUiState
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.TransactionTypeFilter
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: ExpenseUiState,
    viewModel: ExpenseViewModel,
    onAddIncome: () -> Unit = {},
    onEditExpense: (Expense) -> Unit,
    onRequestDeleteExpense: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedExpenseForAction by remember { mutableStateOf<Expense?>(null) }
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar with zero extra insets so header space is reduced
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            title = {
                if (isSearchActive) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search note, category, amount...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .testTag("search_text_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                } else {
                    Column {
                        Text(
                            text = "Transactions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${uiState.expenses.size} ${if (uiState.expenses.size == 1) "entry" else "entries"} recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) viewModel.setSearchQuery("")
                    },
                    modifier = Modifier.testTag("toggle_search_button")
                ) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                        contentDescription = if (isSearchActive) "Close search" else "Search",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Upward scrollable list containing all transactions
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("transactions_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cashflow Summary Strip (Income in Green | Expense in Red | Balance)
            if (uiState.totalIncome > 0 || uiState.totalSpent > 0) {
                item(key = "cashflow_summary_card") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cashflow_summary_card"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Income column (Green)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "INCOME",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "+${uiState.currency.format(uiState.totalIncome)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("summary_total_income")
                                )
                            }

                            // Expense column (Red)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "EXPENSE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uiState.currency.format(uiState.totalSpent),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE53935),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("summary_total_expense")
                                )
                            }

                            // Balance column
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "BALANCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val balancePrefix = if (uiState.netBalance < 0) "-" else ""
                                val absBalance = kotlin.math.abs(uiState.netBalance)
                                Text(
                                    text = "$balancePrefix${uiState.currency.format(absBalance)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.netBalance >= 0) IncomeGreen else Color(0xFFE53935),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("summary_net_balance")
                                )
                            }
                        }
                    }
                }
            }

            // Transaction Type Filter Pills (All / Expenses / Income)
            item(key = "type_filter_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TransactionTypeFilter.entries.forEach { typeFilter ->
                        val isSelected = uiState.selectedTypeFilter == typeFilter
                        val activeColor = when (typeFilter) {
                            TransactionTypeFilter.INCOME -> IncomeGreen
                            TransactionTypeFilter.EXPENSE -> ExpenseRed
                            TransactionTypeFilter.ALL -> MaterialTheme.colorScheme.primary
                        }
                        Surface(
                            onClick = { viewModel.setTypeFilter(typeFilter) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) activeColor else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("type_filter_${typeFilter.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = typeFilter.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Single row containing Payment Mode and Category dropdown filters
            item(key = "filters_dropdown_row") {
                var modeDropdownExpanded by remember { mutableStateOf(false) }
                var categoryDropdownExpanded by remember { mutableStateOf(false) }

                val context = androidx.compose.ui.platform.LocalContext.current
                val allModes = PaymentModeRegistry.getAllModes(context)
                val selectedModeItem = allModes.firstOrNull { it.name.equals(uiState.selectedPaymentModeFilter, ignoreCase = true) }
                val hasActiveFilter = uiState.selectedPaymentModeFilter != null ||
                        uiState.selectedFilter != ExpenseFilter.ALL ||
                        uiState.selectedCategoryFilter != null ||
                        uiState.selectedTypeFilter != TransactionTypeFilter.ALL

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Payment Mode Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        val modeLabel = selectedModeItem?.label ?: when (uiState.selectedFilter) {
                            ExpenseFilter.ALL -> "All Modes"
                            ExpenseFilter.UPI -> "UPI"
                            ExpenseFilter.CASH -> "Cash"
                        }
                        val modeIcon = selectedModeItem?.icon ?: when (uiState.selectedFilter) {
                            ExpenseFilter.ALL -> Icons.Default.FilterList
                            ExpenseFilter.UPI -> Icons.Default.QrCode
                            ExpenseFilter.CASH -> Icons.Default.Payments
                        }
                        val modeIconTint = selectedModeItem?.color ?: when (uiState.selectedFilter) {
                            ExpenseFilter.ALL -> MaterialTheme.colorScheme.onSurfaceVariant
                            ExpenseFilter.UPI -> UpiIndigoAccent
                            ExpenseFilter.CASH -> CashGreenAccent
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { modeDropdownExpanded = true }
                                .border(
                                    width = 1.dp,
                                    color = if (hasActiveFilter) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .testTag("mode_dropdown_trigger"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedModeItem != null || uiState.selectedFilter != ExpenseFilter.ALL) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = null,
                                        tint = modeIconTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = modeLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selectedModeItem != null || uiState.selectedFilter != ExpenseFilter.ALL) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Mode",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = modeDropdownExpanded,
                            onDismissRequest = { modeDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Modes (${uiState.expenses.size})") },
                                onClick = {
                                    viewModel.setPaymentModeFilter(null)
                                    modeDropdownExpanded = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("mode_menu_item_all")
                            )
                            allModes.forEach { mode ->
                                val count = uiState.expenses.count { it.paymentMode.name.equals(mode.name, ignoreCase = true) }
                                DropdownMenuItem(
                                    text = { Text("${mode.label} ($count)") },
                                    onClick = {
                                        viewModel.setPaymentModeFilter(mode.name)
                                        modeDropdownExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(mode.icon, contentDescription = null, tint = mode.color, modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier.testTag("mode_menu_item_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // 2. Category Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        val selectedCat = uiState.selectedCategoryFilter?.let { catId ->
                            uiState.categories.firstOrNull { it.id.equals(catId, ignoreCase = true) }
                                ?: CategoryRegistry.getCategory(context, catId)
                        }
                        val catLabel = selectedCat?.title ?: "All Categories"
                        val catIcon = selectedCat?.icon ?: Icons.Default.Category
                        val catColor = selectedCat?.color ?: MaterialTheme.colorScheme.onSurfaceVariant

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { categoryDropdownExpanded = true }
                                .border(
                                    width = 1.dp,
                                    color = if (selectedCat != null) catColor else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .testTag("category_dropdown_trigger"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedCat != null) {
                                catColor.copy(alpha = 0.1f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = catLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selectedCat != null) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Category",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Categories") },
                                onClick = {
                                    viewModel.setCategoryFilter(null)
                                    categoryDropdownExpanded = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                modifier = Modifier.testTag("cat_menu_item_all")
                            )
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val expenseCats = if (uiState.categories.isNotEmpty()) uiState.categories else CategoryRegistry.getAllCategories(context)
                            val displayCategories = when (uiState.selectedTypeFilter) {
                                TransactionTypeFilter.INCOME -> CategoryItem.INCOME_CATEGORIES
                                TransactionTypeFilter.EXPENSE -> expenseCats
                                TransactionTypeFilter.ALL -> expenseCats + CategoryItem.INCOME_CATEGORIES.filter { incCat ->
                                    uiState.expenses.any { it.category.equals(incCat.id, ignoreCase = true) }
                                }
                            }
                            displayCategories.forEach { cat ->
                                val count = uiState.expenses.count { exp ->
                                    exp.category.equals(cat.id, ignoreCase = true) ||
                                            exp.category.equals(cat.title, ignoreCase = true) ||
                                            CategoryRegistry.resolveCategory(exp.category, expenseCats).id.equals(cat.id, ignoreCase = true)
                                }
                                DropdownMenuItem(
                                    text = { Text("${cat.title} ($count)") },
                                    onClick = {
                                        viewModel.setCategoryFilter(cat.id)
                                        categoryDropdownExpanded = false
                                    },
                                    leadingIcon = {
                                        Icon(cat.icon, contentDescription = null, tint = cat.color, modifier = Modifier.size(18.dp))
                                    },
                                    modifier = Modifier.testTag("cat_menu_item_${cat.id}")
                                )
                            }
                        }
                    }

                    // 3. Clear button if any filter is active
                    if (hasActiveFilter) {
                        IconButton(
                            onClick = {
                                viewModel.setPaymentModeFilter(null)
                                viewModel.setFilter(ExpenseFilter.ALL)
                                viewModel.setCategoryFilter(null)
                                viewModel.setTypeFilter(TransactionTypeFilter.ALL)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("clear_filters_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear filters",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Empty State
            if (uiState.filteredExpenses.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                            .testTag("empty_state_container"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.expenses.isEmpty()) "No Transactions Yet" else "No matching transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.expenses.isEmpty()) {
                                    "Tap '+' to log your first expense or income."
                                } else {
                                    "Try clearing filters or search keywords."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            } else {
                // Grouped by Date according to selected Date Format
                uiState.groupedExpenses.forEach { (dateStr, expensesForDate) ->
                    // Date Header with Day Income (Green) and Day Expense (Red)
                    item(key = "header_$dateStr") {
                        val dayExpenseTotal = expensesForDate.filter { !it.isIncome }.sumOf { it.amount }
                        val dayIncomeTotal = expensesForDate.filter { it.isIncome }.sumOf { it.amount }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateUtils.formatDisplayDate(dateStr, uiState.dateFormat),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (dayIncomeTotal > 0) {
                                    Text(
                                        text = "+${uiState.currency.format(dayIncomeTotal)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen,
                                        modifier = Modifier.testTag("day_income_total_$dateStr")
                                    )
                                }
                                if (dayExpenseTotal > 0) {
                                    Text(
                                        text = uiState.currency.format(dayExpenseTotal),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE53935),
                                        modifier = Modifier.testTag("day_total_$dateStr")
                                    )
                                }
                            }
                        }
                    }

                    // Expense Item Rows for this date
                    items(
                        items = expensesForDate,
                        key = { it.id }
                    ) { expense ->
                        ExpenseItemRow(
                            expense = expense,
                            currency = uiState.currency,
                            onClick = { selectedExpenseForAction = expense },
                            onLongClick = { selectedExpenseForAction = expense }
                        )
                    }
                }
            }
        }
    }

    // Single/Long Press Action Dialog (Edit & Delete options)
    selectedExpenseForAction?.let { expense ->
        val context = LocalContext.current
        val category = CategoryRegistry.getCategory(context, expense.category)
        val formattedAmount = uiState.currency.format(expense.amount)
        val isIncome = expense.isIncome
        val accentColor = if (isIncome) IncomeGreen else category.color

        AlertDialog(
            onDismissRequest = { selectedExpenseForAction = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = category.title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = if (expense.note.isNotBlank()) expense.note else category.title,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isIncome) "+$formattedAmount" else formattedAmount,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isIncome) IncomeGreen else ExpenseRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${DateUtils.formatShortDate(expense.dateString, uiState.dateFormat)} • ${category.title} • ${expense.paymentMode.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Edit Button
                    Button(
                        onClick = {
                            val target = expense
                            selectedExpenseForAction = null
                            onEditExpense(target)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_edit_expense_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isIncome) IncomeGreen else ExpenseRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isIncome) "Edit Income" else "Edit Expense", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Delete Button
                    OutlinedButton(
                        onClick = {
                            val target = expense
                            selectedExpenseForAction = null
                            onRequestDeleteExpense(target)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_delete_expense_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isIncome) "Delete Income" else "Delete Expense", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { selectedExpenseForAction = null },
                    modifier = Modifier.testTag("action_cancel_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

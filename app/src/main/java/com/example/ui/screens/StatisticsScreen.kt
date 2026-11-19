package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryRegistry
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.ui.components.ExpenseItemRow
import com.example.ui.theme.AppCurrency
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.UpiIndigo
import com.example.ui.theme.UpiIndigoAccent
import com.example.ui.viewmodel.CategoryStat
import com.example.ui.viewmodel.ExpenseUiState
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.StatsTimeRange
import com.example.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    uiState: ExpenseUiState,
    viewModel: ExpenseViewModel,
    onEditExpense: (Expense) -> Unit = {},
    onRequestDeleteExpense: (Expense) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCategoryForDetail by remember { mutableStateOf<ExpenseCategory?>(null) }
    var selectedExpenseForAction by remember { mutableStateOf<Expense?>(null) }

    // If category is clicked, open transactions made in that category as a FULL PAGE (not a popup)
    if (selectedCategoryForDetail != null) {
        val category = selectedCategoryForDetail!!
        CategoryTransactionsScreen(
            category = category,
            uiState = uiState,
            initialTimeRange = uiState.statsTimeRange,
            onBack = { selectedCategoryForDetail = null },
            onExpenseClick = { selectedExpenseForAction = it },
            modifier = modifier
        )

        // Single / Long press action dialog for transaction
        selectedExpenseForAction?.let { expense ->
            val context = LocalContext.current
            val cat = CategoryRegistry.getCategory(context, expense.category)
            val formattedAmount = uiState.currency.format(expense.amount)

            AlertDialog(
                onDismissRequest = { selectedExpenseForAction = null },
                title = { Text("Expense Details", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Amount: $formattedAmount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Category: ${cat.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Payment Mode: ${expense.paymentMode.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Date: ${DateUtils.formatDisplayDate(expense.dateString, uiState.dateFormat)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (expense.note.isNotBlank()) {
                            Text(
                                text = "Note: ${expense.note}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val exp = expense
                            selectedExpenseForAction = null
                            onEditExpense(exp)
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            val exp = expense
                            selectedExpenseForAction = null
                            onRequestDeleteExpense(exp)
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = DangerRed)
                    }
                }
            )
        }
        return
    }

    val stats = uiState.statsData
    val currency = uiState.currency

    val formattedTotal = currency.format(stats.totalSpent)
    val formattedCash = currency.format(stats.cashTotal)
    val formattedUpi = currency.format(stats.upiTotal)

    val avgExpense = if (stats.transactionCount > 0) stats.totalSpent / stats.transactionCount else 0.0
    val formattedAvg = currency.format(avgExpense)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar with zero extra insets to reduce header space
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            title = {
                Column {
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Category & Payment Analytics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("statistics_scroll_column"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Time Range Selector
            item(key = "stats_time_range") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatsTimeRange.entries.forEach { range ->
                        FilterChip(
                            selected = uiState.statsTimeRange == range,
                            onClick = { viewModel.setStatsTimeRange(range) },
                            label = { Text(range.label) },
                            modifier = Modifier.testTag("stats_time_range_${range.name.lowercase()}")
                        )
                    }
                }
            }

            // 2. Spending Hero Card
            item(key = "stats_hero_card") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stats_hero_card"),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "TOTAL SPENT (${uiState.statsTimeRange.label.uppercase()})",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formattedTotal,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (stats.totalIncome > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Income: ",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "+${currency.format(stats.totalIncome)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Balance: ",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val prefix = if (stats.netBalance < 0) "-" else ""
                                    Text(
                                        text = "$prefix${currency.format(kotlin.math.abs(stats.netBalance))}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (stats.netBalance >= 0) IncomeGreen else DangerRed
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Category distribution rainbow split bar
                        if (stats.categoryStats.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                stats.categoryStats.forEach { catStat ->
                                    val weight = (catStat.percentage / 100f).coerceAtLeast(0.02f)
                                    Box(
                                        modifier = Modifier
                                            .weight(weight)
                                            .height(8.dp)
                                            .background(catStat.category.color)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Key metrics row (Avg, Transactions, Top Category)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Avg / Expense",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formattedAvg,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(
                                    text = "Total Entries",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${stats.transactionCount}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(
                                    text = "Top Category",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stats.topCategory?.title ?: "—",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = stats.topCategory?.color ?: MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 3. Payment Mode Comparison (Cash vs UPI)
            item(key = "mode_comparison") {
                Text(
                    text = "Payment Mode Comparison",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // UPI Stat Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stats_upi_card"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(UpiIndigo.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = "UPI",
                                        tint = UpiIndigoAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "%.1f%%".format(stats.upiPercentage),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = UpiIndigoAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "UPI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedUpi,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Cash Stat Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stats_cash_card"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(CashGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = "Cash",
                                        tint = CashGreenAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "%.1f%%".format(stats.cashPercentage),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CashGreenAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Cash",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formattedCash,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 4. Categories Breakdown
            item(key = "categories_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Spending by Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Tap any category to view full transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${stats.categoryStats.size} active",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (stats.categoryStats.isEmpty()) {
                item(key = "categories_empty") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No category data available for ${uiState.statsTimeRange.label.lowercase()}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = stats.categoryStats,
                    key = { _, catStat -> catStat.category.id }
                ) { index, catStat ->
                    CategoryStatItem(
                        rank = index + 1,
                        stat = catStat,
                        currency = currency,
                        onClick = { selectedCategoryForDetail = catStat.category }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryStatItem(
    rank: Int,
    stat: CategoryStat,
    currency: AppCurrency = AppCurrency.DEFAULT,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val formattedAmount = currency.format(stat.totalAmount)
    val progress = (stat.percentage / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "cat_progress")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("cat_stat_${stat.category.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(stat.category.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = stat.category.icon,
                        contentDescription = stat.category.title,
                        tint = stat.category.color,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stat.category.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stat.count} ${if (stat.count == 1) "transaction" else "transactions"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%.1f%%".format(stat.percentage),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = stat.category.color
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "View category transactions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = stat.category.color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

// FULL PAGE SCREEN FOR TRANSACTIONS IN A SPECIFIC CATEGORY
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(
    category: ExpenseCategory,
    uiState: ExpenseUiState,
    initialTimeRange: StatsTimeRange,
    onBack: () -> Unit,
    onExpenseClick: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var selectedTimeRange by remember { mutableStateOf(initialTimeRange) }

    val activeCategory = uiState.categories.firstOrNull { it.id.equals(category.id, ignoreCase = true) } ?: category

    val categoryExpenses = remember(uiState.expenses, activeCategory.id, activeCategory.title, selectedTimeRange, uiState.categories) {
        val inCategory = uiState.expenses.filter { exp ->
            !exp.isIncome && (
                exp.category.equals(activeCategory.id, ignoreCase = true) ||
                        exp.category.equals(activeCategory.title, ignoreCase = true) ||
                        CategoryRegistry.resolveCategory(exp.category, uiState.categories).id.equals(activeCategory.id, ignoreCase = true)
            )
        }
        DateUtils.filterExpensesByTimeRange(inCategory, selectedTimeRange)
    }

    val totalAmount = remember(categoryExpenses) { categoryExpenses.sumOf { it.amount } }
    val cashTotal = remember(categoryExpenses) { categoryExpenses.filter { it.paymentMode.name == "CASH" }.sumOf { it.amount } }
    val upiTotal = remember(categoryExpenses) { categoryExpenses.filter { it.paymentMode.name == "UPI" }.sumOf { it.amount } }
    val currency = uiState.currency

    val groupedExpenses = remember(categoryExpenses) {
        categoryExpenses.groupBy { it.dateString }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("category_transactions_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Statistics"
                    )
                }
            },
            title = {
                Column {
                    Text(
                        text = activeCategory.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${categoryExpenses.size} ${if (categoryExpenses.size == 1) "transaction" else "transactions"} • ${currency.format(totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(activeCategory.color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = activeCategory.icon,
                        contentDescription = activeCategory.title,
                        tint = activeCategory.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("category_transactions_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Time Range Selector
            item(key = "cat_time_range") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatsTimeRange.entries.forEach { range ->
                        FilterChip(
                            selected = selectedTimeRange == range,
                            onClick = { selectedTimeRange = range },
                            label = { Text(range.label) },
                            modifier = Modifier.testTag("cat_trans_time_${range.name.lowercase()}")
                        )
                    }
                }
            }

            // Category Summary Hero Card
            item(key = "cat_hero_card") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_detail_hero_card"),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(activeCategory.color.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = activeCategory.icon,
                                        contentDescription = null,
                                        tint = activeCategory.color,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = activeCategory.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${selectedTimeRange.label} Expenses",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = currency.format(totalAmount),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = activeCategory.color
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mini metrics row: UPI spent vs Cash spent vs Avg
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "UPI Spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currency.format(upiTotal),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = UpiIndigoAccent
                                )
                            }
                            Column {
                                Text(
                                    text = "Cash Spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currency.format(cashTotal),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CashGreenAccent
                                )
                            }
                            Column {
                                Text(
                                    text = "Avg / Entry",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val avg = if (categoryExpenses.isNotEmpty()) totalAmount / categoryExpenses.size else 0.0
                                Text(
                                    text = currency.format(avg),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Transactions Header
            item(key = "trans_header") {
                Text(
                    text = "Transactions (${categoryExpenses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            if (categoryExpenses.isEmpty()) {
                item(key = "empty_category_expenses") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(activeCategory.color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = activeCategory.icon,
                                    contentDescription = null,
                                    tint = activeCategory.color,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No transactions in ${activeCategory.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No expenses recorded for this category in ${selectedTimeRange.label.lowercase()}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                groupedExpenses.forEach { (dateStr, dateExpenses) ->
                    item(key = "header_$dateStr") {
                        val dayTotal = dateExpenses.sumOf { it.amount }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateUtils.formatDisplayDate(dateStr, uiState.dateFormat),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = currency.format(dayTotal),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935)
                            )
                        }
                    }

                    items(
                        items = dateExpenses,
                        key = { it.id }
                    ) { expense ->
                        ExpenseItemRow(
                            expense = expense,
                            currency = currency,
                            onClick = { onExpenseClick(expense) },
                            onLongClick = { onExpenseClick(expense) }
                        )
                    }
                }
            }
        }
    }
}

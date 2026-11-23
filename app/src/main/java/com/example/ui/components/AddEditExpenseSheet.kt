package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryItem
import com.example.data.model.CategoryRegistry
import com.example.data.model.Expense
import com.example.data.model.ExpenseCategory
import com.example.data.model.PaymentMode
import com.example.data.model.PaymentModeRegistry
import com.example.ui.theme.AppCurrency
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenAccent
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.UpiIndigo
import com.example.ui.theme.UpiIndigoAccent
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseSheet(
    sheetState: SheetState,
    initialExpense: Expense?,
    initialIsIncome: Boolean = false,
    defaultCategory: ExpenseCategory = ExpenseCategory.FOOD,
    defaultIncomeCategory: ExpenseCategory = CategoryItem.INCOME_DEFAULT,
    currency: AppCurrency = AppCurrency.DEFAULT,
    paymentModes: List<PaymentMode> = emptyList(),
    categories: List<CategoryItem> = emptyList(),
    incomeCategories: List<CategoryItem> = emptyList(),
    onSetDefaultCategory: ((String) -> Unit)? = null,
    onSetDefaultIncomeCategory: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (amount: Double, dateString: String, paymentMode: PaymentMode, category: String, note: String, isIncome: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEditing = initialExpense != null

    var isIncome by remember(initialExpense, initialIsIncome) {
        mutableStateOf(initialExpense?.isIncome ?: initialIsIncome)
    }

    val effectiveModes = remember(paymentModes) {
        if (paymentModes.isNotEmpty()) paymentModes else PaymentModeRegistry.getAllModes(context)
    }

    val expenseCategories = remember(categories) {
        if (categories.isNotEmpty()) categories else CategoryRegistry.getAllCategories(context)
    }
    val effectiveIncomeCategories = remember(incomeCategories) {
        if (incomeCategories.isNotEmpty()) incomeCategories else CategoryRegistry.getAllIncomeCategories(context)
    }
    val activeCategories = if (isIncome) effectiveIncomeCategories else expenseCategories
    val accentColor = if (isIncome) IncomeGreen else ExpenseRed

    var amountText by remember(initialExpense) {
        mutableStateOf(
            if (initialExpense != null) {
                if (initialExpense.amount % 1.0 == 0.0) {
                    initialExpense.amount.toLong().toString()
                } else {
                    initialExpense.amount.toString()
                }
            } else ""
        )
    }

    var selectedMode by remember(initialExpense, effectiveModes) {
        mutableStateOf(
            initialExpense?.paymentMode
                ?: effectiveModes.firstOrNull { it.name == "UPI" }
                ?: effectiveModes.firstOrNull()
                ?: PaymentMode.UPI
        )
    }

    var selectedCategory by remember(initialExpense, defaultCategory, defaultIncomeCategory, expenseCategories, effectiveIncomeCategories, initialIsIncome) {
        mutableStateOf(
            if (initialExpense != null) {
                CategoryRegistry.resolveCategory(initialExpense.category, expenseCategories, effectiveIncomeCategories)
            } else if (initialIsIncome) {
                defaultIncomeCategory
            } else {
                defaultCategory
            }
        )
    }

    // Category dropdown expansion state
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Date strictly (yyyy-MM-dd), defaults to today
    var selectedDateString by remember(initialExpense) {
        mutableStateOf(initialExpense?.dateString ?: DateUtils.getTodayDateString())
    }

    var noteText by remember(initialExpense) {
        mutableStateOf(initialExpense?.note ?: "")
    }

    var amountError by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(initialExpense) {
        if (initialExpense == null) {
            focusRequester.requestFocus()
        }
    }

    val todayString = remember { DateUtils.getTodayDateString() }
    val yesterdayString = remember { DateUtils.getYesterdayDateString() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header: Title and Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when {
                            isEditing && isIncome -> "Edit Income"
                            isEditing -> "Edit Expense"
                            isIncome -> "New Income"
                            else -> "New Expense"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = "Khata • Minimal Entry",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Expense / Income Segmented Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val expenseSelected = !isIncome
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .clickable {
                            if (isIncome) {
                                isIncome = false
                                if (expenseCategories.none { it.id == selectedCategory.id }) {
                                    selectedCategory = defaultCategory
                                }
                            }
                        }
                        .testTag("sheet_type_expense"),
                    shape = RoundedCornerShape(11.dp),
                    color = if (expenseSelected) ExpenseRed else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (expenseSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Expense",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (expenseSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .clickable {
                            if (!isIncome) {
                                isIncome = true
                                if (effectiveIncomeCategories.none { it.id == selectedCategory.id }) {
                                    selectedCategory = defaultIncomeCategory
                                }
                            }
                        }
                        .testTag("sheet_type_income"),
                    shape = RoundedCornerShape(11.dp),
                    color = if (isIncome) IncomeGreen else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. AMOUNT FIELD
            Text(
                text = if (isIncome) "INCOME AMOUNT" else "EXPENSE AMOUNT",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        amountText = filtered
                        amountError = null
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("amount_input_field"),
                placeholder = {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    Text(
                        text = if (isIncome) "+${currency.symbol}" else currency.symbol,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = accentColor.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 2. PAYMENT MODE
            Text(
                text = "PAYMENT MODE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (effectiveModes.size <= 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    effectiveModes.forEach { mode ->
                        val isSelected = selectedMode.name.equals(mode.name, ignoreCase = true)
                        val modeColor = when {
                            mode.name == "UPI" -> UpiIndigoAccent
                            mode.name == "CASH" -> CashGreenAccent
                            else -> mode.color
                        }
                        val modeBg = when {
                            mode.name == "UPI" -> UpiIndigo.copy(alpha = 0.12f)
                            mode.name == "CASH" -> CashGreen.copy(alpha = 0.12f)
                            else -> mode.color.copy(alpha = 0.12f)
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedMode = mode }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) modeColor else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .testTag("mode_${mode.name.lowercase()}_option"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) modeBg else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) modeColor else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = mode.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) modeColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (mode.name == "UPI") "Digital" else if (mode.name == "CASH") "Physical" else "Active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(effectiveModes, key = { it.name }) { mode ->
                        val isSelected = selectedMode.name.equals(mode.name, ignoreCase = true)
                        val modeColor = when {
                            mode.name == "UPI" -> UpiIndigoAccent
                            mode.name == "CASH" -> CashGreenAccent
                            else -> mode.color
                        }
                        val modeBg = when {
                            mode.name == "UPI" -> UpiIndigo.copy(alpha = 0.12f)
                            mode.name == "CASH" -> CashGreen.copy(alpha = 0.12f)
                            else -> mode.color.copy(alpha = 0.12f)
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedMode = mode }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) modeColor else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .testTag("mode_${mode.name.lowercase()}_option"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) modeBg else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) modeColor else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = mode.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) modeColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (mode.name == "UPI") "Digital" else if (mode.name == "CASH") "Physical" else "Active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. CATEGORY DROPDOWN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isIncome) "INCOME SOURCE" else "CATEGORY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                // Option to set this category as default (for expenses or income)
                val setDefaultCallback = if (isIncome) onSetDefaultIncomeCategory else onSetDefaultCategory
                val currentDefaultForType = if (isIncome) defaultIncomeCategory else defaultCategory
                if (setDefaultCallback != null) {
                    val isCurrentDefault = selectedCategory.id == currentDefaultForType.id
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { setDefaultCallback(selectedCategory.id) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("set_default_category_chip"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCurrentDefault) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Default Category",
                            tint = if (isCurrentDefault) selectedCategory.color else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCurrentDefault) "Default" else "Set as default",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrentDefault) selectedCategory.color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { categoryDropdownExpanded = true }
                        .border(
                            width = 1.5.dp,
                            color = selectedCategory.color.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("add_expense_category_dropdown"),
                    shape = RoundedCornerShape(16.dp),
                    color = selectedCategory.color.copy(alpha = 0.08f),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(selectedCategory.color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = selectedCategory.icon,
                                    contentDescription = null,
                                    tint = selectedCategory.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = selectedCategory.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap to choose category",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand category dropdown",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    activeCategories.forEach { category ->
                        val isSelected = selectedCategory.id == category.id
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = category.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) category.color else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(category.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = category.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = category.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                selectedCategory = category
                                categoryDropdownExpanded = false
                            },
                            modifier = Modifier.testTag("add_cat_menu_item_${category.id}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. DATE (STRICTLY DATE ONLY, NO TIME)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DATE (NO TIME)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Text(
                    text = DateUtils.formatDisplayDate(selectedDateString),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedDateString == todayString,
                    onClick = { selectedDateString = todayString },
                    label = { Text("Today") },
                    leadingIcon = if (selectedDateString == todayString) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("date_chip_today")
                )

                FilterChip(
                    selected = selectedDateString == yesterdayString,
                    onClick = { selectedDateString = yesterdayString },
                    label = { Text("Yesterday") },
                    leadingIcon = if (selectedDateString == yesterdayString) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("date_chip_yesterday")
                )

                val isCustomDate = selectedDateString != todayString && selectedDateString != yesterdayString
                FilterChip(
                    selected = isCustomDate,
                    onClick = {
                        val (year, month, day) = DateUtils.parseDateComponents(selectedDateString)
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val (newDateStr, _) = DateUtils.normalizeToStartOfDay(y, m, d)
                                selectedDateString = newDateStr
                            },
                            year,
                            month,
                            day
                        ).show()
                    },
                    label = {
                        Text(if (isCustomDate) DateUtils.formatShortDate(selectedDateString) else "Pick Date")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Pick Date",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("date_chip_calendar")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. NOTE / REMARK (OPTIONAL)
            Text(
                text = "NOTE / REMARK (OPTIONAL)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input_field"),
                placeholder = {
                    Text(
                        if (isIncome) "e.g. Monthly Salary, Freelance, Refund" else "e.g. Chai, Dinner with team, Petrol, Uber",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val parsed = amountText.toDoubleOrNull()
                        if (parsed != null && parsed > 0) {
                            onSave(parsed, selectedDateString, selectedMode, selectedCategory.id, noteText, isIncome)
                        } else {
                            amountError = "Please enter a valid amount"
                        }
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 6. SAVE BUTTON
            Button(
                onClick = {
                    val parsed = amountText.toDoubleOrNull()
                    if (parsed == null || parsed <= 0) {
                        amountError = "Enter an amount greater than 0"
                        return@Button
                    }
                    onSave(parsed, selectedDateString, selectedMode, selectedCategory.id, noteText, isIncome)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_expense_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = when {
                        isEditing && isIncome -> "Update Income"
                        isEditing -> "Update Expense"
                        isIncome -> "Save Income"
                        else -> "Save Expense"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

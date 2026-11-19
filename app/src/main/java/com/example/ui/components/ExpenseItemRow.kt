package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryRegistry
import com.example.data.model.Expense
import com.example.data.model.PaymentMode
import com.example.ui.theme.AppCurrency
import com.example.ui.theme.CashGreenAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.UpiIndigoAccent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpenseItemRow(
    expense: Expense,
    currency: AppCurrency = AppCurrency.DEFAULT,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val formattedAmount = currency.format(expense.amount)
    val isIncome = expense.isIncome

    val isCash = expense.paymentMode.name == "CASH"
    val isUpi = expense.paymentMode.name == "UPI"
    val modeColor = when {
        isCash -> CashGreenAccent
        isUpi -> UpiIndigoAccent
        else -> expense.paymentMode.color
    }
    val category = CategoryRegistry.getCategory(context, expense.category)
    val badgeColor = if (isIncome) IncomeGreen else category.color

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick ?: onClick
            )
            .testTag("expense_item_${expense.id}"),
        shape = RoundedCornerShape(16.dp),
        color = if (isIncome) IncomeGreen.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.title,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details: Note (or Category Title) + Category Name • Mode
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (expense.note.isNotBlank()) expense.note else category.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isIncome) {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = expense.paymentMode.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = modeColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Amount: green with '+' for Income, default surface color for Expense
            Text(
                text = if (isIncome) "+$formattedAmount" else formattedAmount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

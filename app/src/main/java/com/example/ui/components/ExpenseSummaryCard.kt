package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppCurrency
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenAccent
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.UpiIndigo
import com.example.ui.theme.UpiIndigoAccent

@Composable
fun ExpenseSummaryCard(
    totalSpent: Double,
    cashTotal: Double,
    upiTotal: Double,
    currency: AppCurrency = AppCurrency.DEFAULT,
    modifier: Modifier = Modifier
) {
    val formattedTotal = currency.format(totalSpent)
    val formattedCash = currency.format(cashTotal)
    val formattedUpi = currency.format(upiTotal)

    val cashFraction = if (totalSpent > 0) (cashTotal / totalSpent).toFloat().coerceIn(0f, 1f) else 0.5f
    val animatedCashFraction by animateFloatAsState(targetValue = cashFraction, label = "cash_fraction")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("expense_summary_card"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "TOTAL EXPENSES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formattedTotal,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Minimal ratio bar
            if (totalSpent > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    if (cashTotal > 0) {
                        Box(
                            modifier = Modifier
                                .weight(animatedCashFraction.coerceAtLeast(0.02f))
                                .height(6.dp)
                                .background(CashGreenAccent)
                        )
                    }
                    if (upiTotal > 0) {
                        Box(
                            modifier = Modifier
                                .weight((1f - animatedCashFraction).coerceAtLeast(0.02f))
                                .height(6.dp)
                                .background(UpiIndigoAccent)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Cash and UPI Sub-breakdowns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cash Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("cash_total_pill")
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CashGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Cash",
                            tint = CashGreenAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Cash",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedCash,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // UPI Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("upi_total_pill")
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(UpiIndigo.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "UPI",
                            tint = UpiIndigoAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "UPI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedUpi,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

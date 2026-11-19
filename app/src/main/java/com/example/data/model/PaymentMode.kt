package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class PaymentMode(
    val name: String,
    val label: String,
    val iconName: String = "payments",
    val isCustom: Boolean = false,
    val colorHex: Long = 0xFF10B981
) {
    val color: Color get() = Color(colorHex)
    val icon: ImageVector get() = getIcon(iconName)

    companion object {
        val CASH = PaymentMode("CASH", "Cash", "payments", false, 0xFF10B981)
        val UPI = PaymentMode("UPI", "UPI", "qr_code", false, 0xFF6366F1)

        val DEFAULT_MODES = listOf(CASH, UPI)

        val AVAILABLE_ICONS = listOf(
            "payments" to Icons.Default.Payments,
            "qr_code" to Icons.Default.QrCode,
            "credit_card" to Icons.Default.CreditCard,
            "account_balance" to Icons.Default.AccountBalance,
            "wallet" to Icons.Default.AccountBalanceWallet,
            "receipt" to Icons.Default.Receipt
        )

        fun getIcon(iconName: String): ImageVector {
            return AVAILABLE_ICONS.firstOrNull { it.first == iconName }?.second ?: Icons.Default.Payments
        }

        fun valueOf(name: String): PaymentMode {
            return PaymentModeRegistry.getModeByName(name)
        }

        fun entries(): List<PaymentMode> {
            return PaymentModeRegistry.getAllModesDirect()
        }
    }
}

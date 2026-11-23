package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryItem(
    val id: String,
    val title: String,
    val colorHex: Long,
    val iconName: String = "other",
    val isCustom: Boolean = false,
    val isIncome: Boolean = false
) {
    val color: Color get() = Color(colorHex)
    val icon: ImageVector get() = getIcon(iconName)

    companion object {
        val AVAILABLE_ICONS = listOf(
            "fastfood" to Icons.Default.Fastfood,
            "restaurant" to Icons.Default.Restaurant,
            "local_cafe" to Icons.Default.LocalCafe,
            "shopping_cart" to Icons.Default.ShoppingCart,
            "shopping_bag" to Icons.Default.ShoppingBag,
            "directions_car" to Icons.Default.DirectionsCar,
            "flight" to Icons.Default.Flight,
            "electric_bolt" to Icons.Default.ElectricBolt,
            "home" to Icons.Default.Home,
            "movie" to Icons.Default.Movie,
            "fitness_center" to Icons.Default.FitnessCenter,
            "local_hospital" to Icons.Default.LocalHospital,
            "school" to Icons.Default.School,
            "work" to Icons.Default.Work,
            "savings" to Icons.Default.Savings,
            "pets" to Icons.Default.Pets,
            "card_giftcard" to Icons.Default.CardGiftcard,
            "trending_up" to Icons.Default.TrendingUp,
            "account_balance_wallet" to Icons.Default.AccountBalanceWallet,
            "payments" to Icons.Default.Payments,
            "attach_money" to Icons.Default.AttachMoney,
            "other" to Icons.Default.MoreHoriz
        )

        val PALETTE_COLORS = listOf(
            0xFFF59E0B, // Orange
            0xFF10B981, // Emerald Green
            0xFF3B82F6, // Blue
            0xFF8B5CF6, // Purple
            0xFFEAB308, // Yellow/Amber
            0xFFEC4899, // Pink
            0xFFEF4444, // Red
            0xFF06B6D4, // Cyan
            0xFF14B8A6, // Teal
            0xFF6366F1, // Indigo
            0xFF84CC16, // Lime
            0xFF64748B  // Slate
        )

        fun getIcon(name: String): ImageVector {
            return AVAILABLE_ICONS.firstOrNull { it.first == name }?.second ?: Icons.Default.MoreHoriz
        }

        val DEFAULT_CATEGORIES = listOf(
            CategoryItem("food", "Food & Dining", 0xFFF59E0B, "fastfood", isCustom = false),
            CategoryItem("groceries", "Groceries", 0xFF10B981, "shopping_cart", isCustom = false),
            CategoryItem("transport", "Travel & Fuel", 0xFF3B82F6, "directions_car", isCustom = false),
            CategoryItem("shopping", "Shopping", 0xFF8B5CF6, "shopping_bag", isCustom = false),
            CategoryItem("bills", "Bills & Utilities", 0xFFEAB308, "electric_bolt", isCustom = false),
            CategoryItem("entertainment", "Entertainment", 0xFFEC4899, "movie", isCustom = false),
            CategoryItem("health", "Health & Medical", 0xFFEF4444, "local_hospital", isCustom = false),
            CategoryItem("other", "Other", 0xFF64748B, "other", isCustom = false)
        )

        val INCOME_CATEGORIES = listOf(
            CategoryItem("inc_salary", "Salary", 0xFF16A34A, "work", isCustom = false, isIncome = true),
            CategoryItem("inc_business", "Business", 0xFF059669, "trending_up", isCustom = false, isIncome = true),
            CategoryItem("inc_freelance", "Freelance", 0xFF10B981, "account_balance_wallet", isCustom = false, isIncome = true),
            CategoryItem("inc_investment", "Investment", 0xFF0D9488, "savings", isCustom = false, isIncome = true),
            CategoryItem("inc_gift", "Gift & Bonus", 0xFF22C55E, "card_giftcard", isCustom = false, isIncome = true),
            CategoryItem("inc_refund", "Refund", 0xFF15803D, "payments", isCustom = false, isIncome = true),
            CategoryItem("inc_other", "Other Income", 0xFF16A34A, "attach_money", isCustom = false, isIncome = true)
        )

        val INCOME_DEFAULT: CategoryItem get() = INCOME_CATEGORIES[0]

        val FOOD: CategoryItem get() = DEFAULT_CATEGORIES[0]
        val GROCERIES: CategoryItem get() = DEFAULT_CATEGORIES[1]
        val TRANSPORT: CategoryItem get() = DEFAULT_CATEGORIES[2]
        val SHOPPING: CategoryItem get() = DEFAULT_CATEGORIES[3]
        val BILLS: CategoryItem get() = DEFAULT_CATEGORIES[4]
        val ENTERTAINMENT: CategoryItem get() = DEFAULT_CATEGORIES[5]
        val HEALTH: CategoryItem get() = DEFAULT_CATEGORIES[6]
        val OTHER: CategoryItem get() = DEFAULT_CATEGORIES[7]

        val entries: List<CategoryItem> get() = DEFAULT_CATEGORIES

        fun fromId(id: String?): CategoryItem {
            if (id.isNullOrBlank()) return OTHER
            return DEFAULT_CATEGORIES.firstOrNull { it.id.equals(id, ignoreCase = true) }
                ?: DEFAULT_CATEGORIES.firstOrNull { it.title.equals(id, ignoreCase = true) }
                ?: INCOME_CATEGORIES.firstOrNull { it.id.equals(id, ignoreCase = true) }
                ?: INCOME_CATEGORIES.firstOrNull { it.title.equals(id, ignoreCase = true) }
                ?: OTHER
        }
    }
}

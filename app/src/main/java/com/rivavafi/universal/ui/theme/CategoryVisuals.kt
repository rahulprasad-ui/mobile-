package com.rivavafi.universal.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryVisual(
    val title: String,
    val color: Color,
    val icon: ImageVector
)

object CategoryVisuals {
    val EXPENSE = CategoryVisual("Debit", Color(0xFFE53935), Icons.Outlined.TrendingDown)
    val INCOME = CategoryVisual("Credit", Color(0xFF4CAF50), Icons.Outlined.TrendingUp)
    val BILL = CategoryVisual("Bill", Color(0xFF9C27B0), Icons.Outlined.Receipt)
    val INVESTMENT = CategoryVisual("Investment", Color(0xFF00897B), Icons.Outlined.Savings)
    val SUBSCRIPTION = CategoryVisual("Subscription", Color(0xFF3949AB), Icons.Outlined.Autorenew)
    val REWARD = CategoryVisual("Reward", Color(0xFFFBC02D), Icons.Outlined.CardGiftcard)
    val SELF_TRANSFER = CategoryVisual("Transfer", Color(0xFF757575), Icons.Outlined.SyncAlt)
    val IGNORE = CategoryVisual("Ignore", Color(0xFF9E9E9E), Icons.Outlined.Block)
    val DEFAULT = CategoryVisual("Other", Color(0xFF546E7A), Icons.Outlined.HelpOutline)

    val subcategories = mapOf(
        "Food" to CategoryVisual("Food", Color(0xFFFF9800), Icons.Outlined.Restaurant),
        "Groceries" to CategoryVisual("Groceries", Color(0xFF4CAF50), Icons.Outlined.LocalGroceryStore),
        "Transport" to CategoryVisual("Transport", Color(0xFF2196F3), Icons.Outlined.DirectionsCar),
        "Bills" to CategoryVisual("Bills", Color(0xFF9C27B0), Icons.Outlined.ReceiptLong),
        "Shopping" to CategoryVisual("Shopping", Color(0xFFE91E63), Icons.Outlined.ShoppingBag),
        "Recharge" to CategoryVisual("Recharge", Color(0xFF00BCD4), Icons.Outlined.PhoneAndroid),
        "Entertainment" to CategoryVisual("Entertainment", Color(0xFFF44336), Icons.Outlined.Movie),
        "Travel" to CategoryVisual("Travel", Color(0xFF03A9F4), Icons.Outlined.Flight),
        "Loan Given" to CategoryVisual("Loan Given", Color(0xFF795548), Icons.Outlined.Handshake),
        "Loan Payment" to CategoryVisual("Loan Payment", Color(0xFF8D6E63), Icons.Outlined.Payments),
        "Health" to CategoryVisual("Health", Color(0xFFD32F2F), Icons.Outlined.MedicalServices),
        "Education" to CategoryVisual("Education", Color(0xFF3F51B5), Icons.Outlined.School),
        "Utilities" to CategoryVisual("Utilities", Color(0xFF673AB7), Icons.Outlined.Lightbulb),
        "Others" to CategoryVisual("Others", Color(0xFF607D8B), Icons.Outlined.MoreHoriz)
    )

    fun getCategoryVisual(category: String): CategoryVisual {
        return when (category.uppercase()) {
            "DEBIT" -> EXPENSE
            "CREDIT" -> INCOME
            "BILL", "BILL_PENDING" -> BILL
            "INVESTMENT" -> INVESTMENT
            "SUBSCRIPTION" -> SUBSCRIPTION
            "REWARD", "VOUCHER" -> REWARD
            "SELF_TRANSFER" -> SELF_TRANSFER
            "IGNORE" -> IGNORE
            else -> DEFAULT
        }
    }

    fun getSubcategoryVisual(subcategory: String): CategoryVisual {
        return subcategories[subcategory] ?: DEFAULT.copy(title = subcategory)
    }
}

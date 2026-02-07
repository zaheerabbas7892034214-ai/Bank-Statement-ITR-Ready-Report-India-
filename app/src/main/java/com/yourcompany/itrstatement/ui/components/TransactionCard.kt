package com.yourcompany.itrstatement.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourcompany.itrstatement.data.model.Category
import com.yourcompany.itrstatement.data.model.Transaction
import com.yourcompany.itrstatement.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionCard(
    transaction: Transaction,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isCredit = transaction.creditAmount > 0.0
    val isDebit = transaction.debitAmount > 0.0
    val amount = if (isCredit) transaction.creditAmount else transaction.debitAmount
    val amountColor = if (isCredit) Success else Error
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = getCategoryColor(transaction.category).copy(alpha = 0.1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(transaction.category),
                            contentDescription = transaction.category.displayName,
                            tint = getCategoryColor(transaction.category),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDate(transaction.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = getCategoryColor(transaction.category).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = transaction.category.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = getCategoryColor(transaction.category),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatCurrency(amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (isCredit) "Credit" else "Debit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getCategoryIcon(category: Category): ImageVector {
    return when (category) {
        Category.INCOME -> Icons.Default.TrendingUp
        Category.FOOD_DINING -> Icons.Default.Restaurant
        Category.SHOPPING -> Icons.Default.ShoppingCart
        Category.BILLS_UTILITIES -> Icons.Default.Receipt
        Category.TRAVEL -> Icons.Default.Flight
        Category.HEALTHCARE -> Icons.Default.LocalHospital
        Category.ENTERTAINMENT -> Icons.Default.Movie
        Category.TRANSFERS -> Icons.Default.SwapHoriz
        Category.TAXES -> Icons.Default.AccountBalance
        Category.UNCATEGORIZED -> Icons.Default.Category
    }
}

@Composable
private fun getCategoryColor(category: Category): androidx.compose.ui.graphics.Color {
    return when (category) {
        Category.INCOME -> CategoryIncome
        Category.FOOD_DINING -> CategoryFood
        Category.SHOPPING -> CategoryShopping
        Category.BILLS_UTILITIES -> CategoryBills
        Category.TRAVEL -> CategoryTravel
        Category.HEALTHCARE -> CategoryHealthcare
        Category.ENTERTAINMENT -> CategoryEntertainment
        Category.TRANSFERS -> CategoryTransfers
        Category.TAXES -> CategoryTaxes
        Category.UNCATEGORIZED -> CategoryUncategorized
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount)
}

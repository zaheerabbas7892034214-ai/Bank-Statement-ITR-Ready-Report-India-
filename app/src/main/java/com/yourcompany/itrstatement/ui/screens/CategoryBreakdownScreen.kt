package com.yourcompany.itrstatement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourcompany.itrstatement.ui.components.CategoryPieChart
import com.yourcompany.itrstatement.ui.viewmodel.DashboardViewModel
import com.yourcompany.itrstatement.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBreakdownScreen(
    viewModel: DashboardViewModel,
    sessionId: Long? = null,
    onNavigateToTransactionList: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            viewModel.loadDashboardData(sessionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Category Breakdown") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when (val state = uiState) {
            is DashboardViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is DashboardViewModel.UiState.Success -> {
                CategoryBreakdownContent(
                    categoryBreakdown = state.categoryBreakdown,
                    totalExpense = state.totalExpense,
                    onCategoryClick = { category ->
                        onNavigateToTransactionList(category.name)
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is DashboardViewModel.UiState.Empty -> {
                EmptyStateContent(
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is DashboardViewModel.UiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.refreshData() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownContent(
    categoryBreakdown: List<DashboardViewModel.CategorySummary>,
    totalExpense: Double,
    onCategoryClick: (DashboardViewModel.CategorySummary) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Expense Distribution",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (categoryBreakdown.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CategoryPieChart(
                            data = categoryBreakdown.map { summary ->
                                Pair(summary.category.displayName, summary.amount.toFloat())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Total Expenses",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(totalExpense),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(
                items = categoryBreakdown,
                key = { it.category }
            ) { summary ->
                CategorySummaryCard(
                    summary = summary,
                    onClick = { onCategoryClick(summary) }
                )
            }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expense data available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySummaryCard(
    summary: DashboardViewModel.CategorySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = getCategoryColor(summary.category).copy(alpha = 0.1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(summary.category),
                            contentDescription = summary.category.displayName,
                            tint = getCategoryColor(summary.category),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${summary.transactionCount} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = CurrencyFormatter.format(summary.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = getCategoryColor(summary.category).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "${String.format("%.1f", summary.percentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = getCategoryColor(summary.category),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun getCategoryIcon(category: com.yourcompany.itrstatement.data.model.Category): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        com.yourcompany.itrstatement.data.model.Category.INCOME -> Icons.Default.TrendingUp
        com.yourcompany.itrstatement.data.model.Category.FOOD_DINING -> Icons.Default.Restaurant
        com.yourcompany.itrstatement.data.model.Category.SHOPPING -> Icons.Default.ShoppingCart
        com.yourcompany.itrstatement.data.model.Category.BILLS_UTILITIES -> Icons.Default.Receipt
        com.yourcompany.itrstatement.data.model.Category.TRAVEL -> Icons.Default.Flight
        com.yourcompany.itrstatement.data.model.Category.HEALTHCARE -> Icons.Default.LocalHospital
        com.yourcompany.itrstatement.data.model.Category.ENTERTAINMENT -> Icons.Default.Movie
        com.yourcompany.itrstatement.data.model.Category.TRANSFERS -> Icons.Default.SwapHoriz
        com.yourcompany.itrstatement.data.model.Category.TAXES -> Icons.Default.AccountBalance
        com.yourcompany.itrstatement.data.model.Category.UNCATEGORIZED -> Icons.Default.Category
    }
}

@Composable
private fun getCategoryColor(category: com.yourcompany.itrstatement.data.model.Category): androidx.compose.ui.graphics.Color {
    return when (category) {
        com.yourcompany.itrstatement.data.model.Category.INCOME -> MaterialTheme.colorScheme.primary
        com.yourcompany.itrstatement.data.model.Category.FOOD_DINING -> MaterialTheme.colorScheme.tertiary
        com.yourcompany.itrstatement.data.model.Category.SHOPPING -> MaterialTheme.colorScheme.secondary
        com.yourcompany.itrstatement.data.model.Category.BILLS_UTILITIES -> MaterialTheme.colorScheme.error
        com.yourcompany.itrstatement.data.model.Category.TRAVEL -> MaterialTheme.colorScheme.primary
        com.yourcompany.itrstatement.data.model.Category.HEALTHCARE -> MaterialTheme.colorScheme.error
        com.yourcompany.itrstatement.data.model.Category.ENTERTAINMENT -> MaterialTheme.colorScheme.tertiary
        com.yourcompany.itrstatement.data.model.Category.TRANSFERS -> MaterialTheme.colorScheme.secondary
        com.yourcompany.itrstatement.data.model.Category.TAXES -> MaterialTheme.colorScheme.primary
        com.yourcompany.itrstatement.data.model.Category.UNCATEGORIZED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun EmptyStateContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PieChart,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Data Available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Import a bank statement to see category breakdown",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

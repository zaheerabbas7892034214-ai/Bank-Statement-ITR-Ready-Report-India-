package com.yourcompany.itrstatement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourcompany.itrstatement.data.model.Category
import com.yourcompany.itrstatement.ui.components.TransactionCard
import com.yourcompany.itrstatement.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    sessionId: Long? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            viewModel.loadTransactions(sessionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOption("Date (Newest)", TransactionViewModel.SortOrder.DATE_DESC) {
                            viewModel.sortTransactions(it)
                            showSortMenu = false
                        }
                        SortOption("Date (Oldest)", TransactionViewModel.SortOrder.DATE_ASC) {
                            viewModel.sortTransactions(it)
                            showSortMenu = false
                        }
                        SortOption("Amount (High to Low)", TransactionViewModel.SortOrder.AMOUNT_DESC) {
                            viewModel.sortTransactions(it)
                            showSortMenu = false
                        }
                        SortOption("Amount (Low to High)", TransactionViewModel.SortOrder.AMOUNT_ASC) {
                            viewModel.sortTransactions(it)
                            showSortMenu = false
                        }
                    }
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when (val state = uiState) {
            is TransactionViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is TransactionViewModel.UiState.Success -> {
                TransactionListContent(
                    state = state,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query ->
                        searchQuery = query
                        viewModel.searchTransactions(query)
                    },
                    onClearFilters = { viewModel.clearFilters() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is TransactionViewModel.UiState.Empty -> {
                EmptyStateContent(
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is TransactionViewModel.UiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.refreshTransactions() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    if (showFilterDialog && uiState is TransactionViewModel.UiState.Success) {
        FilterDialog(
            selectedCategories = (uiState as TransactionViewModel.UiState.Success).filterOptions.selectedCategories,
            onDismiss = { showFilterDialog = false },
            onApply = { categories ->
                viewModel.filterByCategories(categories)
                showFilterDialog = false
            }
        )
    }
}

@Composable
private fun SortOption(
    text: String,
    sortOrder: TransactionViewModel.SortOrder,
    onClick: (TransactionViewModel.SortOrder) -> Unit
) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = { onClick(sortOrder) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionListContent(
    state: TransactionViewModel.UiState.Success,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (state.filterOptions.selectedCategories.isNotEmpty()) {
            CategoryFilterChips(
                selectedCategories = state.filterOptions.selectedCategories,
                onClearFilters = onClearFilters,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${state.filteredCount} of ${state.totalCount} transactions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No transactions found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onClearFilters) {
                        Text("Clear Filters")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = state.filteredTransactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionCard(transaction = transaction)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search transactions...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true
    )
}

@Composable
private fun CategoryFilterChips(
    selectedCategories: Set<Category>,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(selectedCategories.toList()) { category ->
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text(category.displayName) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        item {
            AssistChip(
                onClick = onClearFilters,
                label = { Text("Clear All") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun FilterDialog(
    selectedCategories: Set<Category>,
    onDismiss: () -> Unit,
    onApply: (Set<Category>) -> Unit
) {
    var tempSelectedCategories by remember { mutableStateOf(selectedCategories) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by Category") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Category.values()) { category ->
                    FilterChip(
                        selected = tempSelectedCategories.contains(category),
                        onClick = {
                            tempSelectedCategories = if (tempSelectedCategories.contains(category)) {
                                tempSelectedCategories - category
                            } else {
                                tempSelectedCategories + category
                            }
                        },
                        label = { Text(category.displayName) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(tempSelectedCategories) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Transactions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Import a bank statement to see transactions",
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

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
import com.yourcompany.itrstatement.data.model.ImportSession
import com.yourcompany.itrstatement.data.model.ParsingStatus
import com.yourcompany.itrstatement.ui.components.ProBadge
import com.yourcompany.itrstatement.ui.viewmodel.HomeViewModel
import com.yourcompany.itrstatement.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToImport: () -> Unit,
    onNavigateToDashboard: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val navigationEvent by viewModel.navigationEvent.collectAsState()

    LaunchedEffect(navigationEvent) {
        when (val event = navigationEvent) {
            is HomeViewModel.NavigationEvent.NavigateToImport -> onNavigateToImport()
            is HomeViewModel.NavigationEvent.NavigateToDashboard -> onNavigateToDashboard(event.sessionId)
            is HomeViewModel.NavigationEvent.NavigateToBilling -> onNavigateToPaywall()
            null -> {}
        }
        if (navigationEvent != null) {
            viewModel.clearNavigationEvent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ITR Statement") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.navigateToImport() },
                icon = { Icon(Icons.Default.Upload, contentDescription = "Import") },
                text = { Text("Import PDF") }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when (val state = uiState) {
            is HomeViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is HomeViewModel.UiState.Success -> {
                if (state.importHistory.isEmpty()) {
                    EmptyStateContent(
                        onImportClick = { viewModel.navigateToImport() },
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    HomeContent(
                        importHistory = state.importHistory,
                        isProActive = state.isProActive,
                        onSessionClick = { viewModel.navigateToDashboard(it) },
                        onDeleteSession = { viewModel.deleteImportHistory(it) },
                        onUpgradeClick = { viewModel.navigateToBilling() },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
            is HomeViewModel.UiState.Error -> {
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
private fun HomeContent(
    importHistory: List<ImportSession>,
    isProActive: Boolean,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isProActive) {
            item {
                ProCtaBanner(onUpgradeClick = onUpgradeClick)
            }
        }

        item {
            Text(
                text = "Import History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(
            items = importHistory,
            key = { it.id }
        ) { session ->
            ImportHistoryCard(
                session = session,
                onClick = { onSessionClick(session.id) },
                onDelete = { onDeleteSession(session.id) }
            )
        }
    }
}

@Composable
private fun ProCtaBanner(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onUpgradeClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Upgrade to Pro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ProBadge()
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Unlock unlimited exports & advanced features",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Upgrade",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportHistoryCard(
    session: ImportSession,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Import") },
            text = { Text("Are you sure you want to delete this import? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateUtils.formatForDisplay(session.importTimestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${session.transactionCount} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (session.parsingStatus != ParsingStatus.SUCCESS) {
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusChip(status = session.parsingStatus)
                }
            }
            
            Row {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                IconButton(onClick = onClick) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "View")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: ParsingStatus) {
    val (text, color) = when (status) {
        ParsingStatus.SUCCESS -> "Success" to MaterialTheme.colorScheme.primary
        ParsingStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
        ParsingStatus.PARTIAL -> "Partial" to MaterialTheme.colorScheme.tertiary
        ParsingStatus.IN_PROGRESS -> "In Progress" to MaterialTheme.colorScheme.secondary
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyStateContent(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Imports Yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Import your bank statement PDF to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onImportClick) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import PDF")
            }
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

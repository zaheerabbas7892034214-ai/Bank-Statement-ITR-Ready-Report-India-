package com.yourcompany.itrstatement.ui.screens

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourcompany.itrstatement.domain.usecase.ExportDataUseCase
import com.yourcompany.itrstatement.ui.components.ProBadge
import com.yourcompany.itrstatement.ui.viewmodel.ExportViewModel
import com.yourcompany.itrstatement.utils.CurrencyFormatter
import com.yourcompany.itrstatement.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exportState by viewModel.exportState.collectAsState()
    val previewState by viewModel.previewState.collectAsState()
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf<ExportDataUseCase.ExportFormat?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        uri?.let {
            if (selectedFormat != null && previewState is ExportViewModel.PreviewState.Available) {
                val transactions = (previewState as ExportViewModel.PreviewState.Available).previewTransactions
                viewModel.exportToFile(context, transactions, selectedFormat!!, it)
            }
        }
    }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportViewModel.ExportState.ProRequired -> {
                onNavigateToPaywall()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Data") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when (val state = exportState) {
            is ExportViewModel.ExportState.Idle,
            is ExportViewModel.ExportState.CheckingEntitlement -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ExportViewModel.ExportState.Ready -> {
                ExportContent(
                    isProActive = state.isProActive,
                    availableFormats = state.availableFormats,
                    previewState = previewState,
                    onFormatSelected = { format ->
                        selectedFormat = format
                        val extension = viewModel.getFormatExtension(format)
                        createDocumentLauncher.launch("transactions.$extension")
                    },
                    onUpgradeClick = onNavigateToPaywall,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is ExportViewModel.ExportState.Exporting -> {
                ExportingContent(
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is ExportViewModel.ExportState.Success -> {
                SuccessContent(
                    fileName = state.fileName,
                    onDone = onNavigateBack,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is ExportViewModel.ExportState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.refreshEntitlementStatus() },
                    onCancel = onNavigateBack,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is ExportViewModel.ExportState.ProRequired -> {
                ProRequiredContent(
                    message = state.message,
                    onUpgrade = onNavigateToPaywall,
                    onCancel = onNavigateBack,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun ExportContent(
    isProActive: Boolean,
    availableFormats: List<ExportDataUseCase.ExportFormat>,
    previewState: ExportViewModel.PreviewState,
    onFormatSelected: (ExportDataUseCase.ExportFormat) -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isProActive) {
            item {
                ProUpgradeBanner(onUpgradeClick = onUpgradeClick)
            }
        }

        item {
            Text(
                text = "Export Formats",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isProActive) {
                    "Choose a format to export your transaction data"
                } else {
                    "Export to CSV is available. Upgrade to Pro for all formats"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(ExportDataUseCase.ExportFormat.values()) { format ->
            val isAvailable = availableFormats.contains(format)
            ExportFormatCard(
                format = format,
                isAvailable = isAvailable,
                isProActive = isProActive,
                onClick = {
                    if (isAvailable) {
                        onFormatSelected(format)
                    } else {
                        onUpgradeClick()
                    }
                }
            )
        }

        if (previewState is ExportViewModel.PreviewState.Available) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                PreviewSection(
                    previewState = previewState,
                    isProActive = isProActive,
                    onUpgradeClick = onUpgradeClick
                )
            }
        }
    }
}

@Composable
private fun ProUpgradeBanner(
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
                        text = "Unlock All Export Formats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ProBadge()
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Export to XLSX, JSON & PDF with Pro",
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

@Composable
private fun ExportFormatCard(
    format: ExportDataUseCase.ExportFormat,
    isAvailable: Boolean,
    isProActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, title, description) = when (format) {
        ExportDataUseCase.ExportFormat.CSV -> Triple(
            Icons.Default.TableChart,
            "CSV (Comma Separated)",
            "Simple format compatible with Excel & Google Sheets"
        )
        ExportDataUseCase.ExportFormat.XLSX -> Triple(
            Icons.Default.InsertDriveFile,
            "Excel (XLSX)",
            "Full-featured Excel workbook with formatting"
        )
        ExportDataUseCase.ExportFormat.JSON -> Triple(
            Icons.Default.Code,
            "JSON",
            "Structured data format for developers & APIs"
        )
        ExportDataUseCase.ExportFormat.PDF -> Triple(
            Icons.Default.PictureAsPdf,
            "PDF Report",
            "Professional formatted report for printing"
        )
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = isAvailable
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
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = if (isAvailable) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isAvailable) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (!isAvailable && !isProActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Pro Only",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Export",
                tint = if (isAvailable) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun PreviewSection(
    previewState: ExportViewModel.PreviewState.Available,
    isProActive: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Data Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (previewState.isLimited) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "First 30 rows",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Total ${previewState.totalCount} transactions available for export",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (previewState.isLimited && !isProActive) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Upgrade to Pro to export all transactions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
            TextButton(onClick = onUpgradeClick) {
                Text("Upgrade Now")
            }
        }
    }
}

@Composable
private fun ExportingContent(
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
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Exporting...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please wait while we prepare your export",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuccessContent(
    fileName: String,
    onDone: () -> Unit,
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
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Export Successful!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "File saved: $fileName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onDone) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun ProRequiredContent(
    message: String,
    onUpgrade: () -> Unit,
    onCancel: () -> Unit,
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
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Pro Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                ProBadge()
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Button(onClick = onUpgrade) {
                    Text("Upgrade to Pro")
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
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
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Export Failed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Button(onClick = onRetry) {
                    Text("Try Again")
                }
            }
        }
    }
}

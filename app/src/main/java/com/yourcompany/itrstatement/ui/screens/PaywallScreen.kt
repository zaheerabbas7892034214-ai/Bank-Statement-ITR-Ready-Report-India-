package com.yourcompany.itrstatement.ui.screens

import android.app.Activity
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
import com.yourcompany.itrstatement.ui.components.ProBadge
import com.yourcompany.itrstatement.ui.viewmodel.BillingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    viewModel: BillingViewModel,
    onNavigateBack: () -> Unit,
    onSubscriptionSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is BillingViewModel.UiState.PurchaseSuccess) {
            onSubscriptionSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade to Pro") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when (val state = uiState) {
            is BillingViewModel.UiState.Idle,
            is BillingViewModel.UiState.Loading,
            is BillingViewModel.UiState.Connecting -> {
                LoadingContent(modifier = Modifier.padding(paddingValues))
            }
            is BillingViewModel.UiState.Ready -> {
                PaywallContent(
                    productInfo = state.productInfo,
                    isSubscribed = state.isSubscribed,
                    subscriptionDetails = state.subscriptionDetails,
                    onSubscribeClick = {
                        viewModel.launchPurchaseFlow(context as Activity)
                    },
                    onRestoreClick = { viewModel.restorePurchases() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is BillingViewModel.UiState.Purchasing -> {
                PurchasingContent(modifier = Modifier.padding(paddingValues))
            }
            is BillingViewModel.UiState.PurchaseSuccess -> {
                SuccessContent(
                    message = state.message,
                    onDone = onNavigateBack,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is BillingViewModel.UiState.PurchaseError -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.refreshBillingState() },
                    onCancel = onNavigateBack,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is BillingViewModel.UiState.ProductNotAvailable -> {
                ProductNotAvailableContent(
                    onRetry = { viewModel.refreshBillingState() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is BillingViewModel.UiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.refreshBillingState() },
                    onCancel = onNavigateBack,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(
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
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun PaywallContent(
    productInfo: BillingViewModel.ProductInfo?,
    isSubscribed: Boolean,
    subscriptionDetails: BillingViewModel.SubscriptionDetails?,
    onSubscribeClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                ProBadge(modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ITR Statement Pro",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Unlock all premium features",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Divider()
        }

        item {
            FeatureList()
        }

        if (!isSubscribed && productInfo != null) {
            item {
                Divider()
            }

            item {
                PricingCard(productInfo = productInfo)
            }

            item {
                Button(
                    onClick = onSubscribeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subscribe Now", style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                TextButton(
                    onClick = onRestoreClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restore Purchases")
                }
            }

            item {
                LegalLinks()
            }
        } else if (isSubscribed && subscriptionDetails != null) {
            item {
                AlreadySubscribedCard(subscriptionDetails = subscriptionDetails)
            }
        }
    }
}

@Composable
private fun FeatureList(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Premium Features",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        FeatureItem(
            icon = Icons.Default.TableChart,
            title = "Export to Excel (XLSX)",
            description = "Export data in full-featured Excel format"
        )
        
        FeatureItem(
            icon = Icons.Default.Code,
            title = "Export to JSON",
            description = "Structured data export for developers"
        )
        
        FeatureItem(
            icon = Icons.Default.PictureAsPdf,
            title = "Professional PDF Reports",
            description = "Generate formatted PDF reports"
        )
        
        FeatureItem(
            icon = Icons.Default.CloudSync,
            title = "Cloud Backup",
            description = "Automatic backup of your transaction data"
        )
        
        FeatureItem(
            icon = Icons.Default.Analytics,
            title = "Advanced Analytics",
            description = "Deep insights and custom reports"
        )
        
        FeatureItem(
            icon = Icons.Default.SupportAgent,
            title = "Priority Support",
            description = "Get help faster with premium support"
        )
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PricingCard(
    productInfo: BillingViewModel.ProductInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = productInfo.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = productInfo.price,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "per year",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            if (productInfo.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = productInfo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun AlreadySubscribedCard(
    subscriptionDetails: BillingViewModel.SubscriptionDetails,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "You're a Pro Member!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (subscriptionDetails.isActive && subscriptionDetails.expiryTimestamp != null) {
                val daysRemaining = subscriptionDetails.daysUntilExpiry ?: 0
                Text(
                    text = if (daysRemaining > 0) {
                        "Your subscription expires in $daysRemaining days"
                    } else {
                        "Your subscription is active"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            if (subscriptionDetails.isInGracePeriod) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Payment issue - Please update payment method",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LegalLinks(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = { /* Open Terms */ }) {
                Text(
                    "Terms of Service",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text("•", style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = { /* Open Privacy */ }) {
                Text(
                    "Privacy Policy",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        Text(
            text = "Subscription renews automatically. Cancel anytime.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PurchasingContent(
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
                text = "Processing Purchase...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please wait while we complete your subscription",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuccessContent(
    message: String,
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
                text = "Welcome to Pro!",
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
            Button(onClick = onDone) {
                Text("Get Started")
            }
        }
    }
}

@Composable
private fun ProductNotAvailableContent(
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
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Product Not Available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unable to load subscription details. Please try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRetry) {
                Text("Retry")
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
                text = "Error",
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
                    Text("Retry")
                }
            }
        }
    }
}

package com.yourcompany.itrstatement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourcompany.itrstatement.BuildConfig
import com.yourcompany.itrstatement.ui.viewmodel.BillingViewModel
import com.yourcompany.itrstatement.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    billingViewModel: BillingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val billingState by billingViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        SettingsContent(
            billingState = billingState,
            onRestorePurchases = { billingViewModel.restorePurchases() },
            onNavigateToPaywall = onNavigateToPaywall,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun SettingsContent(
    billingState: BillingViewModel.UiState,
    onRestorePurchases: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Subscription",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            when (billingState) {
                is BillingViewModel.UiState.Ready -> {
                    SubscriptionStatusCard(
                        isSubscribed = billingState.isSubscribed,
                        subscriptionDetails = billingState.subscriptionDetails,
                        onUpgradeClick = if (!billingState.isSubscribed) onNavigateToPaywall else null
                    )
                }
                is BillingViewModel.UiState.Loading -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                else -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Unable to load subscription status",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsItem(
                icon = Icons.Default.Restore,
                title = "Restore Purchases",
                subtitle = "Restore your previous purchases",
                onClick = onRestorePurchases
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.DeleteSweep,
                title = "Clear Cache",
                subtitle = "Free up storage space",
                onClick = { /* Implement clear cache */ }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Info,
                title = "App Version",
                subtitle = getAppVersion(),
                onClick = null
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Description,
                title = "About ITR Statement",
                subtitle = "Bank statement analysis for Indian taxpayers",
                onClick = { /* Open about dialog */ }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Text(
                text = "Legal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Gavel,
                title = "Terms of Service",
                subtitle = "View our terms of service",
                onClick = { /* Open Terms */ }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                subtitle = "View our privacy policy",
                onClick = { /* Open Privacy */ }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Default.Security,
                title = "Data & Security",
                subtitle = "Learn how we protect your data",
                onClick = { /* Open Security info */ }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                text = "© 2024 ITR Statement. All rights reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SubscriptionStatusCard(
    isSubscribed: Boolean,
    subscriptionDetails: BillingViewModel.SubscriptionDetails?,
    onUpgradeClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = if (isSubscribed) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
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
                    Icon(
                        imageVector = if (isSubscribed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (isSubscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isSubscribed) "Pro Active" else "Free Version",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (isSubscribed && subscriptionDetails != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (subscriptionDetails.expiryTimestamp != null) {
                        val expiryDate = DateUtils.formatForDisplay(subscriptionDetails.expiryTimestamp)
                        Text(
                            text = "Expires on: $expiryDate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSubscribed) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        
                        if (subscriptionDetails.daysUntilExpiry != null && subscriptionDetails.daysUntilExpiry > 0) {
                            Text(
                                text = "${subscriptionDetails.daysUntilExpiry} days remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSubscribed) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                    
                    if (subscriptionDetails.isInGracePeriod) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Payment issue - Please update payment method",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Upgrade to unlock all features",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (!isSubscribed && onUpgradeClick != null) {
                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Upgrade")
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth()
        ) {
            SettingsItemContent(
                icon = icon,
                title = title,
                subtitle = subtitle,
                hasAction = true
            )
        }
    } else {
        Card(modifier = modifier.fillMaxWidth()) {
            SettingsItemContent(
                icon = icon,
                title = title,
                subtitle = subtitle,
                hasAction = false
            )
        }
    }
}

@Composable
private fun SettingsItemContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    hasAction: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
                modifier = Modifier.size(40.dp),
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
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (hasAction) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getAppVersion(): String {
    return try {
        "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    } catch (e: Exception) {
        "Version 1.0.0"
    }
}

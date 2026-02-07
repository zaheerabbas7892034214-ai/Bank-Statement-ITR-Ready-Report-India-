package com.yourcompany.itrstatement.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourcompany.itrstatement.ui.theme.ProGold

@Composable
fun ProBadge(
    modifier: Modifier = Modifier,
    size: ProBadgeSize = ProBadgeSize.Medium,
    showIcon: Boolean = true,
    shimmerEnabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    
    val gradientColors = listOf(
        Color(0xFFFFD700),
        Color(0xFFFFE44D),
        Color(0xFFFFD700),
        Color(0xFFFFA500),
        Color(0xFFFFD700)
    )
    
    val brush = if (shimmerEnabled) {
        Brush.linearGradient(
            colors = gradientColors,
            start = Offset(shimmerOffset - 500f, shimmerOffset - 500f),
            end = Offset(shimmerOffset, shimmerOffset)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
        )
    }
    
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(brush)
                .padding(
                    horizontal = size.paddingHorizontal,
                    vertical = size.paddingVertical
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (showIcon) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "PRO",
                        tint = Color(0xFF8B4513),
                        modifier = Modifier.size(size.iconSize)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                Text(
                    text = "PRO",
                    fontSize = size.fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B4513),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ProBadgeInline(
    modifier: Modifier = Modifier,
    shimmerEnabled: Boolean = false
) {
    ProBadge(
        modifier = modifier,
        size = ProBadgeSize.Small,
        showIcon = false,
        shimmerEnabled = shimmerEnabled
    )
}

@Composable
fun ProBadgeStandalone(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF9E6)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProBadge(
                    size = ProBadgeSize.Large,
                    shimmerEnabled = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Premium Features",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Unlock advanced analytics and exports",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

sealed class ProBadgeSize(
    val fontSize: TextUnit,
    val iconSize: Dp,
    val paddingHorizontal: Dp,
    val paddingVertical: Dp
) {
    data object Small : ProBadgeSize(
        fontSize = 10.sp,
        iconSize = 10.dp,
        paddingHorizontal = 6.dp,
        paddingVertical = 2.dp
    )
    
    data object Medium : ProBadgeSize(
        fontSize = 12.sp,
        iconSize = 14.dp,
        paddingHorizontal = 8.dp,
        paddingVertical = 4.dp
    )
    
    data object Large : ProBadgeSize(
        fontSize = 16.sp,
        iconSize = 20.dp,
        paddingHorizontal = 12.dp,
        paddingVertical = 6.dp
    )
}

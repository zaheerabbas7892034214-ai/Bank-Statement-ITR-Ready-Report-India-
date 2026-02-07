package com.yourcompany.itrstatement.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourcompany.itrstatement.data.model.Category
import com.yourcompany.itrstatement.ui.theme.*
import java.text.NumberFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class CategoryData(
    val category: Category,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

@Composable
fun CategoryPieChart(
    categoryAmounts: Map<Category, Double>,
    modifier: Modifier = Modifier,
    onCategoryClick: ((Category) -> Unit)? = null
) {
    if (categoryAmounts.isEmpty()) {
        EmptyPieChartState(modifier = modifier)
        return
    }
    
    val total = categoryAmounts.values.sum()
    if (total <= 0.0) {
        EmptyPieChartState(modifier = modifier)
        return
    }
    
    val categoryDataList = categoryAmounts.map { (category, amount) ->
        CategoryData(
            category = category,
            amount = amount,
            percentage = (amount / total * 100).toFloat(),
            color = getCategoryChartColor(category)
        )
    }.sortedByDescending { it.amount }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PieChartCanvas(
            categoryDataList = categoryDataList,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(32.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        CategoryLegend(
            categoryDataList = categoryDataList,
            onCategoryClick = onCategoryClick
        )
    }
}

@Composable
private fun PieChartCanvas(
    categoryDataList: List<CategoryData>,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface.hashCode()
    
    Canvas(modifier = modifier) {
        val canvasSize = min(size.width, size.height)
        val radius = canvasSize / 2f * 0.8f
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val strokeWidth = radius * 0.3f
        
        var startAngle = -90f
        
        categoryDataList.forEach { data ->
            val sweepAngle = data.percentage * 3.6f
            
            drawArc(
                color = data.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    centerX - radius,
                    centerY - radius
                ),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
            
            if (data.percentage >= 5f) {
                val angle = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                val labelRadius = radius * 0.65f
                val labelX = centerX + (labelRadius * cos(angle)).toFloat()
                val labelY = centerY + (labelRadius * sin(angle)).toFloat()
                
                val percentText = "${data.percentage.toInt()}%"
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = textColor
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    drawText(percentText, labelX, labelY + 10f, paint)
                }
            }
            
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun CategoryLegend(
    categoryDataList: List<CategoryData>,
    onCategoryClick: ((Category) -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Category Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            categoryDataList.forEach { data ->
                LegendItem(
                    categoryData = data,
                    onClick = { onCategoryClick?.invoke(data.category) }
                )
                if (data != categoryDataList.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    categoryData: CategoryData,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier.size(16.dp),
                    shape = MaterialTheme.shapes.small,
                    color = categoryData.color
                ) {}
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = categoryData.category.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${categoryData.percentage.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = formatCurrency(categoryData.amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyPieChartState(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun getCategoryChartColor(category: Category): Color {
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

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount)
}

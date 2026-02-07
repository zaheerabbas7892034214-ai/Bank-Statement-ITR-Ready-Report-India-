package com.yourcompany.itrstatement.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourcompany.itrstatement.ui.theme.*
import java.text.NumberFormat
import java.util.*
import kotlin.math.max

data class MonthData(
    val month: String,
    val monthShort: String,
    val income: Double,
    val expense: Double
)

@Composable
fun MonthlyBarChart(
    monthlyData: List<MonthData>,
    modifier: Modifier = Modifier
) {
    if (monthlyData.isEmpty()) {
        EmptyBarChartState(modifier = modifier)
        return
    }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            ChartLegendItem(
                color = CategoryIncome,
                label = "Income"
            )
            
            Spacer(modifier = Modifier.width(24.dp))
            
            ChartLegendItem(
                color = CategoryExpense,
                label = "Expense"
            )
        }
        
        BarChartCanvas(
            monthlyData = monthlyData,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp)
        )
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = MaterialTheme.shapes.small,
            color = color
        ) {}
        
        Spacer(modifier = Modifier.width(6.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BarChartCanvas(
    monthlyData: List<MonthData>,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface.hashCode()
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    
    Canvas(modifier = modifier) {
        val chartHeight = size.height * 0.7f
        val chartWidth = size.width
        val bottomPadding = size.height * 0.25f
        val leftPadding = 60f
        val rightPadding = 20f
        val topPadding = 40f
        
        val availableWidth = chartWidth - leftPadding - rightPadding
        val barGroupWidth = availableWidth / monthlyData.size
        val barWidth = (barGroupWidth * 0.35f).coerceAtMost(40f)
        val barSpacing = barWidth * 0.3f
        
        val maxValue = monthlyData.maxOfOrNull { 
            max(it.income, it.expense) 
        } ?: 1.0
        
        val yAxisMax = ((maxValue * 1.2) / 10000).toInt() * 10000 + 10000
        val gridLines = 5
        val gridStep = yAxisMax / gridLines
        
        for (i in 0..gridLines) {
            val y = topPadding + (chartHeight * (1 - i.toFloat() / gridLines))
            
            drawLine(
                color = gridColor,
                start = Offset(leftPadding, y),
                end = Offset(chartWidth - rightPadding, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            
            val value = (gridStep * i).toDouble()
            val label = if (value >= 1000) {
                "${(value / 1000).toInt()}K"
            } else {
                value.toInt().toString()
            }
            
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = textColor
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                drawText(label, leftPadding - 10f, y + 8f, paint)
            }
        }
        
        monthlyData.forEachIndexed { index, data ->
            val centerX = leftPadding + (barGroupWidth * index) + (barGroupWidth / 2)
            
            val incomeHeight = ((data.income / yAxisMax) * chartHeight).toFloat()
            val expenseHeight = ((data.expense / yAxisMax) * chartHeight).toFloat()
            
            val incomeBarX = centerX - barWidth - (barSpacing / 2)
            val incomeBarY = topPadding + chartHeight - incomeHeight
            drawRect(
                color = CategoryIncome,
                topLeft = Offset(incomeBarX, incomeBarY),
                size = Size(barWidth, incomeHeight)
            )
            
            val expenseBarX = centerX + (barSpacing / 2)
            val expenseBarY = topPadding + chartHeight - expenseHeight
            drawRect(
                color = CategoryExpense,
                topLeft = Offset(expenseBarX, expenseBarY),
                size = Size(barWidth, expenseHeight)
            )
            
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = textColor
                    textSize = 26f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = false
                }
                val labelY = topPadding + chartHeight + 30f
                drawText(data.monthShort, centerX, labelY, paint)
            }
        }
        
        drawLine(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            start = Offset(leftPadding, topPadding),
            end = Offset(leftPadding, topPadding + chartHeight),
            strokeWidth = 2f
        )
        
        drawLine(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            start = Offset(leftPadding, topPadding + chartHeight),
            end = Offset(chartWidth - rightPadding, topPadding + chartHeight),
            strokeWidth = 2f
        )
    }
}

@Composable
private fun EmptyBarChartState(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No monthly data available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun createMonthData(month: String, income: Double, expense: Double): MonthData {
    val monthShort = when (month) {
        "January" -> "Jan"
        "February" -> "Feb"
        "March" -> "Mar"
        "April" -> "Apr"
        "May" -> "May"
        "June" -> "Jun"
        "July" -> "Jul"
        "August" -> "Aug"
        "September" -> "Sep"
        "October" -> "Oct"
        "November" -> "Nov"
        "December" -> "Dec"
        else -> month.take(3)
    }
    return MonthData(month, monthShort, income, expense)
}

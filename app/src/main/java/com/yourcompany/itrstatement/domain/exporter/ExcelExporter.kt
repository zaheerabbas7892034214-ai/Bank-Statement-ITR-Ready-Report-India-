package com.yourcompany.itrstatement.domain.exporter

import com.yourcompany.itrstatement.data.model.Category
import com.yourcompany.itrstatement.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExcelExporter {
    
    sealed class Result {
        data class Success(val fileName: String) : Result()
        data class Error(val message: String, val exception: Throwable? = null) : Result()
    }
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
    
    suspend fun exportToExcel(
        transactions: List<Transaction>,
        outputStream: OutputStream
    ): Result = withContext(Dispatchers.IO) {
        if (transactions.isEmpty()) {
            return@withContext Result.Error("No transactions to export")
        }
        
        try {
            val workbook = XSSFWorkbook()
            
            createSummarySheet(workbook, transactions)
            createTransactionsSheet(workbook, transactions)
            createCategoryBreakdownSheet(workbook, transactions)
            
            workbook.write(outputStream)
            workbook.close()
            
            val fileName = "transactions_${timestampFormat.format(Date())}.xlsx"
            Result.Success(fileName)
        } catch (e: Exception) {
            Result.Error("Failed to export Excel: ${e.message}", e)
        }
    }
    
    private fun createSummarySheet(workbook: Workbook, transactions: List<Transaction>) {
        val sheet = workbook.createSheet("Summary")
        
        val headerStyle = createHeaderStyle(workbook)
        val currencyStyle = createCurrencyStyle(workbook)
        
        var rowNum = 0
        
        val titleRow = sheet.createRow(rowNum++)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("Bank Statement ITR Report")
        titleCell.cellStyle = createTitleStyle(workbook)
        
        val dateRow = sheet.createRow(rowNum++)
        dateRow.createCell(0).setCellValue("Generated on:")
        dateRow.createCell(1).setCellValue(
            SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
        )
        
        rowNum++
        
        val totalDebit = transactions.sumOf { it.debitAmount }
        val totalCredit = transactions.sumOf { it.creditAmount }
        
        createSummaryRow(sheet, rowNum++, "Total Transactions", transactions.size.toDouble(), false)
        createSummaryRow(sheet, rowNum++, "Total Debits", totalDebit, true, currencyStyle)
        createSummaryRow(sheet, rowNum++, "Total Credits", totalCredit, true, currencyStyle)
        createSummaryRow(sheet, rowNum++, "Net Amount", totalCredit - totalDebit, true, currencyStyle)
        
        rowNum++
        
        val dateRange = if (transactions.isNotEmpty()) {
            val minDate = transactions.minOf { it.date }
            val maxDate = transactions.maxOf { it.date }
            "${dateFormat.format(Date(minDate))} to ${dateFormat.format(Date(maxDate))}"
        } else {
            "N/A"
        }
        
        val rangeRow = sheet.createRow(rowNum++)
        rangeRow.createCell(0).setCellValue("Date Range:")
        rangeRow.createCell(1).setCellValue(dateRange)
        
        sheet.setColumnWidth(0, 6000)
        sheet.setColumnWidth(1, 4000)
    }
    
    private fun createSummaryRow(
        sheet: Sheet,
        rowNum: Int,
        label: String,
        value: Double,
        isCurrency: Boolean = false,
        style: CellStyle? = null
    ) {
        val row = sheet.createRow(rowNum)
        row.createCell(0).setCellValue(label)
        val valueCell = row.createCell(1)
        valueCell.setCellValue(value)
        if (style != null) {
            valueCell.cellStyle = style
        }
    }
    
    private fun createTransactionsSheet(workbook: Workbook, transactions: List<Transaction>) {
        val sheet = workbook.createSheet("Transactions")
        
        val headerStyle = createHeaderStyle(workbook)
        val dateStyle = createDateStyle(workbook)
        val currencyStyle = createCurrencyStyle(workbook)
        val normalStyle = createNormalStyle(workbook)
        
        val headerRow = sheet.createRow(0)
        val headers = listOf("Date", "Description", "Category", "Debit", "Credit", "Balance")
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        
        transactions.forEachIndexed { index, transaction ->
            val row = sheet.createRow(index + 1)
            
            val dateCell = row.createCell(0)
            dateCell.setCellValue(dateFormat.format(Date(transaction.date)))
            dateCell.cellStyle = dateStyle
            
            val descCell = row.createCell(1)
            descCell.setCellValue(transaction.description)
            descCell.cellStyle = normalStyle
            
            val categoryCell = row.createCell(2)
            categoryCell.setCellValue(transaction.category.displayName)
            categoryCell.cellStyle = normalStyle
            
            val debitCell = row.createCell(3)
            debitCell.setCellValue(transaction.debitAmount)
            debitCell.cellStyle = currencyStyle
            
            val creditCell = row.createCell(4)
            creditCell.setCellValue(transaction.creditAmount)
            creditCell.cellStyle = currencyStyle
            
            val balanceCell = row.createCell(5)
            balanceCell.setCellValue(transaction.balance)
            balanceCell.cellStyle = currencyStyle
        }
        
        sheet.setColumnWidth(0, 3500)
        sheet.setColumnWidth(1, 10000)
        sheet.setColumnWidth(2, 5000)
        sheet.setColumnWidth(3, 3500)
        sheet.setColumnWidth(4, 3500)
        sheet.setColumnWidth(5, 3500)
        
        sheet.createFreezePane(0, 1)
    }
    
    private fun createCategoryBreakdownSheet(workbook: Workbook, transactions: List<Transaction>) {
        val sheet = workbook.createSheet("Category Breakdown")
        
        val headerStyle = createHeaderStyle(workbook)
        val currencyStyle = createCurrencyStyle(workbook)
        val normalStyle = createNormalStyle(workbook)
        
        val headerRow = sheet.createRow(0)
        val headers = listOf("Category", "Count", "Total Debit", "Total Credit", "Net Amount")
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        
        val categoryMap = transactions.groupBy { it.category }
        val categoryStats = categoryMap.map { (category, txns) ->
            CategoryStat(
                category = category,
                count = txns.size,
                totalDebit = txns.sumOf { it.debitAmount },
                totalCredit = txns.sumOf { it.creditAmount }
            )
        }.sortedByDescending { it.totalDebit + it.totalCredit }
        
        categoryStats.forEachIndexed { index, stat ->
            val row = sheet.createRow(index + 1)
            
            val categoryCell = row.createCell(0)
            categoryCell.setCellValue(stat.category.displayName)
            categoryCell.cellStyle = normalStyle
            
            val countCell = row.createCell(1)
            countCell.setCellValue(stat.count.toDouble())
            countCell.cellStyle = normalStyle
            
            val debitCell = row.createCell(2)
            debitCell.setCellValue(stat.totalDebit)
            debitCell.cellStyle = currencyStyle
            
            val creditCell = row.createCell(3)
            creditCell.setCellValue(stat.totalCredit)
            creditCell.cellStyle = currencyStyle
            
            val netCell = row.createCell(4)
            netCell.setCellValue(stat.totalCredit - stat.totalDebit)
            netCell.cellStyle = currencyStyle
        }
        
        sheet.setColumnWidth(0, 6000)
        sheet.setColumnWidth(1, 3000)
        sheet.setColumnWidth(2, 4000)
        sheet.setColumnWidth(3, 4000)
        sheet.setColumnWidth(4, 4000)
        
        sheet.createFreezePane(0, 1)
    }
    
    private fun createTitleStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        font.fontHeightInPoints = 16
        style.setFont(font)
        return style
    }
    
    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        font.color = IndexedColors.WHITE.index
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.DARK_BLUE.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.borderBottom = BorderStyle.THIN
        style.borderTop = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER
        return style
    }
    
    private fun createCurrencyStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.dataFormat = workbook.createDataFormat().getFormat("₹#,##0.00")
        style.borderBottom = BorderStyle.THIN
        style.borderTop = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        return style
    }
    
    private fun createDateStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.borderBottom = BorderStyle.THIN
        style.borderTop = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        return style
    }
    
    private fun createNormalStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.borderBottom = BorderStyle.THIN
        style.borderTop = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        style.wrapText = false
        return style
    }
    
    private data class CategoryStat(
        val category: Category,
        val count: Int,
        val totalDebit: Double,
        val totalCredit: Double
    )
}

package com.yourcompany.itrstatement.domain.exporter

import com.yourcompany.itrstatement.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExporter {
    
    sealed class Result {
        data class Success(val fileName: String) : Result()
        data class Error(val message: String, val exception: Throwable? = null) : Result()
    }
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
    
    suspend fun exportToPdf(
        transactions: List<Transaction>,
        outputStream: OutputStream
    ): Result = withContext(Dispatchers.IO) {
        if (transactions.isEmpty()) {
            return@withContext Result.Error("No transactions to export")
        }
        
        try {
            val pdfContent = generatePdfContent(transactions)
            
            BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->
                writer.write(pdfContent)
            }
            
            val fileName = "transactions_report_${timestampFormat.format(Date())}.txt"
            Result.Success(fileName)
        } catch (e: Exception) {
            Result.Error("Failed to export PDF report: ${e.message}", e)
        }
    }
    
    private fun generatePdfContent(transactions: List<Transaction>): String {
        val builder = StringBuilder()
        
        builder.appendLine("=" * 80)
        builder.appendLine("BANK STATEMENT ITR-READY REPORT")
        builder.appendLine("=" * 80)
        builder.appendLine()
        
        builder.appendLine("Generated on: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}")
        builder.appendLine()
        
        val totalDebit = transactions.sumOf { it.debitAmount }
        val totalCredit = transactions.sumOf { it.creditAmount }
        val netAmount = totalCredit - totalDebit
        
        builder.appendLine("-" * 80)
        builder.appendLine("SUMMARY")
        builder.appendLine("-" * 80)
        builder.appendLine()
        builder.appendLine("Total Transactions: ${transactions.size}")
        builder.appendLine("Total Debits: ${formatCurrency(totalDebit)}")
        builder.appendLine("Total Credits: ${formatCurrency(totalCredit)}")
        builder.appendLine("Net Amount: ${formatCurrency(netAmount)}")
        builder.appendLine()
        
        if (transactions.isNotEmpty()) {
            val minDate = transactions.minOf { it.date }
            val maxDate = transactions.maxOf { it.date }
            builder.appendLine("Date Range: ${dateFormat.format(Date(minDate))} to ${dateFormat.format(Date(maxDate))}")
            builder.appendLine()
        }
        
        builder.appendLine("-" * 80)
        builder.appendLine("CATEGORY BREAKDOWN")
        builder.appendLine("-" * 80)
        builder.appendLine()
        
        val categoryMap = transactions.groupBy { it.category }
        val categoryStats = categoryMap.map { (category, txns) ->
            Triple(
                category.displayName,
                txns.size,
                txns.sumOf { it.debitAmount + it.creditAmount }
            )
        }.sortedByDescending { it.third }
        
        builder.appendLine(String.format("%-30s %10s %20s", "Category", "Count", "Total Amount"))
        builder.appendLine("-" * 80)
        
        for ((category, count, amount) in categoryStats) {
            builder.appendLine(
                String.format(
                    "%-30s %10d %20s",
                    category,
                    count,
                    formatCurrency(amount)
                )
            )
        }
        builder.appendLine()
        
        builder.appendLine("-" * 80)
        builder.appendLine("TRANSACTIONS")
        builder.appendLine("-" * 80)
        builder.appendLine()
        
        builder.appendLine(
            String.format(
                "%-12s %-30s %-20s %12s %12s %12s",
                "Date",
                "Description",
                "Category",
                "Debit",
                "Credit",
                "Balance"
            )
        )
        builder.appendLine("-" * 80)
        
        for (transaction in transactions.sortedBy { it.date }) {
            val date = dateFormat.format(Date(transaction.date))
            val description = truncate(transaction.description, 30)
            val category = truncate(transaction.category.displayName, 20)
            val debit = formatAmount(transaction.debitAmount)
            val credit = formatAmount(transaction.creditAmount)
            val balance = formatAmount(transaction.balance)
            
            builder.appendLine(
                String.format(
                    "%-12s %-30s %-20s %12s %12s %12s",
                    date,
                    description,
                    category,
                    debit,
                    credit,
                    balance
                )
            )
        }
        
        builder.appendLine()
        builder.appendLine("=" * 80)
        builder.appendLine("END OF REPORT")
        builder.appendLine("=" * 80)
        
        return builder.toString()
    }
    
    private fun formatCurrency(amount: Double): String {
        return "₹${String.format(Locale.US, "%,.2f", amount)}"
    }
    
    private fun formatAmount(amount: Double): String {
        return if (amount == 0.0) {
            "-"
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
    }
    
    private fun truncate(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.take(maxLength - 3) + "..."
        } else {
            text.padEnd(maxLength)
        }
    }
    
    private operator fun String.times(count: Int): String {
        return this.repeat(count)
    }
}

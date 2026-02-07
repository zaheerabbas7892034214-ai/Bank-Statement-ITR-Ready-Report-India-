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

class CsvExporter {
    
    sealed class Result {
        data class Success(val fileName: String) : Result()
        data class Error(val message: String, val exception: Throwable? = null) : Result()
    }
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
    
    suspend fun exportToCsv(
        transactions: List<Transaction>,
        outputStream: OutputStream
    ): Result = withContext(Dispatchers.IO) {
        if (transactions.isEmpty()) {
            return@withContext Result.Error("No transactions to export")
        }
        
        try {
            BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->
                writer.write("Date,Description,Category,Debit,Credit,Balance\n")
                
                for (transaction in transactions) {
                    val date = dateFormat.format(Date(transaction.date))
                    val description = escapeField(transaction.description)
                    val category = transaction.category.displayName
                    val debit = formatAmount(transaction.debitAmount)
                    val credit = formatAmount(transaction.creditAmount)
                    val balance = formatAmount(transaction.balance)
                    
                    writer.write("$date,\"$description\",\"$category\",$debit,$credit,$balance\n")
                }
            }
            
            val fileName = "transactions_${timestampFormat.format(Date())}.csv"
            Result.Success(fileName)
        } catch (e: Exception) {
            Result.Error("Failed to export CSV: ${e.message}", e)
        }
    }
    
    suspend fun exportWithSummary(
        transactions: List<Transaction>,
        outputStream: OutputStream
    ): Result = withContext(Dispatchers.IO) {
        if (transactions.isEmpty()) {
            return@withContext Result.Error("No transactions to export")
        }
        
        try {
            BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->
                writer.write("Bank Statement ITR Report\n")
                writer.write("Generated on: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}\n")
                writer.write("\n")
                
                val totalDebit = transactions.sumOf { it.debitAmount }
                val totalCredit = transactions.sumOf { it.creditAmount }
                
                writer.write("Summary\n")
                writer.write("Total Transactions,${transactions.size}\n")
                writer.write("Total Debits,${formatAmount(totalDebit)}\n")
                writer.write("Total Credits,${formatAmount(totalCredit)}\n")
                writer.write("Net Amount,${formatAmount(totalCredit - totalDebit)}\n")
                writer.write("\n")
                
                val categoryTotals = transactions
                    .groupBy { it.category }
                    .mapValues { (_, txns) ->
                        txns.sumOf { it.debitAmount + it.creditAmount }
                    }
                    .toList()
                    .sortedByDescending { it.second }
                
                writer.write("Category Breakdown\n")
                writer.write("Category,Total Amount\n")
                for ((category, amount) in categoryTotals) {
                    writer.write("\"${category.displayName}\",${formatAmount(amount)}\n")
                }
                writer.write("\n")
                
                writer.write("Transactions\n")
                writer.write("Date,Description,Category,Debit,Credit,Balance\n")
                
                for (transaction in transactions) {
                    val date = dateFormat.format(Date(transaction.date))
                    val description = escapeField(transaction.description)
                    val category = transaction.category.displayName
                    val debit = formatAmount(transaction.debitAmount)
                    val credit = formatAmount(transaction.creditAmount)
                    val balance = formatAmount(transaction.balance)
                    
                    writer.write("$date,\"$description\",\"$category\",$debit,$credit,$balance\n")
                }
            }
            
            val fileName = "transactions_summary_${timestampFormat.format(Date())}.csv"
            Result.Success(fileName)
        } catch (e: Exception) {
            Result.Error("Failed to export CSV with summary: ${e.message}", e)
        }
    }
    
    private fun escapeField(field: String): String {
        return field.replace("\"", "\"\"")
    }
    
    private fun formatAmount(amount: Double): String {
        return if (amount == 0.0) "0.00" else String.format(Locale.US, "%.2f", amount)
    }
}

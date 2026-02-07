package com.yourcompany.itrstatement.domain.exporter

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.yourcompany.itrstatement.data.model.Category
import com.yourcompany.itrstatement.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JsonExporter {
    
    sealed class Result {
        data class Success(val fileName: String) : Result()
        data class Error(val message: String, val exception: Throwable? = null) : Result()
    }
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
    
    suspend fun exportToJson(
        transactions: List<Transaction>,
        outputStream: OutputStream
    ): Result = withContext(Dispatchers.IO) {
        if (transactions.isEmpty()) {
            return@withContext Result.Error("No transactions to export")
        }
        
        try {
            val exportData = createExportData(transactions)
            
            BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->
                gson.toJson(exportData, writer)
            }
            
            val fileName = "transactions_${timestampFormat.format(Date())}.json"
            Result.Success(fileName)
        } catch (e: Exception) {
            Result.Error("Failed to export JSON: ${e.message}", e)
        }
    }
    
    private fun createExportData(transactions: List<Transaction>): ExportData {
        val sortedTransactions = transactions.sortedBy { it.date }
        
        val totalDebit = transactions.sumOf { it.debitAmount }
        val totalCredit = transactions.sumOf { it.creditAmount }
        
        val categoryBreakdown = transactions
            .groupBy { it.category }
            .map { (category, txns) ->
                CategoryBreakdown(
                    category = category.displayName,
                    count = txns.size,
                    totalDebit = txns.sumOf { it.debitAmount },
                    totalCredit = txns.sumOf { it.creditAmount },
                    netAmount = txns.sumOf { it.creditAmount - it.debitAmount }
                )
            }
            .sortedByDescending { it.totalDebit + it.totalCredit }
        
        val dateRange = if (sortedTransactions.isNotEmpty()) {
            DateRange(
                startDate = dateFormat.format(Date(sortedTransactions.first().date)),
                endDate = dateFormat.format(Date(sortedTransactions.last().date))
            )
        } else {
            DateRange("", "")
        }
        
        val metadata = Metadata(
            exportedAt = System.currentTimeMillis(),
            exportedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
            totalTransactions = transactions.size,
            dateRange = dateRange
        )
        
        val summary = Summary(
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            netAmount = totalCredit - totalDebit,
            transactionCount = transactions.size
        )
        
        val transactionItems = sortedTransactions.map { transaction ->
            TransactionItem(
                date = dateFormat.format(Date(transaction.date)),
                description = transaction.description,
                category = transaction.category.displayName,
                debit = transaction.debitAmount,
                credit = transaction.creditAmount,
                balance = transaction.balance
            )
        }
        
        return ExportData(
            metadata = metadata,
            summary = summary,
            categoryBreakdown = categoryBreakdown,
            transactions = transactionItems
        )
    }
    
    data class ExportData(
        val metadata: Metadata,
        val summary: Summary,
        val categoryBreakdown: List<CategoryBreakdown>,
        val transactions: List<TransactionItem>
    )
    
    data class Metadata(
        val exportedAt: Long,
        val exportedDate: String,
        val totalTransactions: Int,
        val dateRange: DateRange
    )
    
    data class DateRange(
        val startDate: String,
        val endDate: String
    )
    
    data class Summary(
        val totalDebit: Double,
        val totalCredit: Double,
        val netAmount: Double,
        val transactionCount: Int
    )
    
    data class CategoryBreakdown(
        val category: String,
        val count: Int,
        val totalDebit: Double,
        val totalCredit: Double,
        val netAmount: Double
    )
    
    data class TransactionItem(
        val date: String,
        val description: String,
        val category: String,
        val debit: Double,
        val credit: Double,
        val balance: Double
    )
}

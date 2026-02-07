package com.yourcompany.itrstatement.domain.parser

import com.yourcompany.itrstatement.data.model.Category
import com.yourcompany.itrstatement.data.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

class TransactionParser {
    
    sealed class Result {
        data class Success(val transactions: List<Transaction>) : Result()
        data class Partial(val transactions: List<Transaction>, val errors: Int) : Result()
        data class Error(val message: String) : Result()
    }
    
    private val datePatterns = listOf(
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()),
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()),
        SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()),
        SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yy", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yy", Locale.getDefault())
    ).apply {
        forEach { it.isLenient = false }
    }
    
    private val amountPattern = Pattern.compile(
        """(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )
    
    private val transactionLinePattern = Pattern.compile(
        """(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{1,2}\s+[A-Za-z]{3}\s+\d{2,4})""",
        Pattern.CASE_INSENSITIVE
    )
    
    fun parseTransactions(text: String, importSessionId: Long): Result {
        if (text.isBlank()) {
            return Result.Error("Empty text provided")
        }
        
        val lines = text.lines().filter { it.trim().isNotEmpty() }
        if (lines.isEmpty()) {
            return Result.Error("No valid lines found")
        }
        
        val transactions = mutableListOf<Transaction>()
        var errorCount = 0
        val currentTime = System.currentTimeMillis()
        
        for (line in lines) {
            try {
                val transaction = parseTransactionLine(line, importSessionId, currentTime)
                if (transaction != null) {
                    transactions.add(transaction)
                }
            } catch (e: Exception) {
                errorCount++
            }
        }
        
        return when {
            transactions.isEmpty() -> Result.Error("No transactions could be parsed")
            errorCount > 0 -> Result.Partial(transactions, errorCount)
            else -> Result.Success(transactions)
        }
    }
    
    private fun parseTransactionLine(
        line: String,
        importSessionId: Long,
        currentTime: Long
    ): Transaction? {
        val matcher = transactionLinePattern.matcher(line)
        if (!matcher.find()) {
            return null
        }
        
        val dateStr = matcher.group(1) ?: return null
        val date = parseDate(dateStr) ?: return null
        
        val remainingText = line.substring(matcher.end()).trim()
        
        val amounts = extractAmounts(remainingText)
        if (amounts.isEmpty()) {
            return null
        }
        
        val (debitAmount, creditAmount, balance) = when (amounts.size) {
            1 -> Triple(0.0, 0.0, amounts[0])
            2 -> {
                if (line.contains("Dr", ignoreCase = true) || line.contains("Debit", ignoreCase = true)) {
                    Triple(amounts[0], 0.0, amounts[1])
                } else {
                    Triple(0.0, amounts[0], amounts[1])
                }
            }
            else -> Triple(amounts[0], amounts[1], amounts[2])
        }
        
        val description = extractDescription(remainingText)
        
        return Transaction(
            id = 0,
            importSessionId = importSessionId,
            date = date,
            description = description,
            category = Category.UNCATEGORIZED,
            debitAmount = debitAmount,
            creditAmount = creditAmount,
            balance = balance,
            timestamp = currentTime
        )
    }
    
    private fun parseDate(dateStr: String): Long? {
        for (format in datePatterns) {
            try {
                val date = format.parse(dateStr)
                if (date != null) {
                    return date.time
                }
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }
    
    private fun extractAmounts(text: String): List<Double> {
        val amounts = mutableListOf<Double>()
        val matcher = amountPattern.matcher(text)
        
        while (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(",", "") ?: continue
            try {
                val amount = amountStr.toDouble()
                if (amount >= 0) {
                    amounts.add(amount)
                }
            } catch (e: NumberFormatException) {
                continue
            }
        }
        
        return amounts
    }
    
    private fun extractDescription(text: String): String {
        var description = text
            .replace(amountPattern.toRegex(), "")
            .replace(Regex("""(?:Dr|Cr|Debit|Credit|Balance)\b""", RegexOption.IGNORE_CASE), "")
            .trim()
        
        description = description
            .replace(Regex("\\s+"), " ")
            .take(200)
        
        if (description.isBlank()) {
            description = "Transaction"
        }
        
        return description
    }
    
    fun parseAdvanced(text: String, importSessionId: Long): Result {
        val basicResult = parseTransactions(text, importSessionId)
        
        if (basicResult !is Result.Success && basicResult !is Result.Partial) {
            return parseTabularFormat(text, importSessionId)
        }
        
        return basicResult
    }
    
    private fun parseTabularFormat(text: String, importSessionId: Long): Result {
        val lines = text.lines()
        val transactions = mutableListOf<Transaction>()
        var headerFound = false
        var dateColumnIndex = -1
        var descColumnIndex = -1
        var debitColumnIndex = -1
        var creditColumnIndex = -1
        var balanceColumnIndex = -1
        val currentTime = System.currentTimeMillis()
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            
            if (!headerFound) {
                val lower = trimmed.lowercase()
                if (lower.contains("date") || lower.contains("particulars") || lower.contains("description")) {
                    val parts = trimmed.split(Regex("\\s{2,}|\\t|\\|"))
                    
                    parts.forEachIndexed { index, part ->
                        val partLower = part.lowercase()
                        when {
                            partLower.contains("date") -> dateColumnIndex = index
                            partLower.contains("particular") || partLower.contains("description") || partLower.contains("narration") -> descColumnIndex = index
                            partLower.contains("debit") || partLower.contains("withdrawal") -> debitColumnIndex = index
                            partLower.contains("credit") || partLower.contains("deposit") -> creditColumnIndex = index
                            partLower.contains("balance") -> balanceColumnIndex = index
                        }
                    }
                    
                    if (dateColumnIndex >= 0) {
                        headerFound = true
                    }
                    continue
                }
            }
            
            if (headerFound && dateColumnIndex >= 0) {
                val parts = trimmed.split(Regex("\\s{2,}|\\t|\\|"))
                if (parts.size > dateColumnIndex) {
                    val dateStr = parts[dateColumnIndex].trim()
                    val date = parseDate(dateStr)
                    
                    if (date != null) {
                        val description = if (descColumnIndex >= 0 && parts.size > descColumnIndex) {
                            parts[descColumnIndex].trim()
                        } else {
                            "Transaction"
                        }
                        
                        val debitAmount = if (debitColumnIndex >= 0 && parts.size > debitColumnIndex) {
                            parseAmount(parts[debitColumnIndex])
                        } else 0.0
                        
                        val creditAmount = if (creditColumnIndex >= 0 && parts.size > creditColumnIndex) {
                            parseAmount(parts[creditColumnIndex])
                        } else 0.0
                        
                        val balance = if (balanceColumnIndex >= 0 && parts.size > balanceColumnIndex) {
                            parseAmount(parts[balanceColumnIndex])
                        } else 0.0
                        
                        transactions.add(
                            Transaction(
                                id = 0,
                                importSessionId = importSessionId,
                                date = date,
                                description = description,
                                category = Category.UNCATEGORIZED,
                                debitAmount = debitAmount,
                                creditAmount = creditAmount,
                                balance = balance,
                                timestamp = currentTime
                            )
                        )
                    }
                }
            }
        }
        
        return when {
            transactions.isEmpty() -> Result.Error("No transactions could be parsed from tabular format")
            else -> Result.Success(transactions)
        }
    }
    
    private fun parseAmount(text: String): Double {
        val cleaned = text
            .replace(Regex("[^0-9.]"), "")
            .trim()
        
        return try {
            if (cleaned.isNotEmpty()) cleaned.toDouble() else 0.0
        } catch (e: NumberFormatException) {
            0.0
        }
    }
}

package com.yourcompany.itrstatement.domain.usecase

import com.yourcompany.itrstatement.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class MergeImportsUseCase {
    
    sealed class Result {
        data class Success(val mergedTransactions: List<Transaction>, val stats: MergeStats) : Result()
        data class Error(val message: String) : Result()
    }
    
    data class MergeStats(
        val totalInputTransactions: Int,
        val mergedTransactions: Int,
        val duplicatesRemoved: Int,
        val sessionsCount: Int
    )
    
    suspend fun mergeImportSessions(
        transactionsBySession: Map<Long, List<Transaction>>
    ): Result = withContext(Dispatchers.IO) {
        try {
            if (transactionsBySession.isEmpty()) {
                return@withContext Result.Error("No import sessions to merge")
            }
            
            val allTransactions = transactionsBySession.values.flatten()
            
            if (allTransactions.isEmpty()) {
                return@withContext Result.Error("No transactions found in import sessions")
            }
            
            val merged = removeDuplicatesAndSort(allTransactions)
            
            val stats = MergeStats(
                totalInputTransactions = allTransactions.size,
                mergedTransactions = merged.size,
                duplicatesRemoved = allTransactions.size - merged.size,
                sessionsCount = transactionsBySession.size
            )
            
            Result.Success(merged, stats)
        } catch (e: Exception) {
            Result.Error("Merge failed: ${e.message}")
        }
    }
    
    suspend fun mergeTransactions(
        transactionLists: List<List<Transaction>>
    ): Result = withContext(Dispatchers.IO) {
        try {
            if (transactionLists.isEmpty()) {
                return@withContext Result.Error("No transaction lists to merge")
            }
            
            val allTransactions = transactionLists.flatten()
            
            if (allTransactions.isEmpty()) {
                return@withContext Result.Error("No transactions found")
            }
            
            val merged = removeDuplicatesAndSort(allTransactions)
            
            val stats = MergeStats(
                totalInputTransactions = allTransactions.size,
                mergedTransactions = merged.size,
                duplicatesRemoved = allTransactions.size - merged.size,
                sessionsCount = transactionLists.size
            )
            
            Result.Success(merged, stats)
        } catch (e: Exception) {
            Result.Error("Merge failed: ${e.message}")
        }
    }
    
    private fun removeDuplicatesAndSort(transactions: List<Transaction>): List<Transaction> {
        val uniqueTransactions = mutableMapOf<String, Transaction>()
        
        for (transaction in transactions) {
            val hash = generateTransactionHash(transaction)
            
            if (!uniqueTransactions.containsKey(hash)) {
                uniqueTransactions[hash] = transaction
            } else {
                val existing = uniqueTransactions[hash]!!
                if (transaction.importSessionId < existing.importSessionId) {
                    uniqueTransactions[hash] = transaction
                }
            }
        }
        
        return uniqueTransactions.values.sortedBy { it.date }
    }
    
    private fun generateTransactionHash(transaction: Transaction): String {
        val normalized = normalizeDescription(transaction.description)
        
        val input = buildString {
            append(transaction.date)
            append("|")
            append(normalized)
            append("|")
            append(String.format("%.2f", transaction.debitAmount))
            append("|")
            append(String.format("%.2f", transaction.creditAmount))
        }
        
        return md5Hash(input)
    }
    
    private fun normalizeDescription(description: String): String {
        return description
            .trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9\\s]"), "")
    }
    
    private fun md5Hash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    suspend fun detectDuplicates(transactions: List<Transaction>): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val hashGroups = transactions.groupBy { generateTransactionHash(it) }
        
        hashGroups
            .filter { it.value.size > 1 }
            .map { (hash, duplicates) ->
                DuplicateGroup(
                    hash = hash,
                    transactions = duplicates,
                    count = duplicates.size
                )
            }
            .sortedByDescending { it.count }
    }
    
    suspend fun mergeWithDuplicateResolution(
        transactionLists: List<List<Transaction>>,
        strategy: DuplicateStrategy = DuplicateStrategy.KEEP_FIRST
    ): Result = withContext(Dispatchers.IO) {
        try {
            if (transactionLists.isEmpty()) {
                return@withContext Result.Error("No transaction lists to merge")
            }
            
            val allTransactions = transactionLists.flatten()
            
            if (allTransactions.isEmpty()) {
                return@withContext Result.Error("No transactions found")
            }
            
            val merged = when (strategy) {
                DuplicateStrategy.KEEP_FIRST -> removeDuplicatesKeepFirst(allTransactions)
                DuplicateStrategy.KEEP_LAST -> removeDuplicatesKeepLast(allTransactions)
                DuplicateStrategy.KEEP_OLDEST_SESSION -> removeDuplicatesKeepOldestSession(allTransactions)
            }
            
            val sorted = merged.sortedBy { it.date }
            
            val stats = MergeStats(
                totalInputTransactions = allTransactions.size,
                mergedTransactions = sorted.size,
                duplicatesRemoved = allTransactions.size - sorted.size,
                sessionsCount = transactionLists.size
            )
            
            Result.Success(sorted, stats)
        } catch (e: Exception) {
            Result.Error("Merge with duplicate resolution failed: ${e.message}")
        }
    }
    
    private fun removeDuplicatesKeepFirst(transactions: List<Transaction>): List<Transaction> {
        val uniqueTransactions = mutableMapOf<String, Transaction>()
        
        for (transaction in transactions) {
            val hash = generateTransactionHash(transaction)
            if (!uniqueTransactions.containsKey(hash)) {
                uniqueTransactions[hash] = transaction
            }
        }
        
        return uniqueTransactions.values.toList()
    }
    
    private fun removeDuplicatesKeepLast(transactions: List<Transaction>): List<Transaction> {
        val uniqueTransactions = mutableMapOf<String, Transaction>()
        
        for (transaction in transactions) {
            val hash = generateTransactionHash(transaction)
            uniqueTransactions[hash] = transaction
        }
        
        return uniqueTransactions.values.toList()
    }
    
    private fun removeDuplicatesKeepOldestSession(transactions: List<Transaction>): List<Transaction> {
        val uniqueTransactions = mutableMapOf<String, Transaction>()
        
        for (transaction in transactions) {
            val hash = generateTransactionHash(transaction)
            
            val existing = uniqueTransactions[hash]
            if (existing == null || transaction.importSessionId < existing.importSessionId) {
                uniqueTransactions[hash] = transaction
            }
        }
        
        return uniqueTransactions.values.toList()
    }
    
    data class DuplicateGroup(
        val hash: String,
        val transactions: List<Transaction>,
        val count: Int
    )
    
    enum class DuplicateStrategy {
        KEEP_FIRST,
        KEEP_LAST,
        KEEP_OLDEST_SESSION
    }
}

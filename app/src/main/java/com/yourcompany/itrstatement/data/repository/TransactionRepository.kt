package com.yourcompany.itrstatement.data.repository

import com.yourcompany.itrstatement.data.local.dao.TransactionDao
import com.yourcompany.itrstatement.data.local.entities.TransactionEntity
import com.yourcompany.itrstatement.data.model.Category
import com.yourcompany.itrstatement.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepository(private val transactionDao: TransactionDao) {
    
    suspend fun insertAll(transactions: List<Transaction>): List<Long> {
        val entities = transactions.map { it.toEntity() }
        return transactionDao.insertAll(entities)
    }
    
    fun getAll(): Flow<List<Transaction>> {
        return transactionDao.getAll().map { entities ->
            entities.map { it.toTransaction() }
        }
    }
    
    fun getBySession(sessionId: Long): Flow<List<Transaction>> {
        return transactionDao.getBySession(sessionId).map { entities ->
            entities.map { it.toTransaction() }
        }
    }
    
    suspend fun deleteBySession(sessionId: Long): Int {
        return transactionDao.deleteBySession(sessionId)
    }
    
    fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(query).map { entities ->
            entities.map { it.toTransaction() }
        }
    }
    
    fun getByCategory(category: Category): Flow<List<Transaction>> {
        return transactionDao.getByCategory(category.name).map { entities ->
            entities.map { it.toTransaction() }
        }
    }
    
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toTransaction() }
        }
    }
    
    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            importSessionId = importSessionId,
            date = date,
            description = description,
            category = category.name,
            debitAmount = debitAmount,
            creditAmount = creditAmount,
            balance = balance,
            timestamp = timestamp
        )
    }
    
    private fun TransactionEntity.toTransaction(): Transaction {
        return Transaction(
            id = id,
            importSessionId = importSessionId,
            date = date,
            description = description,
            category = Category.valueOf(category),
            debitAmount = debitAmount,
            creditAmount = creditAmount,
            balance = balance,
            timestamp = timestamp
        )
    }
}

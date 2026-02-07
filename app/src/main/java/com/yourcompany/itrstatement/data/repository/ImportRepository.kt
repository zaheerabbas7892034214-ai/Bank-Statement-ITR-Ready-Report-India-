package com.yourcompany.itrstatement.data.repository

import com.yourcompany.itrstatement.data.local.dao.ImportHistoryDao
import com.yourcompany.itrstatement.data.local.entities.ImportHistoryEntity
import com.yourcompany.itrstatement.data.model.ImportSession
import com.yourcompany.itrstatement.data.model.ParsingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ImportRepository(private val importHistoryDao: ImportHistoryDao) {
    
    suspend fun insert(importSession: ImportSession): Long {
        return importHistoryDao.insert(importSession.toEntity())
    }
    
    fun getAll(): Flow<List<ImportSession>> {
        return importHistoryDao.getAll().map { entities ->
            entities.map { it.toImportSession() }
        }
    }
    
    fun getRecent(limit: Int): Flow<List<ImportSession>> {
        return importHistoryDao.getRecent(limit).map { entities ->
            entities.map { it.toImportSession() }
        }
    }
    
    suspend fun delete(id: Long): Int {
        return importHistoryDao.delete(id)
    }
    
    suspend fun getById(id: Long): ImportSession? {
        return importHistoryDao.getById(id)?.toImportSession()
    }
    
    private fun ImportSession.toEntity(): ImportHistoryEntity {
        return ImportHistoryEntity(
            id = id,
            fileName = fileName,
            importTimestamp = importTimestamp,
            transactionCount = transactionCount,
            fileUri = fileUri,
            parsingStatus = parsingStatus.name
        )
    }
    
    private fun ImportHistoryEntity.toImportSession(): ImportSession {
        return ImportSession(
            id = id,
            fileName = fileName,
            importTimestamp = importTimestamp,
            transactionCount = transactionCount,
            fileUri = fileUri,
            parsingStatus = ParsingStatus.valueOf(parsingStatus)
        )
    }
}

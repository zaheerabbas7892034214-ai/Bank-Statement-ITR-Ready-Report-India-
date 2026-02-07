package com.yourcompany.itrstatement.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yourcompany.itrstatement.data.local.entities.ImportHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportHistoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(importHistory: ImportHistoryEntity): Long
    
    @Query("SELECT * FROM import_history ORDER BY import_timestamp DESC")
    fun getAll(): Flow<List<ImportHistoryEntity>>
    
    @Query("SELECT * FROM import_history ORDER BY import_timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ImportHistoryEntity>>
    
    @Query("DELETE FROM import_history WHERE id = :id")
    suspend fun delete(id: Long): Int
    
    @Query("SELECT * FROM import_history WHERE id = :id")
    suspend fun getById(id: Long): ImportHistoryEntity?
}

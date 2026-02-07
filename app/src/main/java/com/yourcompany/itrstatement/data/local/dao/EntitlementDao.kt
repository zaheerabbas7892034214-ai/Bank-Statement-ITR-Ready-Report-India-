package com.yourcompany.itrstatement.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yourcompany.itrstatement.data.local.entities.EntitlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntitlementDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entitlement: EntitlementEntity)
    
    @Query("SELECT * FROM entitlement WHERE id = 1")
    fun get(): Flow<EntitlementEntity?>
    
    @Update
    suspend fun update(entitlement: EntitlementEntity)
}

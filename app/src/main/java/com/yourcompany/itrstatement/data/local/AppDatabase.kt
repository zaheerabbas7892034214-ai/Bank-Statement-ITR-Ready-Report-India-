package com.yourcompany.itrstatement.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yourcompany.itrstatement.data.local.dao.EntitlementDao
import com.yourcompany.itrstatement.data.local.dao.ImportHistoryDao
import com.yourcompany.itrstatement.data.local.dao.TransactionDao
import com.yourcompany.itrstatement.data.local.entities.EntitlementEntity
import com.yourcompany.itrstatement.data.local.entities.ImportHistoryEntity
import com.yourcompany.itrstatement.data.local.entities.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        ImportHistoryEntity::class,
        EntitlementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun transactionDao(): TransactionDao
    abstract fun importHistoryDao(): ImportHistoryDao
    abstract fun entitlementDao(): EntitlementDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "itr_statement_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

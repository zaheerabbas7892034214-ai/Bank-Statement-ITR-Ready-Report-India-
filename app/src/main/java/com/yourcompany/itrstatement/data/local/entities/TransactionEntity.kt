package com.yourcompany.itrstatement.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "import_session_id")
    val importSessionId: Long,
    
    @ColumnInfo(name = "date")
    val date: Long,
    
    @ColumnInfo(name = "description")
    val description: String,
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "debit_amount")
    val debitAmount: Double,
    
    @ColumnInfo(name = "credit_amount")
    val creditAmount: Double,
    
    @ColumnInfo(name = "balance")
    val balance: Double,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)

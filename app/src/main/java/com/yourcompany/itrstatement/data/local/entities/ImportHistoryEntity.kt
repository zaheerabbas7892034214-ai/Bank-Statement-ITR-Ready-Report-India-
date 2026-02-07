package com.yourcompany.itrstatement.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "import_history")
data class ImportHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "file_name")
    val fileName: String,
    
    @ColumnInfo(name = "import_timestamp")
    val importTimestamp: Long,
    
    @ColumnInfo(name = "transaction_count")
    val transactionCount: Int,
    
    @ColumnInfo(name = "file_uri")
    val fileUri: String,
    
    @ColumnInfo(name = "parsing_status")
    val parsingStatus: String
)

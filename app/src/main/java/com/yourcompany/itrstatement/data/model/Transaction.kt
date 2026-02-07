package com.yourcompany.itrstatement.data.model

data class Transaction(
    val id: Long = 0,
    val importSessionId: Long,
    val date: Long,
    val description: String,
    val category: Category,
    val debitAmount: Double,
    val creditAmount: Double,
    val balance: Double,
    val timestamp: Long
)

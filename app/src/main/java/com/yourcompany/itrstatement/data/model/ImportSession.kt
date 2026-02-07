package com.yourcompany.itrstatement.data.model

data class ImportSession(
    val id: Long = 0,
    val fileName: String,
    val importTimestamp: Long,
    val transactionCount: Int,
    val fileUri: String,
    val parsingStatus: ParsingStatus
)

enum class ParsingStatus {
    SUCCESS,
    FAILED,
    PARTIAL,
    IN_PROGRESS
}

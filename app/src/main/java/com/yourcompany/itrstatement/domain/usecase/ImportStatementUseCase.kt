package com.yourcompany.itrstatement.domain.usecase

import android.content.Context
import android.net.Uri
import com.yourcompany.itrstatement.data.model.ImportSession
import com.yourcompany.itrstatement.data.model.ParsingStatus
import com.yourcompany.itrstatement.data.model.Transaction
import com.yourcompany.itrstatement.data.repository.ImportRepository
import com.yourcompany.itrstatement.data.repository.TransactionRepository
import com.yourcompany.itrstatement.domain.categorizer.TransactionCategorizer
import com.yourcompany.itrstatement.domain.parser.OcrProcessor
import com.yourcompany.itrstatement.domain.parser.PdfTextExtractor
import com.yourcompany.itrstatement.domain.parser.TransactionParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImportStatementUseCase(
    private val pdfExtractor: PdfTextExtractor,
    private val ocrProcessor: OcrProcessor,
    private val transactionParser: TransactionParser,
    private val transactionCategorizer: TransactionCategorizer,
    private val importRepository: ImportRepository,
    private val transactionRepository: TransactionRepository
) {
    
    sealed class Result {
        data class Success(val importSession: ImportSession, val transactions: List<Transaction>) : Result()
        data class Partial(val importSession: ImportSession, val transactions: List<Transaction>, val errors: String) : Result()
        data class Error(val message: String) : Result()
    }
    
    suspend fun importStatement(
        context: Context,
        uri: Uri,
        fileName: String
    ): Result = withContext(Dispatchers.IO) {
        try {
            val extractedText = extractText(context, uri)
                ?: return@withContext Result.Error("Failed to extract text from document")
            
            val parseResult = transactionParser.parseAdvanced(extractedText, 0)
            
            when (parseResult) {
                is TransactionParser.Result.Success -> {
                    saveAndCategorize(parseResult.transactions, fileName, uri.toString(), ParsingStatus.SUCCESS)
                }
                is TransactionParser.Result.Partial -> {
                    val result = saveAndCategorize(
                        parseResult.transactions,
                        fileName,
                        uri.toString(),
                        ParsingStatus.PARTIAL
                    )
                    when (result) {
                        is Result.Success -> Result.Partial(
                            result.importSession,
                            result.transactions,
                            "Partially parsed with ${parseResult.errors} errors"
                        )
                        else -> result
                    }
                }
                is TransactionParser.Result.Error -> {
                    Result.Error("Failed to parse transactions: ${parseResult.message}")
                }
            }
        } catch (e: Exception) {
            Result.Error("Import failed: ${e.message}")
        }
    }
    
    private suspend fun extractText(context: Context, uri: Uri): String? {
        val pdfResult = pdfExtractor.extractText(context, uri)
        
        return when (pdfResult) {
            is PdfTextExtractor.Result.Success -> pdfResult.text
            is PdfTextExtractor.Result.Error -> {
                val ocrResult = ocrProcessor.recognizeText(context, uri)
                when (ocrResult) {
                    is OcrProcessor.Result.Success -> ocrResult.text
                    is OcrProcessor.Result.Error -> null
                }
            }
        }
    }
    
    private suspend fun saveAndCategorize(
        transactions: List<Transaction>,
        fileName: String,
        fileUri: String,
        status: ParsingStatus
    ): Result {
        if (transactions.isEmpty()) {
            return Result.Error("No transactions found")
        }
        
        val importSession = ImportSession(
            id = 0,
            fileName = fileName,
            importTimestamp = System.currentTimeMillis(),
            transactionCount = transactions.size,
            fileUri = fileUri,
            parsingStatus = status
        )
        
        val sessionId = importRepository.insert(importSession)
        
        val categorizedTransactions = transactionCategorizer.categorizeAll(transactions)
        
        val transactionsWithSession = categorizedTransactions.map { it.copy(importSessionId = sessionId) }
        
        transactionRepository.insertAll(transactionsWithSession)
        
        val savedSession = importSession.copy(id = sessionId)
        
        return Result.Success(savedSession, transactionsWithSession)
    }
    
    suspend fun reimportStatement(
        context: Context,
        sessionId: Long,
        uri: Uri,
        fileName: String
    ): Result = withContext(Dispatchers.IO) {
        try {
            transactionRepository.deleteBySession(sessionId)
            importRepository.delete(sessionId)
            
            importStatement(context, uri, fileName)
        } catch (e: Exception) {
            Result.Error("Reimport failed: ${e.message}")
        }
    }
    
    suspend fun validateImport(context: Context, uri: Uri): ValidationResult = withContext(Dispatchers.IO) {
        try {
            val extractResult = pdfExtractor.extractText(context, uri)
            
            when (extractResult) {
                is PdfTextExtractor.Result.Success -> {
                    val text = extractResult.text
                    val hasDatePatterns = text.contains(Regex("""\d{1,2}[/-]\d{1,2}[/-]\d{2,4}"""))
                    val hasAmountPatterns = text.contains(Regex("""(?:Rs\.?|INR|₹)?\s*\d+"""))
                    
                    if (hasDatePatterns && hasAmountPatterns) {
                        ValidationResult.Valid(estimatedTransactionCount(text))
                    } else {
                        ValidationResult.Invalid("Document does not appear to be a bank statement")
                    }
                }
                is PdfTextExtractor.Result.Error -> {
                    ValidationResult.Invalid("Cannot read document: ${extractResult.message}")
                }
            }
        } catch (e: Exception) {
            ValidationResult.Invalid("Validation failed: ${e.message}")
        }
    }
    
    private fun estimatedTransactionCount(text: String): Int {
        val dateMatches = Regex("""\d{1,2}[/-]\d{1,2}[/-]\d{2,4}""").findAll(text)
        return dateMatches.count()
    }
    
    sealed class ValidationResult {
        data class Valid(val estimatedTransactions: Int) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}

package com.yourcompany.itrstatement.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.yourcompany.itrstatement.data.model.Transaction
import com.yourcompany.itrstatement.data.repository.EntitlementRepository
import com.yourcompany.itrstatement.domain.exporter.CsvExporter
import com.yourcompany.itrstatement.domain.exporter.ExcelExporter
import com.yourcompany.itrstatement.domain.exporter.JsonExporter
import com.yourcompany.itrstatement.domain.exporter.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ExportDataUseCase(
    private val entitlementRepository: EntitlementRepository,
    private val csvExporter: CsvExporter,
    private val excelExporter: ExcelExporter,
    private val jsonExporter: JsonExporter,
    private val pdfExporter: PdfExporter
) {
    
    sealed class Result {
        data class Success(val fileName: String, val uri: Uri) : Result()
        data class Error(val message: String) : Result()
        data class ProRequired(val message: String) : Result()
    }
    
    enum class ExportFormat {
        CSV,
        EXCEL,
        JSON,
        PDF
    }
    
    suspend fun exportData(
        context: Context,
        transactions: List<Transaction>,
        format: ExportFormat,
        destinationUri: Uri
    ): Result = withContext(Dispatchers.IO) {
        try {
            if (transactions.isEmpty()) {
                return@withContext Result.Error("No transactions to export")
            }
            
            val requiresPro = format != ExportFormat.CSV
            
            if (requiresPro) {
                val entitlement = entitlementRepository.get().first()
                if (entitlement?.isProActive != true) {
                    return@withContext Result.ProRequired(
                        "Pro version required for ${format.name} export. Upgrade to unlock this feature."
                    )
                }
            }
            
            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: return@withContext Result.Error("Cannot open destination file")
            
            val exportResult = when (format) {
                ExportFormat.CSV -> csvExporter.exportWithSummary(transactions, outputStream)
                ExportFormat.EXCEL -> excelExporter.exportToExcel(transactions, outputStream)
                ExportFormat.JSON -> jsonExporter.exportToJson(transactions, outputStream)
                ExportFormat.PDF -> pdfExporter.exportToPdf(transactions, outputStream)
            }
            
            outputStream.close()
            
            when (exportResult) {
                is CsvExporter.Result.Success -> Result.Success(exportResult.fileName, destinationUri)
                is ExcelExporter.Result.Success -> Result.Success(exportResult.fileName, destinationUri)
                is JsonExporter.Result.Success -> Result.Success(exportResult.fileName, destinationUri)
                is PdfExporter.Result.Success -> Result.Success(exportResult.fileName, destinationUri)
                is CsvExporter.Result.Error -> Result.Error(exportResult.message)
                is ExcelExporter.Result.Error -> Result.Error(exportResult.message)
                is JsonExporter.Result.Error -> Result.Error(exportResult.message)
                is PdfExporter.Result.Error -> Result.Error(exportResult.message)
                else -> Result.Error("Unknown export error")
            }
        } catch (e: Exception) {
            Result.Error("Export failed: ${e.message}")
        }
    }
    
    suspend fun exportToDefaultLocation(
        context: Context,
        transactions: List<Transaction>,
        format: ExportFormat,
        fileName: String
    ): Result = withContext(Dispatchers.IO) {
        try {
            if (transactions.isEmpty()) {
                return@withContext Result.Error("No transactions to export")
            }
            
            val requiresPro = format != ExportFormat.CSV
            
            if (requiresPro) {
                val entitlement = entitlementRepository.get().first()
                if (entitlement?.isProActive != true) {
                    return@withContext Result.ProRequired(
                        "Pro version required for ${format.name} export"
                    )
                }
            }
            
            val extension = when (format) {
                ExportFormat.CSV -> "csv"
                ExportFormat.EXCEL -> "xlsx"
                ExportFormat.JSON -> "json"
                ExportFormat.PDF -> "txt"  // Text-based report format
            }
            
            val fullFileName = if (fileName.endsWith(".$extension")) {
                fileName
            } else {
                "$fileName.$extension"
            }
            
            val cacheFile = java.io.File(context.cacheDir, fullFileName)
            
            cacheFile.outputStream().use { outputStream ->
                val exportResult = when (format) {
                    ExportFormat.CSV -> csvExporter.exportWithSummary(transactions, outputStream)
                    ExportFormat.EXCEL -> excelExporter.exportToExcel(transactions, outputStream)
                    ExportFormat.JSON -> jsonExporter.exportToJson(transactions, outputStream)
                    ExportFormat.PDF -> pdfExporter.exportToPdf(transactions, outputStream)
                }
                
                when (exportResult) {
                    is CsvExporter.Result.Error -> return@withContext Result.Error(exportResult.message)
                    is ExcelExporter.Result.Error -> return@withContext Result.Error(exportResult.message)
                    is JsonExporter.Result.Error -> return@withContext Result.Error(exportResult.message)
                    is PdfExporter.Result.Error -> return@withContext Result.Error(exportResult.message)
                    else -> {}
                }
            }
            
            Result.Success(fullFileName, Uri.fromFile(cacheFile))
        } catch (e: Exception) {
            Result.Error("Export to default location failed: ${e.message}")
        }
    }
    
    suspend fun isProActive(): Boolean = withContext(Dispatchers.IO) {
        try {
            val entitlement = entitlementRepository.get().first()
            entitlement?.isProActive == true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun canExport(format: ExportFormat): Boolean = withContext(Dispatchers.IO) {
        try {
            if (format == ExportFormat.CSV) {
                return@withContext true
            }
            
            val entitlement = entitlementRepository.get().first()
            entitlement?.isProActive == true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getSupportedFormats(isProActive: Boolean): List<ExportFormat> {
        return if (isProActive) {
            listOf(ExportFormat.CSV, ExportFormat.EXCEL, ExportFormat.JSON, ExportFormat.PDF)
        } else {
            listOf(ExportFormat.CSV)
        }
    }
    
    fun getFormatDescription(format: ExportFormat): String {
        return when (format) {
            ExportFormat.CSV -> "CSV (Comma Separated Values) - Compatible with Excel, Google Sheets"
            ExportFormat.EXCEL -> "Excel (XLSX) - Professional report with multiple sheets [PRO]"
            ExportFormat.JSON -> "JSON - Structured data format for developers [PRO]"
            ExportFormat.PDF -> "Text Report - ITR-ready formatted text report [PRO]"
        }
    }
    
    fun getFormatExtension(format: ExportFormat): String {
        return when (format) {
            ExportFormat.CSV -> "csv"
            ExportFormat.EXCEL -> "xlsx"
            ExportFormat.JSON -> "json"
            ExportFormat.PDF -> "txt"  // Text-based report format
        }
    }
}

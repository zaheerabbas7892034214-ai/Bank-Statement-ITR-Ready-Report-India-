package com.yourcompany.itrstatement.domain.parser

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class PdfTextExtractor {
    
    sealed class Result {
        data class Success(val text: String) : Result()
        data class Error(val message: String, val exception: Throwable? = null) : Result()
    }
    
    suspend fun extractText(context: Context, uri: Uri): Result = withContext(Dispatchers.IO) {
        try {
            PDFBoxResourceLoader.init(context)
            
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.Error("Unable to open file")
            
            inputStream.use { stream ->
                val document = PDDocument.load(stream)
                document.use { doc ->
                    if (doc.isEncrypted) {
                        return@withContext Result.Error("PDF is encrypted and cannot be processed")
                    }
                    
                    if (doc.numberOfPages == 0) {
                        return@withContext Result.Error("PDF has no pages")
                    }
                    
                    val stripper = PDFTextStripper()
                    stripper.sortByPosition = true
                    stripper.startPage = 1
                    stripper.endPage = doc.numberOfPages
                    
                    val text = stripper.getText(doc)
                    
                    if (text.isBlank()) {
                        return@withContext Result.Error("No text found in PDF")
                    }
                    
                    Result.Success(text.trim())
                }
            }
        } catch (e: IOException) {
            Result.Error("Failed to read PDF: ${e.message}", e)
        } catch (e: OutOfMemoryError) {
            Result.Error("PDF file is too large to process", e)
        } catch (e: Exception) {
            Result.Error("Unexpected error: ${e.message}", e)
        }
    }
    
    suspend fun extractTextWithPageLimit(
        context: Context, 
        uri: Uri, 
        maxPages: Int = 100
    ): Result = withContext(Dispatchers.IO) {
        try {
            PDFBoxResourceLoader.init(context)
            
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.Error("Unable to open file")
            
            inputStream.use { stream ->
                val document = PDDocument.load(stream)
                document.use { doc ->
                    if (doc.isEncrypted) {
                        return@withContext Result.Error("PDF is encrypted")
                    }
                    
                    val pageCount = doc.numberOfPages
                    if (pageCount == 0) {
                        return@withContext Result.Error("PDF has no pages")
                    }
                    
                    val stripper = PDFTextStripper()
                    stripper.sortByPosition = true
                    stripper.startPage = 1
                    stripper.endPage = minOf(pageCount, maxPages)
                    
                    val text = stripper.getText(doc)
                    
                    if (text.isBlank()) {
                        return@withContext Result.Error("No text found in PDF")
                    }
                    
                    Result.Success(text.trim())
                }
            }
        } catch (e: Exception) {
            Result.Error("Failed to extract text: ${e.message}", e)
        }
    }
}

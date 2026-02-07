package com.yourcompany.itrstatement.domain.parser

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class OcrProcessor {
    
    sealed class Result {
        data class Success(val text: String) : Result()
        data class Error(val message: String, val exception: Throwable? = null) : Result()
    }
    
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    suspend fun recognizeText(context: Context, uri: Uri): Result = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            
            val result = suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text
                        if (text.isBlank()) {
                            continuation.resume(Result.Error("No text recognized in image"))
                        } else {
                            continuation.resume(Result.Success(text.trim()))
                        }
                    }
                    .addOnFailureListener { e ->
                        continuation.resume(Result.Error("OCR failed: ${e.message}", e))
                    }
            }
            
            result
        } catch (e: Exception) {
            Result.Error("Failed to process image: ${e.message}", e)
        }
    }
    
    suspend fun recognizeTextFromMultiplePages(
        context: Context,
        uris: List<Uri>
    ): Result = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            return@withContext Result.Error("No images provided")
        }
        
        try {
            val textBuilder = StringBuilder()
            
            for ((index, uri) in uris.withIndex()) {
                when (val pageResult = recognizeText(context, uri)) {
                    is Result.Success -> {
                        if (index > 0) {
                            textBuilder.append("\n\n--- Page ${index + 1} ---\n\n")
                        }
                        textBuilder.append(pageResult.text)
                    }
                    is Result.Error -> {
                        return@withContext Result.Error(
                            "Failed to process page ${index + 1}: ${pageResult.message}",
                            pageResult.exception
                        )
                    }
                }
            }
            
            val finalText = textBuilder.toString().trim()
            if (finalText.isBlank()) {
                Result.Error("No text recognized from any page")
            } else {
                Result.Success(finalText)
            }
        } catch (e: Exception) {
            Result.Error("Failed to process multiple pages: ${e.message}", e)
        }
    }
    
    fun close() {
        try {
            recognizer.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}

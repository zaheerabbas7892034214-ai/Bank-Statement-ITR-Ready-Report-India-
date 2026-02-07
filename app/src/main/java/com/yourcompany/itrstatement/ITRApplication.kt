package com.yourcompany.itrstatement

import android.app.Application
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class ITRApplication : Application() {
    
    companion object {
        private const val TAG = "ITRApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize PDFBox Android
        try {
            PDFBoxResourceLoader.init(applicationContext)
            Log.d(TAG, "PDFBox initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize PDFBox", e)
        }
        
        Log.d(TAG, "Bank Statement ITR Application started")
    }
}

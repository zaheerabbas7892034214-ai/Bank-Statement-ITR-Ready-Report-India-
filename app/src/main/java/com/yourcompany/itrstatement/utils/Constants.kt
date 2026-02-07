package com.yourcompany.itrstatement.utils

object Constants {
    // Billing
    const val PRODUCT_ID_PRO_YEARLY = "itr_pro_yearly"
    const val BASE_PLAN_ID_YEARLY = "yearly_base"
    
    // Database
    const val DATABASE_NAME = "itr_statement_db"
    
    // Free tier limits
    const val FREE_IMPORT_HISTORY_LIMIT = 5
    const val FREE_PREVIEW_ROWS = 30
    
    // Subscription duration
    const val SUBSCRIPTION_DURATION_MILLIS = 365L * 24 * 60 * 60 * 1000 // 1 year
    
    // Cache duration
    const val ENTITLEMENT_CACHE_DURATION_MILLIS = 60 * 60 * 1000L // 1 hour
    
    // File picker
    const val MIME_TYPE_PDF = "application/pdf"
    const val MIME_TYPE_CSV = "text/csv"
    const val MIME_TYPE_EXCEL = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val MIME_TYPE_JSON = "application/json"
    
    // Export file names
    const val EXPORT_FILE_NAME_CSV = "itr_statement.csv"
    const val EXPORT_FILE_NAME_EXCEL = "itr_statement.xlsx"
    const val EXPORT_FILE_NAME_JSON = "itr_statement.json"
    const val EXPORT_FILE_NAME_PDF = "itr_report.pdf"
    
    // Date formats
    const val DATE_FORMAT_DISPLAY = "dd MMM yyyy"
    const val DATE_FORMAT_EXPORT = "dd/MM/yyyy"
    const val DATE_FORMAT_ISO = "yyyy-MM-dd"
    
    // Currency
    const val CURRENCY_SYMBOL = "₹"
    const val CURRENCY_CODE = "INR"
    
    // App info
    const val APP_VERSION = "1.0.0"
    const val PRIVACY_POLICY_URL = "https://yourcompany.com/privacy"
    const val TERMS_OF_SERVICE_URL = "https://yourcompany.com/terms"
    
    // Parsing
    const val MIN_TRANSACTION_AMOUNT = 0.01
    const val MAX_DESCRIPTION_LENGTH = 200
}

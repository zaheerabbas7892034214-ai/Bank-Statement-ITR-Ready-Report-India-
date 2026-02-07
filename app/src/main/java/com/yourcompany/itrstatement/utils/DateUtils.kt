package com.yourcompany.itrstatement.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val displayFormat = SimpleDateFormat(Constants.DATE_FORMAT_DISPLAY, Locale.getDefault())
    private val exportFormat = SimpleDateFormat(Constants.DATE_FORMAT_EXPORT, Locale.getDefault())
    private val isoFormat = SimpleDateFormat(Constants.DATE_FORMAT_ISO, Locale.getDefault())
    
    fun formatForDisplay(timestamp: Long): String {
        return displayFormat.format(Date(timestamp))
    }
    
    fun formatForExport(timestamp: Long): String {
        return exportFormat.format(Date(timestamp))
    }
    
    fun formatToIso(timestamp: Long): String {
        return isoFormat.format(Date(timestamp))
    }
    
    fun parseDate(dateString: String): Long? {
        val formats = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()),
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()),
            SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )
        
        for (format in formats) {
            try {
                return format.parse(dateString)?.time
            } catch (e: Exception) {
                // Try next format
            }
        }
        return null
    }
    
    fun getCurrentTimestamp(): Long = System.currentTimeMillis()
    
    fun getStartOfMonth(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    fun getEndOfMonth(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    fun getMonthName(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return monthFormat.format(calendar.time)
    }
    
    fun isExpired(expiryTimestamp: Long): Boolean {
        return expiryTimestamp < getCurrentTimestamp()
    }
    
    fun getDaysUntilExpiry(expiryTimestamp: Long): Int {
        val diffMillis = expiryTimestamp - getCurrentTimestamp()
        return (diffMillis / (24 * 60 * 60 * 1000)).toInt()
    }
}

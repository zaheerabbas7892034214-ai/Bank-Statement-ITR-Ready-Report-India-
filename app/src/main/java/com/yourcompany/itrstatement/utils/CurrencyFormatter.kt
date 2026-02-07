package com.yourcompany.itrstatement.utils

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val indianLocale = Locale("en", "IN")
    private val currencyFormat = NumberFormat.getCurrencyInstance(indianLocale)
    private val decimalFormat = DecimalFormat("#,##,##0.00")
    
    init {
        currencyFormat.maximumFractionDigits = 2
        currencyFormat.minimumFractionDigits = 2
    }
    
    fun format(amount: Double): String {
        return currencyFormat.format(amount)
    }
    
    fun formatWithoutSymbol(amount: Double): String {
        return decimalFormat.format(amount)
    }
    
    fun formatCompact(amount: Double): String {
        return when {
            amount >= 10_000_000 -> String.format("%.2fCr", amount / 10_000_000)
            amount >= 100_000 -> String.format("%.2fL", amount / 100_000)
            amount >= 1_000 -> String.format("%.2fK", amount / 1_000)
            else -> format(amount)
        }
    }
    
    fun parseAmount(amountString: String): Double? {
        return try {
            val cleaned = amountString
                .replace(Constants.CURRENCY_SYMBOL, "")
                .replace(",", "")
                .trim()
            cleaned.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    fun formatForExport(amount: Double): String {
        return String.format(Locale.US, "%.2f", amount)
    }
}

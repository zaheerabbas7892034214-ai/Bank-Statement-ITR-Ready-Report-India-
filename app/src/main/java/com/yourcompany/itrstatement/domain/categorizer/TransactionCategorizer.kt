package com.yourcompany.itrstatement.domain.categorizer

import com.yourcompany.itrstatement.data.model.Category
import com.yourcompany.itrstatement.data.model.Transaction
import java.util.Locale

class TransactionCategorizer {
    
    private val categoryKeywords = mapOf(
        Category.INCOME to listOf(
            "salary", "sal", "income", "interest", "refund", "credit", "bonus", 
            "dividend", "cashback", "reward", "reimbursement", "incentive", 
            "commission", "stipend", "payment received", "fund transfer cr"
        ),
        Category.FOOD_DINING to listOf(
            "swiggy", "zomato", "restaurant", "food", "cafe", "dining", "hotel",
            "dominos", "pizza", "mcdonald", "kfc", "burger", "subway", "starbucks",
            "dunkin", "chai", "coffee", "bakery", "eatery", "canteen", "mess"
        ),
        Category.SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "shopping", "retail", "store", "mall",
            "supermarket", "grocery", "bigbasket", "grofers", "reliance", "dmart",
            "lifestyle", "pantaloons", "westside", "max", "shoppers stop", "ajio",
            "meesho", "snapdeal", "ebay", "purchase"
        ),
        Category.BILLS_UTILITIES to listOf(
            "electricity", "power", "water", "gas", "internet", "broadband", "mobile",
            "recharge", "bill", "phone", "telecom", "airtel", "jio", "vodafone", "vi",
            "bsnl", "bses", "adani", "tata power", "utility", "municipal", "corporation"
        ),
        Category.TRAVEL to listOf(
            "uber", "ola", "rapido", "fuel", "petrol", "diesel", "hp", "indian oil",
            "bharat petroleum", "shell", "flight", "airline", "indigo", "spicejet",
            "air india", "goair", "vistara", "train", "irctc", "railway", "bus",
            "redbus", "metro", "toll", "parking", "fastag"
        ),
        Category.HEALTHCARE to listOf(
            "pharmacy", "hospital", "clinic", "doctor", "medical", "health",
            "apollo", "fortis", "max healthcare", "medanta", "manipal", "medicine",
            "pharma", "diagnostic", "lab", "path lab", "dr lal", "thyrocare",
            "insurance", "mediclaim"
        ),
        Category.ENTERTAINMENT to listOf(
            "netflix", "prime", "amazon prime", "hotstar", "disney", "spotify",
            "youtube", "movie", "cinema", "pvr", "inox", "theater", "entertainment",
            "subscription", "gaming", "steam", "playstation", "xbox", "gym",
            "fitness", "sports", "club"
        ),
        Category.TRANSFERS to listOf(
            "upi", "neft", "imps", "rtgs", "transfer", "wallet", "paytm", "phonepe",
            "google pay", "gpay", "bhim", "mobikwik", "freecharge", "fund transfer",
            "self transfer", "to a/c", "from a/c", "sweep"
        ),
        Category.TAXES to listOf(
            "tds", "tax", "advance tax", "gst", "income tax", "it dept", "cbdt",
            "tax payment", "challan", "tin", "pan"
        )
    )
    
    fun categorize(transaction: Transaction): Transaction {
        val description = transaction.description.toLowerCase(Locale.getDefault())
        
        val category = findCategory(description) ?: determineFromAmount(transaction)
        
        return transaction.copy(category = category)
    }
    
    fun categorizeAll(transactions: List<Transaction>): List<Transaction> {
        return transactions.map { categorize(it) }
    }
    
    private fun findCategory(description: String): Category? {
        var bestMatch: Category? = null
        var maxScore = 0
        
        for ((category, keywords) in categoryKeywords) {
            var score = 0
            for (keyword in keywords) {
                if (description.contains(keyword)) {
                    score += keyword.length
                }
            }
            if (score > maxScore) {
                maxScore = score
                bestMatch = category
            }
        }
        
        return if (maxScore > 0) bestMatch else null
    }
    
    private fun determineFromAmount(transaction: Transaction): Category {
        return when {
            transaction.creditAmount > 0 && transaction.debitAmount == 0.0 -> {
                when {
                    transaction.creditAmount >= 10000.0 -> Category.INCOME
                    else -> Category.UNCATEGORIZED
                }
            }
            transaction.debitAmount > 0 && transaction.creditAmount == 0.0 -> {
                Category.UNCATEGORIZED
            }
            else -> Category.UNCATEGORIZED
        }
    }
    
    fun getCategoryStatistics(transactions: List<Transaction>): Map<Category, CategoryStats> {
        val statsMap = mutableMapOf<Category, CategoryStats>()
        
        for (transaction in transactions) {
            val category = transaction.category
            val stats = statsMap.getOrPut(category) {
                CategoryStats(category, 0, 0.0, 0.0)
            }
            
            statsMap[category] = stats.copy(
                count = stats.count + 1,
                totalDebit = stats.totalDebit + transaction.debitAmount,
                totalCredit = stats.totalCredit + transaction.creditAmount
            )
        }
        
        return statsMap
    }
    
    fun recategorize(
        transaction: Transaction,
        newCategory: Category
    ): Transaction {
        return transaction.copy(category = newCategory)
    }
    
    fun suggestCategory(description: String): Category {
        val lower = description.toLowerCase(Locale.getDefault())
        return findCategory(lower) ?: Category.UNCATEGORIZED
    }
    
    data class CategoryStats(
        val category: Category,
        val count: Int,
        val totalDebit: Double,
        val totalCredit: Double
    ) {
        val netAmount: Double
            get() = totalCredit - totalDebit
    }
}

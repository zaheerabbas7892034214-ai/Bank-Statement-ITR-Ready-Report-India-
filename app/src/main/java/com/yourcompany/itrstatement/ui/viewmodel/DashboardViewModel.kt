package com.yourcompany.itrstatement.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.itrstatement.data.model.Category
import com.yourcompany.itrstatement.data.model.Transaction
import com.yourcompany.itrstatement.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Calendar

class DashboardViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    data class MonthlySummary(
        val month: String,
        val year: Int,
        val totalIncome: Double,
        val totalExpense: Double,
        val savings: Double,
        val transactionCount: Int
    )

    data class CategorySummary(
        val category: Category,
        val amount: Double,
        val percentage: Float,
        val transactionCount: Int
    )

    data class MonthlyTrend(
        val monthYear: String,
        val income: Double,
        val expense: Double,
        val savings: Double
    )

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val transactions: List<Transaction>,
            val monthlySummary: MonthlySummary,
            val categoryBreakdown: List<CategorySummary>,
            val monthlyTrends: List<MonthlyTrend>,
            val totalIncome: Double,
            val totalExpense: Double,
            val totalSavings: Double
        ) : UiState()
        object Empty : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var allTransactions: List<Transaction> = emptyList()
    private var startDate: Long? = null
    private var endDate: Long? = null

    init {
        loadDashboardData()
    }

    fun loadDashboardData(sessionId: Long? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                val transactionsFlow = if (sessionId != null) {
                    transactionRepository.getBySession(sessionId)
                } else {
                    transactionRepository.getAll()
                }

                transactionsFlow.catch { exception ->
                    _uiState.value = UiState.Error(
                        exception.message ?: "Failed to load transactions"
                    )
                }.collect { transactions ->
                    allTransactions = transactions

                    if (transactions.isEmpty()) {
                        _uiState.value = UiState.Empty
                    } else {
                        val filteredTransactions = applyDateFilter(transactions)
                        _uiState.value = calculateDashboardData(filteredTransactions)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun filterByDateRange(startDateMillis: Long?, endDateMillis: Long?) {
        startDate = startDateMillis
        endDate = endDateMillis

        if (allTransactions.isEmpty()) {
            return
        }

        val filteredTransactions = applyDateFilter(allTransactions)
        _uiState.value = calculateDashboardData(filteredTransactions)
    }

    fun clearDateFilter() {
        startDate = null
        endDate = null

        if (allTransactions.isNotEmpty()) {
            _uiState.value = calculateDashboardData(allTransactions)
        }
    }

    private fun applyDateFilter(transactions: List<Transaction>): List<Transaction> {
        var filtered = transactions

        if (startDate != null) {
            filtered = filtered.filter { it.date >= startDate!! }
        }

        if (endDate != null) {
            filtered = filtered.filter { it.date <= endDate!! }
        }

        return filtered
    }

    private fun calculateDashboardData(transactions: List<Transaction>): UiState {
        val totalIncome = transactions.sumOf { it.creditAmount }
        val totalExpense = transactions.sumOf { it.debitAmount }
        val totalSavings = totalIncome - totalExpense

        val monthlySummary = calculateMonthlySummary(transactions)
        val categoryBreakdown = calculateCategoryBreakdown(transactions, totalExpense)
        val monthlyTrends = calculateMonthlyTrends(transactions)

        return UiState.Success(
            transactions = transactions.sortedByDescending { it.date },
            monthlySummary = monthlySummary,
            categoryBreakdown = categoryBreakdown,
            monthlyTrends = monthlyTrends,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalSavings = totalSavings
        )
    }

    private fun calculateMonthlySummary(transactions: List<Transaction>): MonthlySummary {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val currentMonthTransactions = transactions.filter { transaction ->
            calendar.timeInMillis = transaction.date
            calendar.get(Calendar.MONTH) == currentMonth &&
            calendar.get(Calendar.YEAR) == currentYear
        }

        val income = currentMonthTransactions.sumOf { it.creditAmount }
        val expense = currentMonthTransactions.sumOf { it.debitAmount }

        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        return MonthlySummary(
            month = monthNames[currentMonth],
            year = currentYear,
            totalIncome = income,
            totalExpense = expense,
            savings = income - expense,
            transactionCount = currentMonthTransactions.size
        )
    }

    private fun calculateCategoryBreakdown(
        transactions: List<Transaction>,
        totalExpense: Double
    ): List<CategorySummary> {
        val expenseTransactions = transactions.filter { it.debitAmount > 0 }

        val categoryMap = expenseTransactions.groupBy { it.category }

        return categoryMap.map { (category, txns) ->
            val amount = txns.sumOf { it.debitAmount }
            val percentage = if (totalExpense > 0) {
                ((amount / totalExpense) * 100).toFloat()
            } else {
                0f
            }

            CategorySummary(
                category = category,
                amount = amount,
                percentage = percentage,
                transactionCount = txns.size
            )
        }.sortedByDescending { it.amount }
    }

    private fun calculateMonthlyTrends(transactions: List<Transaction>): List<MonthlyTrend> {
        val calendar = Calendar.getInstance()

        val monthlyData = transactions.groupBy { transaction ->
            calendar.timeInMillis = transaction.date
            val month = calendar.get(Calendar.MONTH)
            val year = calendar.get(Calendar.YEAR)
            Pair(year, month)
        }

        val monthNames = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        return monthlyData.map { (yearMonth, txns) ->
            val income = txns.sumOf { it.creditAmount }
            val expense = txns.sumOf { it.debitAmount }

            MonthlyTrend(
                monthYear = "${monthNames[yearMonth.second]} ${yearMonth.first}",
                income = income,
                expense = expense,
                savings = income - expense
            )
        }.sortedBy { trend ->
            val parts = trend.monthYear.split(" ")
            val year = parts[1].toInt()
            val month = monthNames.indexOf(parts[0])
            year * 12 + month
        }.takeLast(12)
    }

    fun refreshData() {
        loadDashboardData()
    }
}

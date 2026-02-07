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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TransactionViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    enum class SortOrder {
        DATE_DESC,
        DATE_ASC,
        AMOUNT_DESC,
        AMOUNT_ASC,
        DESCRIPTION_ASC,
        DESCRIPTION_DESC
    }

    data class FilterOptions(
        val searchQuery: String = "",
        val selectedCategories: Set<Category> = emptySet(),
        val startDate: Long? = null,
        val endDate: Long? = null,
        val sortOrder: SortOrder = SortOrder.DATE_DESC
    )

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val transactions: List<Transaction>,
            val filteredTransactions: List<Transaction>,
            val filterOptions: FilterOptions,
            val totalCount: Int,
            val filteredCount: Int
        ) : UiState()
        object Empty : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class UpdateResult {
        object Success : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _updateResult = MutableStateFlow<UpdateResult?>(null)
    val updateResult: StateFlow<UpdateResult?> = _updateResult.asStateFlow()

    private var allTransactions: List<Transaction> = emptyList()
    private var currentFilterOptions = FilterOptions()

    init {
        loadTransactions()
    }

    fun loadTransactions(sessionId: Long? = null) {
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
                        applyFiltersAndUpdate()
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun searchTransactions(query: String) {
        currentFilterOptions = currentFilterOptions.copy(searchQuery = query)
        applyFiltersAndUpdate()
    }

    fun filterByCategories(categories: Set<Category>) {
        currentFilterOptions = currentFilterOptions.copy(selectedCategories = categories)
        applyFiltersAndUpdate()
    }

    fun filterByDateRange(startDate: Long?, endDate: Long?) {
        currentFilterOptions = currentFilterOptions.copy(
            startDate = startDate,
            endDate = endDate
        )
        applyFiltersAndUpdate()
    }

    fun sortTransactions(sortOrder: SortOrder) {
        currentFilterOptions = currentFilterOptions.copy(sortOrder = sortOrder)
        applyFiltersAndUpdate()
    }

    fun clearFilters() {
        currentFilterOptions = FilterOptions()
        applyFiltersAndUpdate()
    }

    fun updateTransactionCategory(transactionId: Long, newCategory: Category) {
        viewModelScope.launch {
            try {
                val transaction = allTransactions.find { it.id == transactionId }

                if (transaction == null) {
                    _updateResult.value = UpdateResult.Error("Transaction not found")
                    return@launch
                }

                val updatedTransaction = transaction.copy(category = newCategory)
                
                transactionRepository.insertAll(listOf(updatedTransaction))

                _updateResult.value = UpdateResult.Success

                loadTransactions()
            } catch (e: Exception) {
                _updateResult.value = UpdateResult.Error(
                    e.message ?: "Failed to update category"
                )
            }
        }
    }

    private fun applyFiltersAndUpdate() {
        var filtered = allTransactions

        if (currentFilterOptions.searchQuery.isNotEmpty()) {
            val query = currentFilterOptions.searchQuery.lowercase()
            filtered = filtered.filter {
                it.description.lowercase().contains(query) ||
                it.category.displayName.lowercase().contains(query)
            }
        }

        if (currentFilterOptions.selectedCategories.isNotEmpty()) {
            filtered = filtered.filter {
                currentFilterOptions.selectedCategories.contains(it.category)
            }
        }

        if (currentFilterOptions.startDate != null) {
            filtered = filtered.filter { it.date >= currentFilterOptions.startDate!! }
        }

        if (currentFilterOptions.endDate != null) {
            filtered = filtered.filter { it.date <= currentFilterOptions.endDate!! }
        }

        filtered = applySorting(filtered, currentFilterOptions.sortOrder)

        _uiState.value = UiState.Success(
            transactions = allTransactions,
            filteredTransactions = filtered,
            filterOptions = currentFilterOptions,
            totalCount = allTransactions.size,
            filteredCount = filtered.size
        )
    }

    private fun applySorting(transactions: List<Transaction>, sortOrder: SortOrder): List<Transaction> {
        return when (sortOrder) {
            SortOrder.DATE_DESC -> transactions.sortedByDescending { it.date }
            SortOrder.DATE_ASC -> transactions.sortedBy { it.date }
            SortOrder.AMOUNT_DESC -> transactions.sortedByDescending {
                maxOf(it.debitAmount, it.creditAmount)
            }
            SortOrder.AMOUNT_ASC -> transactions.sortedBy {
                maxOf(it.debitAmount, it.creditAmount)
            }
            SortOrder.DESCRIPTION_ASC -> transactions.sortedBy { it.description.lowercase() }
            SortOrder.DESCRIPTION_DESC -> transactions.sortedByDescending { it.description.lowercase() }
        }
    }

    fun clearUpdateResult() {
        _updateResult.value = null
    }

    fun refreshTransactions() {
        loadTransactions()
    }

    fun getAvailableCategories(): List<Category> {
        return Category.values().toList()
    }
}

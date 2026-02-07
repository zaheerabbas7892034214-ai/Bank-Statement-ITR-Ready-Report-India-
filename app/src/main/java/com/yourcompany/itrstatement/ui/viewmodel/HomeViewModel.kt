package com.yourcompany.itrstatement.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.itrstatement.billing.EntitlementManager
import com.yourcompany.itrstatement.data.model.ImportSession
import com.yourcompany.itrstatement.data.repository.ImportRepository
import com.yourcompany.itrstatement.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HomeViewModel(
    private val importRepository: ImportRepository,
    private val transactionRepository: TransactionRepository,
    private val entitlementManager: EntitlementManager
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val importHistory: List<ImportSession>,
            val isProActive: Boolean,
            val totalImports: Int
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class NavigationEvent {
        object NavigateToImport : NavigationEvent()
        data class NavigateToDashboard(val sessionId: Long) : NavigationEvent()
        object NavigateToBilling : NavigationEvent()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            try {
                combine(
                    importRepository.getAll(),
                    entitlementManager.getProStatusFlow()
                ) { imports, isPro ->
                    UiState.Success(
                        importHistory = imports.sortedByDescending { it.importTimestamp },
                        isProActive = isPro,
                        totalImports = imports.size
                    )
                }.catch { exception ->
                    _uiState.value = UiState.Error(
                        exception.message ?: "Failed to load home data"
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun deleteImportHistory(sessionId: Long) {
        viewModelScope.launch {
            try {
                transactionRepository.deleteBySession(sessionId)
                importRepository.delete(sessionId)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    "Failed to delete import: ${e.message}"
                )
            }
        }
    }

    fun navigateToImport() {
        _navigationEvent.value = NavigationEvent.NavigateToImport
    }

    fun navigateToDashboard(sessionId: Long) {
        _navigationEvent.value = NavigationEvent.NavigateToDashboard(sessionId)
    }

    fun navigateToBilling() {
        _navigationEvent.value = NavigationEvent.NavigateToBilling
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    fun refreshData() {
        loadHomeData()
    }
}

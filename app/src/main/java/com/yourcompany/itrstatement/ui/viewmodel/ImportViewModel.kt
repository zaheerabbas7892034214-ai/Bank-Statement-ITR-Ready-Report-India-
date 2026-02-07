package com.yourcompany.itrstatement.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.itrstatement.data.model.ImportSession
import com.yourcompany.itrstatement.data.model.Transaction
import com.yourcompany.itrstatement.domain.usecase.ImportStatementUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImportViewModel(
    private val importStatementUseCase: ImportStatementUseCase
) : ViewModel() {

    sealed class ImportState {
        object Idle : ImportState()
        object Validating : ImportState()
        data class ValidationSuccess(val estimatedTransactions: Int) : ImportState()
        data class ValidationError(val message: String) : ImportState()
        data class Parsing(val progress: Int) : ImportState()
        data class Categorizing(val progress: Int, val total: Int) : ImportState()
        data class Success(
            val importSession: ImportSession,
            val transactionCount: Int
        ) : ImportState()
        data class PartialSuccess(
            val importSession: ImportSession,
            val transactionCount: Int,
            val errors: String
        ) : ImportState()
        data class Error(val message: String) : ImportState()
    }

    sealed class NavigationEvent {
        data class NavigateToDashboard(val sessionId: Long) : NavigationEvent()
        object NavigateBack : NavigationEvent()
    }

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    fun validateFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _importState.value = ImportState.Validating

                val result = importStatementUseCase.validateImport(context, uri)

                _importState.value = when (result) {
                    is ImportStatementUseCase.ValidationResult.Valid -> {
                        ImportState.ValidationSuccess(result.estimatedTransactions)
                    }
                    is ImportStatementUseCase.ValidationResult.Invalid -> {
                        ImportState.ValidationError(result.reason)
                    }
                }
            } catch (e: Exception) {
                _importState.value = ImportState.ValidationError(
                    e.message ?: "Validation failed"
                )
            }
        }
    }

    fun importStatement(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            try {
                _importState.value = ImportState.Parsing(0)

                val result = importStatementUseCase.importStatement(
                    context = context,
                    uri = uri,
                    fileName = fileName
                )

                _importState.value = when (result) {
                    is ImportStatementUseCase.Result.Success -> {
                        ImportState.Success(
                            importSession = result.importSession,
                            transactionCount = result.transactions.size
                        )
                    }
                    is ImportStatementUseCase.Result.Partial -> {
                        ImportState.PartialSuccess(
                            importSession = result.importSession,
                            transactionCount = result.transactions.size,
                            errors = result.errors
                        )
                    }
                    is ImportStatementUseCase.Result.Error -> {
                        ImportState.Error(result.message)
                    }
                }

                if (result is ImportStatementUseCase.Result.Success ||
                    result is ImportStatementUseCase.Result.Partial) {
                    val sessionId = when (result) {
                        is ImportStatementUseCase.Result.Success -> result.importSession.id
                        is ImportStatementUseCase.Result.Partial -> result.importSession.id
                        else -> 0L
                    }
                    if (sessionId > 0) {
                        _navigationEvent.value = NavigationEvent.NavigateToDashboard(sessionId)
                    }
                }
            } catch (e: Exception) {
                _importState.value = ImportState.Error(
                    e.message ?: "Import failed"
                )
            }
        }
    }

    fun updateParsingProgress(progress: Int) {
        if (_importState.value is ImportState.Parsing) {
            _importState.value = ImportState.Parsing(progress)
        }
    }

    fun updateCategorizingProgress(current: Int, total: Int) {
        _importState.value = ImportState.Categorizing(current, total)
    }

    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    fun resetState() {
        _importState.value = ImportState.Idle
    }
}

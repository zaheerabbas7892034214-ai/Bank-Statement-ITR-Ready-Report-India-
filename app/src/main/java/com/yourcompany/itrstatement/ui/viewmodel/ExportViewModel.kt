package com.yourcompany.itrstatement.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.itrstatement.billing.EntitlementManager
import com.yourcompany.itrstatement.data.model.Transaction
import com.yourcompany.itrstatement.domain.usecase.ExportDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExportViewModel(
    private val exportDataUseCase: ExportDataUseCase,
    private val entitlementManager: EntitlementManager
) : ViewModel() {

    sealed class ExportState {
        object Idle : ExportState()
        object CheckingEntitlement : ExportState()
        data class Ready(
            val isProActive: Boolean,
            val availableFormats: List<ExportDataUseCase.ExportFormat>
        ) : ExportState()
        object Exporting : ExportState()
        data class Success(val fileName: String, val uri: Uri) : ExportState()
        data class Error(val message: String) : ExportState()
        data class ProRequired(val message: String) : ExportState()
    }

    sealed class PreviewState {
        object Idle : PreviewState()
        data class Available(
            val previewTransactions: List<Transaction>,
            val totalCount: Int,
            val isLimited: Boolean
        ) : PreviewState()
    }

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _previewState = MutableStateFlow<PreviewState>(PreviewState.Idle)
    val previewState: StateFlow<PreviewState> = _previewState.asStateFlow()

    private var cachedTransactions: List<Transaction> = emptyList()

    init {
        checkEntitlementStatus()
    }

    private fun checkEntitlementStatus() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.CheckingEntitlement

                val isProActive = entitlementManager.isProActive()
                val availableFormats = exportDataUseCase.getSupportedFormats(isProActive)

                _exportState.value = ExportState.Ready(
                    isProActive = isProActive,
                    availableFormats = availableFormats
                )
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(
                    "Failed to check Pro status: ${e.message}"
                )
            }
        }
    }

    fun prepareExport(transactions: List<Transaction>) {
        cachedTransactions = transactions
        generatePreview(transactions)
        checkEntitlementStatus()
    }

    private fun generatePreview(transactions: List<Transaction>) {
        viewModelScope.launch {
            try {
                val isProActive = entitlementManager.isProActive()

                if (!isProActive && transactions.size > 30) {
                    _previewState.value = PreviewState.Available(
                        previewTransactions = transactions.take(30),
                        totalCount = transactions.size,
                        isLimited = true
                    )
                } else {
                    _previewState.value = PreviewState.Available(
                        previewTransactions = transactions,
                        totalCount = transactions.size,
                        isLimited = false
                    )
                }
            } catch (e: Exception) {
                _previewState.value = PreviewState.Idle
            }
        }
    }

    fun exportToFile(
        context: Context,
        transactions: List<Transaction>,
        format: ExportDataUseCase.ExportFormat,
        destinationUri: Uri
    ) {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Exporting

                val result = exportDataUseCase.exportData(
                    context = context,
                    transactions = transactions,
                    format = format,
                    destinationUri = destinationUri
                )

                _exportState.value = when (result) {
                    is ExportDataUseCase.Result.Success -> {
                        ExportState.Success(result.fileName, result.uri)
                    }
                    is ExportDataUseCase.Result.Error -> {
                        ExportState.Error(result.message)
                    }
                    is ExportDataUseCase.Result.ProRequired -> {
                        ExportState.ProRequired(result.message)
                    }
                }
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(
                    "Export failed: ${e.message}"
                )
            }
        }
    }

    fun exportToDefaultLocation(
        context: Context,
        transactions: List<Transaction>,
        format: ExportDataUseCase.ExportFormat,
        fileName: String
    ) {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Exporting

                val result = exportDataUseCase.exportToDefaultLocation(
                    context = context,
                    transactions = transactions,
                    format = format,
                    fileName = fileName
                )

                _exportState.value = when (result) {
                    is ExportDataUseCase.Result.Success -> {
                        ExportState.Success(result.fileName, result.uri)
                    }
                    is ExportDataUseCase.Result.Error -> {
                        ExportState.Error(result.message)
                    }
                    is ExportDataUseCase.Result.ProRequired -> {
                        ExportState.ProRequired(result.message)
                    }
                }
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(
                    "Export failed: ${e.message}"
                )
            }
        }
    }

    fun canExportFormat(format: ExportDataUseCase.ExportFormat) {
        viewModelScope.launch {
            val canExport = exportDataUseCase.canExport(format)

            if (!canExport) {
                _exportState.value = ExportState.ProRequired(
                    "Pro version required for ${format.name} export"
                )
            }
        }
    }

    fun getFormatDescription(format: ExportDataUseCase.ExportFormat): String {
        return exportDataUseCase.getFormatDescription(format)
    }

    fun getFormatExtension(format: ExportDataUseCase.ExportFormat): String {
        return exportDataUseCase.getFormatExtension(format)
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    fun resetPreviewState() {
        _previewState.value = PreviewState.Idle
    }

    fun refreshEntitlementStatus() {
        checkEntitlementStatus()
    }
}

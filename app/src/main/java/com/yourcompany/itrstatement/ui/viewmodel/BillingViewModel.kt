package com.yourcompany.itrstatement.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.yourcompany.itrstatement.billing.BillingManager
import com.yourcompany.itrstatement.billing.EntitlementManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingViewModel(
    private val billingManager: BillingManager,
    private val entitlementManager: EntitlementManager
) : ViewModel() {

    data class ProductInfo(
        val productId: String,
        val name: String,
        val description: String,
        val price: String,
        val priceCurrencyCode: String
    )

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Connecting : UiState()
        data class Ready(
            val productInfo: ProductInfo?,
            val isSubscribed: Boolean,
            val subscriptionDetails: SubscriptionDetails?
        ) : UiState()
        object Purchasing : UiState()
        data class PurchaseSuccess(val message: String) : UiState()
        data class PurchaseError(val message: String) : UiState()
        object ProductNotAvailable : UiState()
        data class Error(val message: String) : UiState()
    }

    data class SubscriptionDetails(
        val isActive: Boolean,
        val expiryTimestamp: Long?,
        val daysUntilExpiry: Long?,
        val isInGracePeriod: Boolean
    )

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentProductDetails: ProductDetails? = null

    init {
        observeBillingState()
        observeEntitlementState()
        initializeBilling()
    }

    private fun observeBillingState() {
        viewModelScope.launch {
            billingManager.billingState.collect { state ->
                when (state) {
                    is BillingManager.BillingState.Disconnected -> {
                        if (_uiState.value !is UiState.Ready) {
                            _uiState.value = UiState.Idle
                        }
                    }
                    is BillingManager.BillingState.Connecting -> {
                        _uiState.value = UiState.Connecting
                    }
                    is BillingManager.BillingState.Connected -> {
                        _uiState.value = UiState.Loading
                    }
                    is BillingManager.BillingState.ProductDetailsLoaded -> {
                        currentProductDetails = state.details
                        updateReadyState()
                    }
                    is BillingManager.BillingState.ProductDetailsNotFound -> {
                        _uiState.value = UiState.ProductNotAvailable
                    }
                    is BillingManager.BillingState.PurchasePending -> {
                        _uiState.value = UiState.Purchasing
                    }
                    is BillingManager.BillingState.PurchaseSuccess -> {
                        _uiState.value = UiState.PurchaseSuccess(
                            "Subscription activated successfully!"
                        )
                        updateReadyState()
                    }
                    is BillingManager.BillingState.PurchaseError -> {
                        _uiState.value = UiState.PurchaseError(state.message)
                    }
                    is BillingManager.BillingState.ActiveSubscription -> {
                        updateReadyState()
                    }
                    is BillingManager.BillingState.NoActiveSubscription -> {
                        updateReadyState()
                    }
                    is BillingManager.BillingState.Error -> {
                        _uiState.value = UiState.Error(state.message)
                    }
                }
            }
        }
    }

    private fun observeEntitlementState() {
        viewModelScope.launch {
            entitlementManager.entitlementState.collect { state ->
                when (state) {
                    is EntitlementManager.EntitlementState.Active,
                    is EntitlementManager.EntitlementState.Inactive,
                    is EntitlementManager.EntitlementState.Expired,
                    is EntitlementManager.EntitlementState.GracePeriod -> {
                        updateReadyState()
                    }
                    is EntitlementManager.EntitlementState.Loading -> {
                    }
                }
            }
        }
    }

    private fun initializeBilling() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Connecting
                billingManager.startConnection()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    "Failed to initialize billing: ${e.message}"
                )
            }
        }
    }

    private fun updateReadyState() {
        viewModelScope.launch {
            try {
                val entitlementInfo = entitlementManager.getEntitlementInfo()

                val productInfo = currentProductDetails?.let { details ->
                    val offerDetails = details.subscriptionOfferDetails?.firstOrNull()
                    val pricingPhase = offerDetails?.pricingPhases?.pricingPhaseList?.firstOrNull()

                    ProductInfo(
                        productId = details.productId,
                        name = details.name,
                        description = details.description,
                        price = pricingPhase?.formattedPrice ?: "N/A",
                        priceCurrencyCode = pricingPhase?.priceCurrencyCode ?: ""
                    )
                }

                val subscriptionDetails = SubscriptionDetails(
                    isActive = entitlementInfo.isActive,
                    expiryTimestamp = entitlementInfo.expiryTimestamp,
                    daysUntilExpiry = entitlementInfo.daysUntilExpiry,
                    isInGracePeriod = entitlementInfo.isInGracePeriod
                )

                _uiState.value = UiState.Ready(
                    productInfo = productInfo,
                    isSubscribed = entitlementInfo.isActive,
                    subscriptionDetails = subscriptionDetails
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    "Failed to load subscription info: ${e.message}"
                )
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        viewModelScope.launch {
            try {
                if (!billingManager.isConnected()) {
                    _uiState.value = UiState.Error("Billing not ready. Please try again.")
                    return@launch
                }

                _uiState.value = UiState.Purchasing
                billingManager.launchPurchaseFlow(activity)
            } catch (e: Exception) {
                _uiState.value = UiState.PurchaseError(
                    "Failed to start purchase: ${e.message}"
                )
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                billingManager.restorePurchases()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    "Failed to restore purchases: ${e.message}"
                )
            }
        }
    }

    fun checkSubscriptionStatus() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                billingManager.queryActivePurchases()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    "Failed to check subscription: ${e.message}"
                )
            }
        }
    }

    fun refreshBillingState() {
        viewModelScope.launch {
            try {
                if (!billingManager.isConnected()) {
                    initializeBilling()
                } else {
                    checkSubscriptionStatus()
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    "Failed to refresh: ${e.message}"
                )
            }
        }
    }

    fun clearPurchaseResult() {
        if (_uiState.value is UiState.PurchaseSuccess || _uiState.value is UiState.PurchaseError) {
            updateReadyState()
        }
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.endConnection()
    }
}

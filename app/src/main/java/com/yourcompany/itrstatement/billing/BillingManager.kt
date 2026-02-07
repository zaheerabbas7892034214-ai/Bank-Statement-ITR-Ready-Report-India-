package com.yourcompany.itrstatement.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingManager(
    private val context: Context,
    private val onPurchaseCompleted: (Purchase) -> Unit,
    private val onPurchaseError: (String) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_ID_PRO_YEARLY = "itr_pro_yearly"
        const val BASE_PLAN_ID = "yearly_base"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null

    sealed class BillingState {
        object Disconnected : BillingState()
        object Connecting : BillingState()
        object Connected : BillingState()
        data class Error(val message: String) : BillingState()
        object PurchasePending : BillingState()
        data class PurchaseSuccess(val purchase: Purchase) : BillingState()
        data class PurchaseError(val message: String) : BillingState()
        data class ProductDetailsLoaded(val details: ProductDetails) : BillingState()
        object ProductDetailsNotFound : BillingState()
        data class ActiveSubscription(val purchase: Purchase) : BillingState()
        object NoActiveSubscription : BillingState()
    }

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Disconnected)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val billingClientStateListener = object : BillingClientStateListener {
        override fun onBillingServiceDisconnected() {
            Log.d(TAG, "Billing service disconnected")
            _billingState.value = BillingState.Disconnected
        }

        override fun onBillingSetupFinished(billingResult: BillingResult) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Billing setup finished successfully")
                _billingState.value = BillingState.Connected
                queryProductDetails()
                queryActivePurchases()
            } else {
                Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                _billingState.value = BillingState.Error(
                    billingResult.debugMessage ?: "Unknown billing error"
                )
            }
        }
    }

    fun startConnection() {
        if (billingClient?.isReady == true) {
            Log.d(TAG, "Billing client already connected")
            _billingState.value = BillingState.Connected
            return
        }

        _billingState.value = BillingState.Connecting
        
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(billingClientStateListener)
    }

    fun endConnection() {
        try {
            billingClient?.endConnection()
            billingClient = null
            _billingState.value = BillingState.Disconnected
            Log.d(TAG, "Billing connection ended")
        } catch (e: Exception) {
            Log.e(TAG, "Error ending billing connection", e)
        }
    }

    private fun queryProductDetails() {
        scope.launch {
            try {
                val client = billingClient
                if (client == null || !client.isReady) {
                    Log.e(TAG, "Billing client not ready")
                    _billingState.value = BillingState.Error("Billing client not ready")
                    return@launch
                }

                val productList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID_PRO_YEARLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )

                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()

                val result = withContext(Dispatchers.IO) {
                    client.queryProductDetails(params)
                }

                val productDetailsList = result.productDetailsList
                
                if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    if (productDetailsList.isNullOrEmpty()) {
                        Log.w(TAG, "Product details not found")
                        _billingState.value = BillingState.ProductDetailsNotFound
                    } else {
                        productDetails = productDetailsList[0]
                        Log.d(TAG, "Product details loaded: ${productDetails?.productId}")
                        _billingState.value = BillingState.ProductDetailsLoaded(productDetails!!)
                    }
                } else {
                    Log.e(TAG, "Failed to query product details: ${result.billingResult.debugMessage}")
                    _billingState.value = BillingState.Error(
                        result.billingResult.debugMessage ?: "Failed to load product details"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception querying product details", e)
                _billingState.value = BillingState.Error("Error loading product details: ${e.message}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        scope.launch {
            try {
                val client = billingClient
                if (client == null || !client.isReady) {
                    Log.e(TAG, "Billing client not ready for purchase")
                    _billingState.value = BillingState.PurchaseError("Billing not ready")
                    onPurchaseError("Billing system not ready. Please try again.")
                    return@launch
                }

                val details = productDetails
                if (details == null) {
                    Log.e(TAG, "Product details not available")
                    _billingState.value = BillingState.PurchaseError("Product not available")
                    onPurchaseError("Product not available. Please try again later.")
                    return@launch
                }

                val subscriptionOfferDetails = details.subscriptionOfferDetails
                if (subscriptionOfferDetails.isNullOrEmpty()) {
                    Log.e(TAG, "No subscription offers available")
                    _billingState.value = BillingState.PurchaseError("No offers available")
                    onPurchaseError("No subscription offers available.")
                    return@launch
                }

                val offerToken = subscriptionOfferDetails.find { 
                    it.basePlanId == BASE_PLAN_ID 
                }?.offerToken ?: subscriptionOfferDetails[0].offerToken

                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                val billingResult = withContext(Dispatchers.Main) {
                    client.launchBillingFlow(activity, billingFlowParams)
                }

                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "Failed to launch billing flow: ${billingResult.debugMessage}")
                    _billingState.value = BillingState.PurchaseError(
                        billingResult.debugMessage ?: "Failed to start purchase"
                    )
                    onPurchaseError("Failed to start purchase: ${billingResult.debugMessage}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception launching purchase flow", e)
                _billingState.value = BillingState.PurchaseError("Error: ${e.message}")
                onPurchaseError("Purchase error: ${e.message}")
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                } else {
                    Log.w(TAG, "Purchases list is null")
                    _billingState.value = BillingState.NoActiveSubscription
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled purchase")
                _billingState.value = BillingState.PurchaseError("Purchase canceled")
                onPurchaseError("Purchase canceled by user")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned, restoring purchases")
                queryActivePurchases()
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                _billingState.value = BillingState.PurchaseError(
                    billingResult.debugMessage ?: "Purchase failed"
                )
                onPurchaseError("Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        scope.launch {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    Log.d(TAG, "Purchase completed: ${purchase.products}")
                    
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    } else {
                        _billingState.value = BillingState.PurchaseSuccess(purchase)
                        onPurchaseCompleted(purchase)
                    }
                }
                Purchase.PurchaseState.PENDING -> {
                    Log.d(TAG, "Purchase pending: ${purchase.products}")
                    _billingState.value = BillingState.PurchasePending
                }
                Purchase.PurchaseState.UNSPECIFIED_STATE -> {
                    Log.w(TAG, "Purchase in unspecified state")
                    _billingState.value = BillingState.PurchaseError("Purchase state unknown")
                    onPurchaseError("Purchase state is unknown. Please contact support.")
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        scope.launch {
            try {
                val client = billingClient
                if (client == null || !client.isReady) {
                    Log.e(TAG, "Billing client not ready for acknowledgement")
                    return@launch
                }

                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                val result = withContext(Dispatchers.IO) {
                    client.acknowledgePurchase(params)
                }

                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged successfully")
                    _billingState.value = BillingState.PurchaseSuccess(purchase)
                    onPurchaseCompleted(purchase)
                } else {
                    Log.e(TAG, "Failed to acknowledge purchase: ${result.debugMessage}")
                    _billingState.value = BillingState.PurchaseError(
                        "Failed to acknowledge: ${result.debugMessage}"
                    )
                    onPurchaseError("Failed to verify purchase: ${result.debugMessage}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception acknowledging purchase", e)
                _billingState.value = BillingState.PurchaseError("Error verifying purchase: ${e.message}")
                onPurchaseError("Error verifying purchase: ${e.message}")
            }
        }
    }

    fun queryActivePurchases() {
        scope.launch {
            try {
                val client = billingClient
                if (client == null || !client.isReady) {
                    Log.e(TAG, "Billing client not ready for purchase query")
                    return@launch
                }

                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()

                val result = withContext(Dispatchers.IO) {
                    client.queryPurchasesAsync(params)
                }

                if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val purchases = result.purchasesList
                    
                    if (purchases.isNotEmpty()) {
                        val activePurchase = purchases.firstOrNull { purchase ->
                            purchase.products.contains(PRODUCT_ID_PRO_YEARLY) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                        }

                        if (activePurchase != null) {
                            Log.d(TAG, "Active subscription found")
                            _billingState.value = BillingState.ActiveSubscription(activePurchase)
                            onPurchaseCompleted(activePurchase)
                        } else {
                            Log.d(TAG, "No active subscription found")
                            _billingState.value = BillingState.NoActiveSubscription
                        }
                    } else {
                        Log.d(TAG, "No purchases found")
                        _billingState.value = BillingState.NoActiveSubscription
                    }
                } else {
                    Log.e(TAG, "Failed to query purchases: ${result.billingResult.debugMessage}")
                    _billingState.value = BillingState.Error(
                        result.billingResult.debugMessage ?: "Failed to query purchases"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception querying purchases", e)
                _billingState.value = BillingState.Error("Error checking purchases: ${e.message}")
            }
        }
    }

    fun restorePurchases() {
        Log.d(TAG, "Restoring purchases")
        queryActivePurchases()
    }

    fun isConnected(): Boolean = billingClient?.isReady == true
}

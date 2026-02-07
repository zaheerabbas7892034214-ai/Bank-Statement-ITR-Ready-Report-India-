package com.yourcompany.itrstatement.billing

import android.util.Log
import com.android.billingclient.api.Purchase
import com.yourcompany.itrstatement.data.local.entities.EntitlementEntity
import com.yourcompany.itrstatement.data.repository.EntitlementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class EntitlementManager(
    private val entitlementRepository: EntitlementRepository
) {

    companion object {
        private const val TAG = "EntitlementManager"
        private val CACHE_VALIDITY_DURATION = TimeUnit.HOURS.toMillis(1)
        private val GRACE_PERIOD_DURATION = TimeUnit.DAYS.toMillis(3)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    sealed class EntitlementState {
        object Loading : EntitlementState()
        data class Active(val expiryTimestamp: Long?) : EntitlementState()
        object Inactive : EntitlementState()
        data class Expired(val expiredAt: Long) : EntitlementState()
        data class GracePeriod(val expiryTimestamp: Long, val gracePeriodEnd: Long) : EntitlementState()
    }

    private val _entitlementState = MutableStateFlow<EntitlementState>(EntitlementState.Loading)
    val entitlementState: StateFlow<EntitlementState> = _entitlementState.asStateFlow()

    init {
        observeEntitlementChanges()
    }

    private fun observeEntitlementChanges() {
        scope.launch {
            entitlementRepository.get().collect { entity ->
                updateEntitlementState(entity)
            }
        }
    }

    private fun updateEntitlementState(entity: EntitlementEntity?) {
        if (entity == null) {
            _entitlementState.value = EntitlementState.Inactive
            return
        }

        val currentTime = System.currentTimeMillis()
        
        when {
            !entity.isProActive -> {
                if (entity.subscriptionExpiryTimestamp != null) {
                    _entitlementState.value = EntitlementState.Expired(entity.subscriptionExpiryTimestamp)
                } else {
                    _entitlementState.value = EntitlementState.Inactive
                }
            }
            entity.subscriptionExpiryTimestamp == null -> {
                _entitlementState.value = EntitlementState.Active(null)
            }
            entity.subscriptionExpiryTimestamp > currentTime -> {
                _entitlementState.value = EntitlementState.Active(entity.subscriptionExpiryTimestamp)
            }
            else -> {
                val gracePeriodEnd = entity.subscriptionExpiryTimestamp + GRACE_PERIOD_DURATION
                if (currentTime <= gracePeriodEnd) {
                    _entitlementState.value = EntitlementState.GracePeriod(
                        entity.subscriptionExpiryTimestamp,
                        gracePeriodEnd
                    )
                } else {
                    _entitlementState.value = EntitlementState.Expired(entity.subscriptionExpiryTimestamp)
                }
            }
        }
    }

    suspend fun isProActive(): Boolean = withContext(Dispatchers.IO) {
        try {
            val entity = entitlementRepository.get().first()
            
            if (entity == null) {
                Log.d(TAG, "No entitlement entity found")
                return@withContext false
            }

            if (!entity.isProActive) {
                Log.d(TAG, "Pro status is inactive")
                return@withContext false
            }

            val currentTime = System.currentTimeMillis()

            if (isCacheValid(entity.lastCheckedTimestamp, currentTime)) {
                Log.d(TAG, "Using cached Pro status: active")
                return@withContext validateSubscriptionExpiry(entity, currentTime)
            }

            Log.d(TAG, "Cache expired, needs refresh")
            return@withContext validateSubscriptionExpiry(entity, currentTime)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Pro status", e)
            return@withContext false
        }
    }

    fun getProStatusFlow(): Flow<Boolean> {
        return entitlementRepository.get().map { entity ->
            if (entity == null) return@map false
            
            if (!entity.isProActive) return@map false
            
            val currentTime = System.currentTimeMillis()
            validateSubscriptionExpiry(entity, currentTime)
        }
    }

    private fun isCacheValid(lastCheckedTimestamp: Long, currentTime: Long): Boolean {
        return (currentTime - lastCheckedTimestamp) < CACHE_VALIDITY_DURATION
    }

    private fun validateSubscriptionExpiry(entity: EntitlementEntity, currentTime: Long): Boolean {
        val expiryTimestamp = entity.subscriptionExpiryTimestamp ?: return entity.isProActive

        return when {
            expiryTimestamp > currentTime -> {
                Log.d(TAG, "Subscription active until ${expiryTimestamp}")
                true
            }
            currentTime <= expiryTimestamp + GRACE_PERIOD_DURATION -> {
                Log.d(TAG, "Subscription in grace period")
                true
            }
            else -> {
                Log.d(TAG, "Subscription expired at ${expiryTimestamp}")
                false
            }
        }
    }

    suspend fun grantEntitlement(purchase: Purchase) = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val expiryTimestamp = calculateExpiryTimestamp(purchase)

            Log.d(TAG, "Granting entitlement for purchase: ${purchase.products}")
            
            val existingEntity = entitlementRepository.get().first()
            
            if (existingEntity == null) {
                entitlementRepository.insert(
                    isProActive = true,
                    subscriptionExpiryTimestamp = expiryTimestamp,
                    purchaseToken = purchase.purchaseToken,
                    lastCheckedTimestamp = currentTime
                )
                Log.d(TAG, "Entitlement inserted")
            } else {
                entitlementRepository.update(
                    isProActive = true,
                    subscriptionExpiryTimestamp = expiryTimestamp,
                    purchaseToken = purchase.purchaseToken,
                    lastCheckedTimestamp = currentTime
                )
                Log.d(TAG, "Entitlement updated")
            }
            
            _entitlementState.value = EntitlementState.Active(expiryTimestamp)
        } catch (e: Exception) {
            Log.e(TAG, "Error granting entitlement", e)
            throw e
        }
    }

    private fun calculateExpiryTimestamp(purchase: Purchase): Long? {
        return try {
            val purchaseTime = purchase.purchaseTime
            val oneYearInMillis = TimeUnit.DAYS.toMillis(365)
            purchaseTime + oneYearInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating expiry timestamp", e)
            null
        }
    }

    suspend fun revokeEntitlement(reason: String = "Unknown") = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            
            Log.d(TAG, "Revoking entitlement. Reason: $reason")
            
            val existingEntity = entitlementRepository.get().first()
            
            if (existingEntity != null) {
                entitlementRepository.update(
                    isProActive = false,
                    subscriptionExpiryTimestamp = existingEntity.subscriptionExpiryTimestamp,
                    purchaseToken = null,
                    lastCheckedTimestamp = currentTime
                )
                Log.d(TAG, "Entitlement revoked")
            }
            
            _entitlementState.value = if (existingEntity?.subscriptionExpiryTimestamp != null) {
                EntitlementState.Expired(existingEntity.subscriptionExpiryTimestamp)
            } else {
                EntitlementState.Inactive
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error revoking entitlement", e)
            throw e
        }
    }

    suspend fun refreshEntitlementStatus(purchase: Purchase?) = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            
            if (purchase == null) {
                Log.d(TAG, "No active purchase, checking if entitlement should be revoked")
                val existingEntity = entitlementRepository.get().first()
                
                if (existingEntity?.isProActive == true) {
                    val shouldRevoke = existingEntity.subscriptionExpiryTimestamp?.let { expiry ->
                        currentTime > expiry + GRACE_PERIOD_DURATION
                    } ?: false
                    
                    if (shouldRevoke) {
                        revokeEntitlement("Subscription expired and no active purchase")
                    }
                }
                return@withContext
            }

            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                grantEntitlement(purchase)
            } else {
                Log.d(TAG, "Purchase state is not PURCHASED: ${purchase.purchaseState}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing entitlement status", e)
        }
    }

    suspend fun validateAndUpdateEntitlement() = withContext(Dispatchers.IO) {
        try {
            val entity = entitlementRepository.get().first()
            
            if (entity == null) {
                Log.d(TAG, "No entitlement to validate")
                return@withContext
            }

            val currentTime = System.currentTimeMillis()

            if (!entity.isProActive) {
                Log.d(TAG, "Entitlement already inactive")
                return@withContext
            }

            val expiryTimestamp = entity.subscriptionExpiryTimestamp
            if (expiryTimestamp != null && currentTime > expiryTimestamp + GRACE_PERIOD_DURATION) {
                Log.d(TAG, "Entitlement expired, revoking")
                revokeEntitlement("Subscription expired beyond grace period")
            } else {
                entitlementRepository.update(
                    isProActive = entity.isProActive,
                    subscriptionExpiryTimestamp = entity.subscriptionExpiryTimestamp,
                    purchaseToken = entity.purchaseToken,
                    lastCheckedTimestamp = currentTime
                )
                Log.d(TAG, "Entitlement timestamp updated")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating entitlement", e)
        }
    }

    suspend fun getEntitlementInfo(): EntitlementInfo = withContext(Dispatchers.IO) {
        try {
            val entity = entitlementRepository.get().first()
            
            if (entity == null) {
                return@withContext EntitlementInfo(
                    isActive = false,
                    expiryTimestamp = null,
                    purchaseToken = null,
                    lastChecked = 0L,
                    daysUntilExpiry = null,
                    isInGracePeriod = false
                )
            }

            val currentTime = System.currentTimeMillis()
            val isActive = entity.isProActive && validateSubscriptionExpiry(entity, currentTime)
            
            val daysUntilExpiry = entity.subscriptionExpiryTimestamp?.let { expiry ->
                val diff = expiry - currentTime
                if (diff > 0) TimeUnit.MILLISECONDS.toDays(diff) else null
            }

            val isInGracePeriod = entity.subscriptionExpiryTimestamp?.let { expiry ->
                currentTime > expiry && currentTime <= expiry + GRACE_PERIOD_DURATION
            } ?: false

            EntitlementInfo(
                isActive = isActive,
                expiryTimestamp = entity.subscriptionExpiryTimestamp,
                purchaseToken = entity.purchaseToken,
                lastChecked = entity.lastCheckedTimestamp,
                daysUntilExpiry = daysUntilExpiry,
                isInGracePeriod = isInGracePeriod
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting entitlement info", e)
            EntitlementInfo(
                isActive = false,
                expiryTimestamp = null,
                purchaseToken = null,
                lastChecked = 0L,
                daysUntilExpiry = null,
                isInGracePeriod = false
            )
        }
    }

    data class EntitlementInfo(
        val isActive: Boolean,
        val expiryTimestamp: Long?,
        val purchaseToken: String?,
        val lastChecked: Long,
        val daysUntilExpiry: Long?,
        val isInGracePeriod: Boolean
    )
}

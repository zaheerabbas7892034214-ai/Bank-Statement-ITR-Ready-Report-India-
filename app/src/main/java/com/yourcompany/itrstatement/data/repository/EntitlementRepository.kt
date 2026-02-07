package com.yourcompany.itrstatement.data.repository

import com.yourcompany.itrstatement.data.local.dao.EntitlementDao
import com.yourcompany.itrstatement.data.local.entities.EntitlementEntity
import kotlinx.coroutines.flow.Flow

class EntitlementRepository(private val entitlementDao: EntitlementDao) {
    
    suspend fun insert(
        isProActive: Boolean,
        subscriptionExpiryTimestamp: Long?,
        purchaseToken: String?,
        lastCheckedTimestamp: Long
    ) {
        val entitlement = EntitlementEntity(
            id = 1,
            isProActive = isProActive,
            subscriptionExpiryTimestamp = subscriptionExpiryTimestamp,
            purchaseToken = purchaseToken,
            lastCheckedTimestamp = lastCheckedTimestamp
        )
        entitlementDao.insert(entitlement)
    }
    
    fun get(): Flow<EntitlementEntity?> {
        return entitlementDao.get()
    }
    
    suspend fun update(
        isProActive: Boolean,
        subscriptionExpiryTimestamp: Long?,
        purchaseToken: String?,
        lastCheckedTimestamp: Long
    ) {
        val entitlement = EntitlementEntity(
            id = 1,
            isProActive = isProActive,
            subscriptionExpiryTimestamp = subscriptionExpiryTimestamp,
            purchaseToken = purchaseToken,
            lastCheckedTimestamp = lastCheckedTimestamp
        )
        entitlementDao.update(entitlement)
    }
    
    suspend fun updateProStatus(isProActive: Boolean, timestamp: Long) {
        val entitlement = EntitlementEntity(
            id = 1,
            isProActive = isProActive,
            subscriptionExpiryTimestamp = null,
            purchaseToken = null,
            lastCheckedTimestamp = timestamp
        )
        entitlementDao.update(entitlement)
    }
}

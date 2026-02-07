package com.yourcompany.itrstatement.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "entitlement")
data class EntitlementEntity(
    @PrimaryKey
    val id: Int = 1,
    
    @ColumnInfo(name = "is_pro_active")
    val isProActive: Boolean,
    
    @ColumnInfo(name = "subscription_expiry_timestamp")
    val subscriptionExpiryTimestamp: Long?,
    
    @ColumnInfo(name = "purchase_token")
    val purchaseToken: String?,
    
    @ColumnInfo(name = "last_checked_timestamp")
    val lastCheckedTimestamp: Long
)

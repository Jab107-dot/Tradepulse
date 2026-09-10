package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AlertCondition(val label: String) {
    CROSSES_ABOVE("Price crosses above"),
    CROSSES_BELOW("Price crosses below"),
    PCT_CHANGE_UP("24h Gain exceeds (%)"),
    PCT_CHANGE_DOWN("24h Drop exceeds (%)"),
    SENTIMENT_GREED("Sentiment enters Greed (>70)"),
    SENTIMENT_FEAR("Sentiment enters Fear (<30)")
}

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val assetId: String,
    val assetSymbol: String,
    val condition: String, // AlertCondition name
    val targetValue: Double,
    val note: String = "",
    val isEnabled: Boolean = true,
    val isTriggered: Boolean = false,
    val triggeredAt: Long? = null,
    val triggeredPrice: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

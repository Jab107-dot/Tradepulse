package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceAlertDao {
    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<PriceAlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE isEnabled = 1 AND isTriggered = 0")
    fun getActiveAlerts(): Flow<List<PriceAlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE id = :id LIMIT 1")
    suspend fun getAlertById(id: Long): PriceAlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlertEntity): Long

    @Update
    suspend fun updateAlert(alert: PriceAlertEntity)

    @Query("UPDATE price_alerts SET isTriggered = 1, triggeredAt = :timestamp, triggeredPrice = :price WHERE id = :id")
    suspend fun markAlertTriggered(id: Long, timestamp: Long, price: Double)

    @Query("UPDATE price_alerts SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setAlertEnabled(id: Long, isEnabled: Boolean)

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Long)

    @Query("DELETE FROM price_alerts WHERE isTriggered = 1")
    suspend fun clearTriggeredAlerts()
}

package com.example.data.local

import kotlinx.coroutines.flow.Flow

class AlertRepository(private val dao: PriceAlertDao) {
    val allAlerts: Flow<List<PriceAlertEntity>> = dao.getAllAlerts()
    val activeAlerts: Flow<List<PriceAlertEntity>> = dao.getActiveAlerts()

    suspend fun createAlert(alert: PriceAlertEntity): Long {
        return dao.insertAlert(alert)
    }

    suspend fun toggleAlert(id: Long, isEnabled: Boolean) {
        dao.setAlertEnabled(id, isEnabled)
    }

    suspend fun markTriggered(id: Long, price: Double) {
        dao.markAlertTriggered(id, System.currentTimeMillis(), price)
    }

    suspend fun deleteAlert(id: Long) {
        dao.deleteAlertById(id)
    }

    suspend fun clearTriggered() {
        dao.clearTriggeredAlerts()
    }
}

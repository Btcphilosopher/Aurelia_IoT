package com.example.data.database

import androidx.room.*
import com.example.data.model.IoTDevice
import com.example.data.model.AutomationRule
import com.example.data.model.TelemetryLog
import com.example.data.model.NotificationAlert
import kotlinx.coroutines.flow.Flow

@Dao
interface IoTDao {

    // --- IoT Devices ---
    @Query("SELECT * FROM iot_devices ORDER BY id DESC")
    fun getAllDevices(): Flow<List<IoTDevice>>

    @Query("SELECT * FROM iot_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDeviceById(deviceId: String): IoTDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: IoTDevice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<IoTDevice>)

    @Update
    suspend fun updateDevice(device: IoTDevice)

    @Query("UPDATE iot_devices SET status = :status WHERE deviceId = :deviceId")
    suspend fun updateDeviceStatus(deviceId: String, status: String)

    @Query("UPDATE iot_devices SET latestTelemetry = :telemetry, status = :status WHERE deviceId = :deviceId")
    suspend fun updateDeviceTelemetry(deviceId: String, telemetry: String, status: String)

    @Query("DELETE FROM iot_devices WHERE deviceId = :deviceId")
    suspend fun deleteDevice(deviceId: String)

    // --- Automation Rules ---
    @Query("SELECT * FROM automation_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<AutomationRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutomationRule)

    @Update
    suspend fun updateRule(rule: AutomationRule)

    @Query("DELETE FROM automation_rules WHERE id = :ruleId")
    suspend fun deleteRule(ruleId: Int)

    // --- Telemetry Logs ---
    @Query("SELECT * FROM telemetry_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllTelemetryLogs(): Flow<List<TelemetryLog>>

    @Query("SELECT * FROM telemetry_logs WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 50")
    fun getTelemetryLogsForDevice(deviceId: String): Flow<List<TelemetryLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetryLog(log: TelemetryLog)

    // --- Notification Alerts ---
    @Query("SELECT * FROM notification_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<NotificationAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: NotificationAlert)

    @Query("UPDATE notification_alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAlertAsRead(alertId: Int)

    @Query("DELETE FROM notification_alerts")
    suspend fun clearAllAlerts()
}

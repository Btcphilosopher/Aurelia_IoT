package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "iot_devices")
data class IoTDevice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: String,
    val name: String,
    val type: String, // "CONSUMER", "INDUSTRIAL", "INFRASTRUCTURE", "VEHICLE", "AGRICULTURE"
    val subType: String, // "Light", "PLC", "Thermostat", "Generator", "Flow Valve", "Traffic Camera"
    val owner: String,
    val firmwareVersion: String,
    val status: String, // "ONLINE", "OFFLINE", "ALERT", "MAINTENANCE"
    val connectivityProfile: String, // "WIFI", "BLE", "ZIGBEE", "Z_WAVE", "LORAWAN", "CELLULAR"
    val securityProfile: String, // "mTLS (RSA-4096)", "Aurelia Signature (ECC)", "Legacy SSH"
    val latestTelemetry: String, // "vibration:0.04mm, temp:42C, load:88%" etc.
    val publicKey: String, // cryptographic verification key
    val isMeshNode: Boolean,
    val meshParentId: String? = null,
    val locationName: String = "Sovereign sector Alpha",
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194
)

@Entity(tableName = "automation_rules")
data class AutomationRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val triggerDeviceId: String,
    val triggerField: String, // "temp", "vibration", "humidity", "moisture", "signal"
    val triggerOperator: String, // ">", "<", "=="
    val triggerValue: Double,
    val targetDeviceId: String,
    val targetAction: String, // "COOLDOWN", "ACTIVATE", "SHUTDOWN", "RESET", "ALERT"
    val isActive: Boolean = true
)

@Entity(tableName = "telemetry_logs")
data class TelemetryLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceId: String,
    val timestamp: Long,
    val metricName: String,
    val value: Double
)

@Entity(tableName = "notification_alerts")
data class NotificationAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: Long,
    val severity: String, // "INFO", "WARNING", "CRITICAL"
    val isRead: Boolean = false
)

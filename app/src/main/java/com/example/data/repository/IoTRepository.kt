package com.example.data.repository

import com.example.data.database.IoTDao
import com.example.data.model.IoTDevice
import com.example.data.model.AutomationRule
import com.example.data.model.TelemetryLog
import com.example.data.model.NotificationAlert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class IoTRepository(private val iotDao: IoTDao) {

    val allDevices: Flow<List<IoTDevice>> = iotDao.getAllDevices()
    val allRules: Flow<List<AutomationRule>> = iotDao.getAllRules()
    val allLogs: Flow<List<TelemetryLog>> = iotDao.getAllTelemetryLogs()
    val allAlerts: Flow<List<NotificationAlert>> = iotDao.getAllAlerts()

    fun getLogsForDevice(deviceId: String): Flow<List<TelemetryLog>> =
        iotDao.getTelemetryLogsForDevice(deviceId)

    suspend fun insertDevice(device: IoTDevice) = iotDao.insertDevice(device)
    suspend fun updateDevice(device: IoTDevice) = iotDao.updateDevice(device)
    suspend fun updateDeviceStatus(deviceId: String, status: String) = iotDao.updateDeviceStatus(deviceId, status)
    suspend fun updateDeviceTelemetry(deviceId: String, telemetry: String, status: String) =
        iotDao.updateDeviceTelemetry(deviceId, telemetry, status)
    suspend fun deleteDevice(deviceId: String) = iotDao.deleteDevice(deviceId)

    suspend fun insertRule(rule: AutomationRule) = iotDao.insertRule(rule)
    suspend fun updateRule(rule: AutomationRule) = iotDao.updateRule(rule)
    suspend fun deleteRule(ruleId: Int) = iotDao.deleteRule(ruleId)

    suspend fun insertTelemetryLog(log: TelemetryLog) = iotDao.insertTelemetryLog(log)

    suspend fun insertAlert(alert: NotificationAlert) = iotDao.insertAlert(alert)
    suspend fun markAlertAsRead(alertId: Int) = iotDao.markAlertAsRead(alertId)
    suspend fun clearAllAlerts() = iotDao.clearAllAlerts()

    suspend fun getDeviceById(deviceId: String): IoTDevice? = iotDao.getDeviceById(deviceId)

    suspend fun checkSeedData() {
        val current = allDevices.first()
        if (current.isEmpty()) {
            val seedDevices = listOf(
                IoTDevice(
                    deviceId = "IND-PLC-14",
                    name = "Bypass Pump Station 14",
                    type = "INDUSTRIAL",
                    subType = "Process Pump PLC",
                    owner = "AURELIA Waterworks",
                    firmwareVersion = "v4.1.20-edge",
                    status = "ONLINE",
                    connectivityProfile = "ZIGBEE",
                    securityProfile = "mTLS (RSA-4096)",
                    latestTelemetry = "vibration:0.04mm, temp:46C, flowRate:210L/m",
                    publicKey = "AureliaPubKey-Ind-14-xyz789",
                    isMeshNode = true,
                    meshParentId = "MESH-GATEWAY",
                    locationName = "Pumping Substation B",
                    latitude = 37.7833,
                    longitude = -122.4167
                ),
                IoTDevice(
                    deviceId = "INF-BAT-G1",
                    name = "Grid Storage Battery Vault",
                    type = "INFRASTRUCTURE",
                    subType = "Sovereign Power Vault",
                    owner = "AURELIA Energy Grid",
                    firmwareVersion = "v2.0.1",
                    status = "ONLINE",
                    connectivityProfile = "WIFI",
                    securityProfile = "Aurelia Signature (ECC)",
                    latestTelemetry = "charge:85%, power_load:12kW, cell_temp:31.2C",
                    publicKey = "AureliaPubKey-Inf-G1-ecc123",
                    isMeshNode = true,
                    meshParentId = "MESH-GATEWAY",
                    locationName = "Main Power Station East",
                    latitude = 37.7715,
                    longitude = -122.4411
                ),
                IoTDevice(
                    deviceId = "INF-WTR-VAL4",
                    name = "Reservoir Outflow Gate 04",
                    type = "INFRASTRUCTURE",
                    subType = "Flow Control Valve",
                    owner = "AURELIA Utilities",
                    firmwareVersion = "v1.12.5",
                    status = "ALERT",
                    connectivityProfile = "LORAWAN",
                    securityProfile = "Aurelia Signature (ECC)",
                    latestTelemetry = "pressure:185psi, gate_opening:94%, leak_detected:true",
                    publicKey = "AureliaPubKey-Inf-Val4-abc777",
                    isMeshNode = true,
                    meshParentId = null,
                    locationName = "Sector 4 Reservoir Spillway",
                    latitude = 37.7950,
                    longitude = -122.3995
                ),
                IoTDevice(
                    deviceId = "VEH-FLEET-08",
                    name = "Logistics Transporter 08",
                    type = "VEHICLE",
                    subType = "Freight Truck Tracker",
                    owner = "AURELIA Transport Hub",
                    firmwareVersion = "v3.1.2",
                    status = "ONLINE",
                    connectivityProfile = "CELLULAR",
                    securityProfile = "mTLS (RSA-4096)",
                    latestTelemetry = "speed:54mph, fuel:68%, cargo_temp:4C",
                    publicKey = "AureliaPubKey-Veh-T08-opt888",
                    isMeshNode = false,
                    locationName = "Highway 101 South",
                    latitude = 37.7554,
                    longitude = -122.4201
                ),
                IoTDevice(
                    deviceId = "AGR-SOIL-P3",
                    name = "Irrigation Soil Probe 03",
                    type = "AGRICULTURE",
                    subType = "Soil Sensor Array",
                    owner = "AURELIA Agritech Sector",
                    firmwareVersion = "v1.0.8",
                    status = "OFFLINE",
                    connectivityProfile = "LORAWAN",
                    securityProfile = "mTLS (RSA-4096)",
                    latestTelemetry = "moisture:14%, soil_temp:18.5C",
                    publicKey = "AureliaPubKey-Agr-Probe3-pqrs55",
                    isMeshNode = true,
                    meshParentId = null,
                    locationName = "Sector D Soy Field",
                    latitude = 37.7650,
                    longitude = -122.4600
                ),
                IoTDevice(
                    deviceId = "CON-LIGHT-01",
                    name = "Command Center Downlight",
                    type = "CONSUMER",
                    subType = "RGBW Controller",
                    owner = "Operator Sovereign Space",
                    firmwareVersion = "v1.0.0",
                    status = "ONLINE",
                    connectivityProfile = "Z_WAVE",
                    securityProfile = "Legacy SSH",
                    latestTelemetry = "brightness:80%, rgb:0xFFD4AF37", // Brass yellow color
                    publicKey = "AureliaPubKey-Con-L1",
                    isMeshNode = true,
                    meshParentId = "MESH-GATEWAY",
                    locationName = "Primary Control Console",
                    latitude = 37.7788,
                    longitude = -122.4111
                ),
                IoTDevice(
                    deviceId = "INF-TRAF-SECTOR5",
                    name = "Interstate Transit Controller",
                    type = "INFRASTRUCTURE",
                    subType = "Smart Sign & Sensor",
                    owner = "AURELIA Transport Hub",
                    firmwareVersion = "v2.5.0",
                    status = "ONLINE",
                    connectivityProfile = "CELLULAR",
                    securityProfile = "mTLS (RSA-4096)",
                    latestTelemetry = "avg_speed:42mph, flow_rate:140cpm",
                    publicKey = "AureliaPubKey-Inf-Traf5",
                    isMeshNode = false,
                    locationName = "Downtown Section 5 Junction",
                    latitude = 37.7891,
                    longitude = -122.4014
                ),
                IoTDevice(
                    deviceId = "AGR-IRRIG-GATE",
                    name = "Sovereign Irrigation Gate 01",
                    type = "AGRICULTURE",
                    subType = "Hydraulic Flow Gate",
                    owner = "AURELIA Agritech Sector",
                    firmwareVersion = "v2.3.1",
                    status = "ONLINE",
                    connectivityProfile = "LORAWAN",
                    securityProfile = "Aurelia Signature (ECC)",
                    latestTelemetry = "valve_status:SHUT, pressure:42psi",
                    publicKey = "AureliaPubKey-Agr-IrrigG1",
                    isMeshNode = true,
                    meshParentId = null,
                    locationName = "North Field Crop Canal",
                    latitude = 37.7601,
                    longitude = -122.4512
                )
            )
            iotDao.insertDevices(seedDevices)

            // Seed some automation rules
            val seedRules = listOf(
                AutomationRule(
                    name = "Automated Overpressure Drain",
                    triggerDeviceId = "INF-WTR-VAL4",
                    triggerField = "pressure",
                    triggerOperator = ">",
                    triggerValue = 150.0,
                    targetDeviceId = "IND-PLC-14",
                    targetAction = "COOLDOWN", // Drain action
                    isActive = true
                ),
                AutomationRule(
                    name = "Soy Field Critical Irrigation",
                    triggerDeviceId = "AGR-SOIL-P3",
                    triggerField = "moisture",
                    triggerOperator = "<",
                    triggerValue = 20.0,
                    targetDeviceId = "AGR-IRRIG-GATE",
                    targetAction = "ACTIVATE", // Open gate
                    isActive = true
                )
            )
            for (rule in seedRules) {
                iotDao.insertRule(rule)
            }

            // Seed alerts
            iotDao.insertAlert(
                NotificationAlert(
                    title = "Pressure Threshold Breach",
                    message = "Reservoir Outflow Gate 04 reading is at 185psi (Threshold: 150psi). Automated bypass active.",
                    timestamp = System.currentTimeMillis() - 3600000,
                    severity = "CRITICAL"
                )
            )
            iotDao.insertAlert(
                NotificationAlert(
                    title = "Device Offline Alarm",
                    message = "Agricultural Soil Probe 03 failed to report back within 15 mins heartbeat window.",
                    timestamp = System.currentTimeMillis() - 1800000,
                    severity = "WARNING"
                )
            )
        }
    }
}

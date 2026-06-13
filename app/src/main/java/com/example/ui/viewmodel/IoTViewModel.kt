package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.AutomationRule
import com.example.data.model.IoTDevice
import com.example.data.model.NotificationAlert
import com.example.data.model.TelemetryLog
import com.example.data.repository.IoTRepository
import com.example.data.api.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class IoTViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IoTRepository
    
    // UI state streams
    val devices: StateFlow<List<IoTDevice>>
    val rules: StateFlow<List<AutomationRule>>
    val logs: StateFlow<List<TelemetryLog>>
    val alerts: StateFlow<List<NotificationAlert>>

    // Selected device for Digital Twin detail view
    private val _selectedDevice = MutableStateFlow<IoTDevice?>(null)
    val selectedDevice: StateFlow<IoTDevice?> = _selectedDevice.asStateFlow()

    // Assistant Chat messages (Role, Content)
    private val _chatMessages = MutableStateFlow<List<Pair<String, String>>>(
        listOf("Assistant" to "AURELIA IoT Assistant initiated. Local databases loaded. Ready for operational inquiries regarding our industrial PLCs, grid networks, or field automation rules.")
    )
    val chatMessages: StateFlow<List<Pair<String, String>>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Active mode toggles (Standard, Industrial, Smart City)
    private val _platformMode = MutableStateFlow("STANDARD") // "STANDARD", "INDUSTRIAL", "SMART_CITY"
    val platformMode: StateFlow<String> = _platformMode.asStateFlow()

    // Simulation active status
    private val _isSimulating = MutableStateFlow(true)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = IoTRepository(database.iotDao())

        devices = repository.allDevices.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        rules = repository.allRules.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        logs = repository.allLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        alerts = repository.allAlerts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.checkSeedData()
            // Default selected device to the initial industrial PLC on startup
            delay(500)
            val currentDevices = devices.value
            if (currentDevices.isNotEmpty()) {
                _selectedDevice.value = currentDevices.firstOrNull { it.deviceId == "IND-PLC-14" } ?: currentDevices.first()
            }
        }

        // Start local simulation engine loop
        startSimulationEngine()
    }

    fun selectDevice(device: IoTDevice) {
        _selectedDevice.value = device
    }

    fun setPlatformMode(mode: String) {
        _platformMode.value = mode
    }

    fun toggleSimulation() {
        _isSimulating.value = !_isSimulating.value
    }

    // --- Device Commands ---
    fun toggleDevicePower(deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val device = repository.getDeviceById(deviceId) ?: return@launch
            val newStatus = if (device.status == "OFFLINE") "ONLINE" else "OFFLINE"
            repository.updateDeviceStatus(deviceId, newStatus)
            
            // Log transaction
            repository.insertTelemetryLog(
                TelemetryLog(
                    deviceId = deviceId,
                    timestamp = System.currentTimeMillis(),
                    metricName = "power_state",
                    value = if (newStatus == "ONLINE") 1.0 else 0.0
                )
            )

            // Dynamic twin update
            if (_selectedDevice.value?.deviceId == deviceId) {
                _selectedDevice.value = _selectedDevice.value?.copy(status = newStatus)
            }
        }
    }

    fun rebootDevice(deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val device = repository.getDeviceById(deviceId) ?: return@launch
            repository.updateDeviceStatus(deviceId, "MAINTENANCE")
            delay(2000)
            repository.updateDeviceStatus(deviceId, "ONLINE")

            repository.insertTelemetryLog(
                TelemetryLog(
                    deviceId = deviceId,
                    timestamp = System.currentTimeMillis(),
                    metricName = "system_reset",
                    value = 1.0
                )
            )

            repository.insertAlert(
                NotificationAlert(
                    title = "System Reboot Authorized",
                    message = "${device.name} ($deviceId) executed soft firmware reboot. Signature authenticated via Aurelia Vault.",
                    timestamp = System.currentTimeMillis(),
                    severity = "INFO"
                )
            )

            if (_selectedDevice.value?.deviceId == deviceId) {
                _selectedDevice.value = _selectedDevice.value?.copy(status = "ONLINE")
            }
        }
    }

    fun updateDeviceDetails(deviceId: String, name: String, location: String, safetyProfile: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val device = repository.getDeviceById(deviceId) ?: return@launch
            val updated = device.copy(
                name = name,
                locationName = location,
                securityProfile = safetyProfile
            )
            repository.updateDevice(updated)
            if (_selectedDevice.value?.deviceId == deviceId) {
                _selectedDevice.value = updated
            }
        }
    }

    fun triggerBulkRestart() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentOnline = devices.value.filter { it.status == "ONLINE" }
            for (dev in currentOnline) {
                repository.updateDeviceStatus(dev.deviceId, "MAINTENANCE")
            }
            delay(1500)
            for (dev in currentOnline) {
                repository.updateDeviceStatus(dev.deviceId, "ONLINE")
                repository.insertTelemetryLog(
                    TelemetryLog(
                        deviceId = dev.deviceId,
                        timestamp = System.currentTimeMillis(),
                        metricName = "cascade_reset",
                        value = 1.0
                    )
                )
            }

            repository.insertAlert(
                NotificationAlert(
                    title = "Bulk Cascade Reboot Complete",
                    message = "Simultaneous cryptographic soft-reboot completed for ${currentOnline.size} online assets.",
                    timestamp = System.currentTimeMillis(),
                    severity = "WARNING"
                )
            )
        }
    }

    fun addDevice(
        deviceId: String,
        name: String,
        type: String,
        subType: String,
        connectivity: String,
        security: String,
        location: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val shortPubKey = "AureliaPubKey-${type.take(3)}-${UUID.randomUUID().toString().take(6)}"
            val dummyDevice = IoTDevice(
                deviceId = deviceId.trim().ifEmpty { "DEV-" + UUID.randomUUID().toString().take(5).uppercase() },
                name = name.trim().ifEmpty { "Generic Sensor" },
                type = type,
                subType = subType.trim().ifEmpty { "Generic" },
                owner = "Local Owner Auth",
                firmwareVersion = "v1.0.0-sovereign",
                status = "ONLINE",
                connectivityProfile = connectivity,
                securityProfile = security,
                latestTelemetry = "status:1",
                publicKey = shortPubKey,
                isMeshNode = true,
                meshParentId = "MESH-GATEWAY",
                locationName = location.trim().ifEmpty { "Sector Main" }
            )

            repository.insertDevice(dummyDevice)
            repository.insertAlert(
                NotificationAlert(
                    title = "New Asset Enrolled",
                    message = "Secure handshake verified. Created digital twin of ${dummyDevice.name} under identifier ${dummyDevice.deviceId}.",
                    timestamp = System.currentTimeMillis(),
                    severity = "INFO"
                )
            )
        }
    }

    fun removeDevice(deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDevice(deviceId)
            if (_selectedDevice.value?.deviceId == deviceId) {
                _selectedDevice.value = null
            }
            repository.insertAlert(
                NotificationAlert(
                    title = "Asset Commission Revoked",
                    message = "Device registry cleared for node ID: $deviceId. Cleared local telemetry bindings.",
                    timestamp = System.currentTimeMillis(),
                    severity = "WARNING"
                )
            )
        }
    }

    // --- Automation Rules ---
    fun addRule(
        name: String,
        triggerDevId: String,
        metric: String,
        op: String,
        value: Double,
        targetDevId: String,
        action: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val freshRule = AutomationRule(
                name = name.ifEmpty { "Auto Override Control" },
                triggerDeviceId = triggerDevId,
                triggerField = metric,
                triggerOperator = op,
                triggerValue = value,
                targetDeviceId = targetDevId,
                targetAction = action,
                isActive = true
            )
            repository.insertRule(freshRule)
            repository.insertAlert(
                NotificationAlert(
                    title = "Edge Automation Deployed",
                    message = "Ruleized logical script [${freshRule.name}]compiled and flashed into edge memory blocks.",
                    timestamp = System.currentTimeMillis(),
                    severity = "INFO"
                )
            )
        }
    }

    fun toggleRuleActive(rule: AutomationRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRule(rule.copy(isActive = !rule.isActive))
        }
    }

    fun deleteRule(ruleId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRule(ruleId)
        }
    }

    // --- Alerts ---
    fun markAlertAsRead(alertId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAlertAsRead(alertId)
        }
    }

    fun clearAllAlerts() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllAlerts()
        }
    }

    // --- AI Chat Assistant ---
    fun submitChatMessage(text: String) {
        if (text.isBlank()) return
        val originalChat = _chatMessages.value
        _chatMessages.value = originalChat + ("User" to text)
        _isChatLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            // Build device description context
            val currentDevices = devices.value
            val summary = StringBuilder("Available Network State:\n")
            for (dev in currentDevices) {
                summary.append("- ${dev.name} [ID:${dev.deviceId}, Status:${dev.status}, Type:${dev.type}, Telemetry:${dev.latestTelemetry}]\n")
            }
            summary.append("\nActive Automation Rules:\n")
            for (r in rules.value) {
                summary.append("- [Rule: ${r.name}, Trigger: ${r.triggerDeviceId} is ${r.triggerField} ${r.triggerOperator} ${r.triggerValue} -> Target: ${r.targetDeviceId} / ${r.targetAction}]\n")
            }

            val response = GeminiClient.askAssistant(text, summary.toString())
            _chatMessages.value = _chatMessages.value + ("Assistant" to response)
            _isChatLoading.value = false
        }
    }

    // --- Simulation engine and live script ---
    private fun startSimulationEngine() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(4000)
                if (!_isSimulating.value) continue

                val currentDevices = devices.value
                if (currentDevices.isEmpty()) continue

                val currentRules = rules.value

                // Pick a target device to simulate telemetry flux
                for (device in currentDevices) {
                    if (device.status == "OFFLINE") continue

                    // Parse & modify telemetry metrics
                    val nextRaw = simulateFieldVariance(device)
                    repository.updateDeviceTelemetry(device.deviceId, nextRaw.first, device.status)

                    // Write logs for the changed metrics
                    for (metric in nextRaw.second) {
                        repository.insertTelemetryLog(
                            TelemetryLog(
                                deviceId = device.deviceId,
                                timestamp = System.currentTimeMillis(),
                                metricName = metric.first,
                                value = metric.second
                            )
                        )

                        // Evaluate rules against this changed metric!
                        evaluateAutomationRulesForMetric(device.deviceId, metric.first, metric.second, currentRules)
                    }

                    // Force trigger live selected device visual change
                    if (_selectedDevice.value?.deviceId == device.deviceId) {
                        _selectedDevice.value = _selectedDevice.value?.copy(latestTelemetry = nextRaw.first)
                    }
                }
            }
        }
    }

    private fun simulateFieldVariance(device: IoTDevice): Pair<String, List<Pair<String, Double>>> {
        val list = mutableListOf<Pair<String, Double>>()
        val nextTelemetry = when (device.deviceId) {
            "IND-PLC-14" -> {
                // vibration:0.04mm, temp:46C, flowRate:210L/m
                val v = (0.02 + Math.random() * 0.08).coerceIn(0.01, 0.12)
                val t = (42 + Math.random() * 8)
                val f = (195 + Math.random() * 30)
                list.add("vibration" to v)
                list.add("temp" to t)
                list.add("flowRate" to f)
                String.format("vibration:%.2fmm, temp:%.1fC, flowRate:%.1fL/m", v, t, f)
            }
            "INF-BAT-G1" -> {
                // charge:85%, power_load:12kW, cell_temp:31.2C
                val c = (devices.value.find { it.deviceId == "INF-BAT-G1" }?.latestTelemetry?.substringAfter("charge:")?.substringBefore("%")?.toDoubleOrNull() ?: 85.0) - 0.2
                val currentCharge = if (c < 10.0) 98.0 else c
                val l = 8.0 + Math.random() * 8.0
                val t = 29.0 + Math.random() * 4.0
                list.add("charge" to currentCharge)
                list.add("power_load" to l)
                list.add("cell_temp" to t)
                String.format("charge:%.1f%%, power_load:%.1fkW, cell_temp:%.1fC", currentCharge, l, t)
            }
            "INF-WTR-VAL4" -> {
                // pressure:185psi, gate_opening:94%, leak_detected:true/false
                val p = (130 + Math.random() * 70) // fluctuates around threshold 150psi
                val g = 90.0 + Math.random() * 6.0
                list.add("pressure" to p)
                list.add("gate_opening" to g)
                String.format("pressure:%.1fpsi, gate_opening:%.0f%%, leak_detected:true", p, g)
            }
            "VEH-FLEET-08" -> {
                // speed:54mph, fuel:68%, cargo_temp:4C
                val speed = (45 + Math.random() * 20)
                val fuel = (devices.value.find { it.deviceId == "VEH-FLEET-08" }?.latestTelemetry?.substringAfter("fuel:")?.substringBefore("%")?.toDoubleOrNull() ?: 68.0) - 0.1
                val currentFuel = if (fuel < 5.0) 95.0 else fuel
                val temp = (3.5 + Math.random() * 1.5)
                list.add("speed" to speed)
                list.add("fuel" to currentFuel)
                list.add("cargo_temp" to temp)
                String.format("speed:%.1fmph, fuel:%.1f%%, cargo_temp:%.1fC", speed, currentFuel, temp)
            }
            "AGR-SOIL-P3" -> {
                // moisture:14%, soil_temp:18.5C (OFFLINE usually but lets simulate metric anyway)
                val m = (10 + Math.random() * 15)
                val t = (17 + Math.random() * 3)
                list.add("moisture" to m)
                list.add("soil_temp" to t)
                String.format("moisture:%.1f%%, soil_temp:%.1fC", m, t)
            }
            "CON-LIGHT-01" -> {
                val b = 70.0 + Math.random() * 20.0
                list.add("brightness" to b)
                String.format("brightness:%.0f%%, rgb:0xFFD4AF37", b)
            }
            else -> {
                val valSim = 10 + Math.random() * 50
                list.add("param" to valSim)
                String.format("v:%.1f, active:1", valSim)
            }
        }
        return nextTelemetry to list
    }

    private suspend fun evaluateAutomationRulesForMetric(
        deviceId: String,
        metric: String,
        value: Double,
        activeRules: List<AutomationRule>
    ) {
        val matches = activeRules.filter {
            it.isActive && it.triggerDeviceId == deviceId && it.triggerField == metric
        }

        for (rule in matches) {
            val triggerMet = when (rule.triggerOperator) {
                ">" -> value > rule.triggerValue
                "<" -> value < rule.triggerValue
                "==" -> Math.abs(value - rule.triggerValue) < 0.1
                else -> false
            }

            if (triggerMet) {
                // Automation executed! Update target device and push an alert
                val targetDev = repository.getDeviceById(rule.targetDeviceId) ?: continue
                
                // Let's perform target action
                val actionMessage = if (rule.targetDeviceId == "AGR-IRRIG-GATE") {
                    "AUTOMATION FIRED: ${rule.name}. Open flow gate valve. Action payload: [VALVE_OPEN=TRUE]"
                } else {
                    "AUTOMATION FIRED: ${rule.name}. Triggered local ${rule.targetAction} override on target cell ${rule.targetDeviceId}."
                }

                // Append alert to dashboard
                repository.insertAlert(
                    NotificationAlert(
                        title = "Automation Triggered: ${rule.name}",
                        message = "Logic condition: [$metric = ${String.format("%.1f", value)} ${rule.triggerOperator} ${rule.triggerValue}] passed. Sent dispatch signal to target: ${targetDev.name} ($actionMessage)",
                        timestamp = System.currentTimeMillis(),
                        severity = "CRITICAL"
                    )
                )

                // Mock target response change in database
                if (rule.targetDeviceId == "AGR-IRRIG-GATE") {
                    repository.updateDeviceTelemetry(
                        rule.targetDeviceId,
                        "valve_status:FLOWING, pressure:38psi",
                        "ONLINE"
                    )
                } else if (rule.targetDeviceId == "IND-PLC-14") {
                    repository.updateDeviceTelemetry(
                        rule.targetDeviceId,
                        "vibration:0.02mm, temp:38C, flowRate:95L/m", // cooling activated
                        "ONLINE"
                    )
                }
            }
        }
    }
}

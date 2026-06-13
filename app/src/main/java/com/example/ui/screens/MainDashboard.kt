package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
import com.example.data.model.AutomationRule
import com.example.data.model.IoTDevice
import com.example.data.model.NotificationAlert
import com.example.data.model.TelemetryLog
import com.example.ui.theme.*
import com.example.ui.viewmodel.IoTViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(viewModel: IoTViewModel) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
    val platformMode by viewModel.platformMode.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulating.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("DEVICES") } // "DEVICES", "SCADA", "AUTOMATION", "MESH", "AI_ASSISTANT", "ALERTS"
    var showAddDeviceDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        containerColor = DeepNavyBg,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = "SOVEREIGN NETWORK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BrightBrass,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                        Text(
                            text = "AURELIA IOT",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }
                },
                actions = {
                    Text(
                        text = if (isSimulating) "SIM_ACTIVE" else "SIM_PAUSED",
                        color = if (isSimulating) StatusGreen else StatusAmber,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(
                        onClick = { viewModel.toggleSimulation() },
                        modifier = Modifier.testTag("toggle_simulator_btn")
                    ) {
                        if (isSimulating) {
                            Row(
                                modifier = Modifier.size(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(StatusAmber))
                                Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(StatusAmber))
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Toggle simulator loop",
                                tint = StatusGreen
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.triggerBulkRestart() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BorderMuted,
                            contentColor = BrightBrass
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("bulk_reboot_header_btn")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Bulk rebuild core",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BULK CASCADE",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepNavyBg,
                    titleContentColor = TextWhite
                )
            )
        },
        bottomBar = {
            // Adaptive Navigation (Bottom Bar for Compact screen sizes)
            BoxWithConstraints {
                if (maxWidth <= 750.dp) {
                    NavigationBar(
                        containerColor = DeepNavyBg,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .border(width = 1.dp, color = BorderMuted, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    ) {
                        val navItems = listOf(
                            Triple("DEVICES", Icons.Default.List, "Twins"),
                            Triple("SCADA", Icons.Default.Settings, "SCADA"),
                            Triple("AUTOMATION", Icons.Default.Build, "Rules"),
                            Triple("MESH", Icons.Default.Share, "Mesh"),
                            Triple("AI_ASSISTANT", Icons.Default.Warning, "AI Operations"),
                            Triple("ALERTS", Icons.Default.Notifications, "Alarms")
                        )
                        navItems.forEach { (tab, icon, label) ->
                            NavigationBarItem(
                                selected = activeTab == tab,
                                onClick = { activeTab = tab },
                                icon = {
                                    Box {
                                        Icon(icon, contentDescription = label)
                                        if (tab == "ALERTS" && alerts.any { !it.isRead }) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .align(Alignment.TopEnd)
                                                    .background(StatusRed, CircleShape)
                                            )
                                        }
                                    }
                                },
                                label = { Text(label, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BrightBrass,
                                    selectedTextColor = BrightBrass,
                                    indicatorColor = BrightBrass.copy(alpha = 0.12f),
                                    unselectedIconColor = TextDim,
                                    unselectedTextColor = TextDim
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth > 750.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // Left hand sidebar for wider layouts (Tablet / Desktop ergonomics)
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = DeepNavyBg,
                        modifier = Modifier
                            .fillMaxHeight()
                            .border(width = 1.dp, color = BorderMuted, shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)),
                        header = {
                            IconButton(
                                onClick = { showAddDeviceDialog = true },
                                modifier = Modifier
                                    .padding(vertical = 16.dp)
                                    .background(BrightBrass, RoundedCornerShape(12.dp))
                                    .size(44.dp)
                                    .testTag("rail_add_device_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add node", tint = DeepNavyBg)
                            }
                        }
                    ) {
                        val navItems = listOf(
                            Triple("DEVICES", Icons.Default.List, "Digital Twins"),
                            Triple("SCADA", Icons.Default.Settings, "SCADA Panel"),
                            Triple("AUTOMATION", Icons.Default.Build, "Logic Rules"),
                            Triple("MESH", Icons.Default.Share, "Local Mesh"),
                            Triple("AI_ASSISTANT", Icons.Default.Warning, "Operations AI"),
                            Triple("ALERTS", Icons.Default.Notifications, "Alarms System")
                        )
                        navItems.forEach { (tab, icon, label) ->
                            NavigationRailItem(
                                selected = activeTab == tab,
                                onClick = { activeTab = tab },
                                icon = {
                                    Box {
                                        Icon(icon, contentDescription = label)
                                        if (tab == "ALERTS" && alerts.any { !it.isRead }) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .align(Alignment.TopEnd)
                                                    .background(StatusRed, CircleShape)
                                            )
                                        }
                                    }
                                },
                                label = { Text(label, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = BrightBrass,
                                    selectedTextColor = BrightBrass,
                                    indicatorColor = BrightBrass.copy(alpha = 0.12f),
                                    unselectedIconColor = TextDim,
                                    unselectedTextColor = TextDim
                                )
                            )
                        }
                    }
                }

                // Primary Content Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(DeepNavyBg)
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                        },
                        label = "MainTabsTransitions"
                    ) { targetTab ->
                        when (targetTab) {
                            "DEVICES" -> DeviceListAndTwinTwin(
                                devices = devices,
                                selectedDevice = selectedDevice,
                                logs = logs,
                                viewModel = viewModel,
                                onAddDeviceClick = { showAddDeviceDialog = true },
                                isWideScreen = isWideScreen
                            )
                            "SCADA" -> ScadaMimicLayout(devices = devices, viewModel = viewModel)
                            "AUTOMATION" -> AutomationEngineScreen(
                                rules = rules,
                                devices = devices,
                                viewModel = viewModel
                            )
                            "MESH" -> MeshTopologyScreen(devices = devices)
                            "AI_ASSISTANT" -> AiOpsAssistantScreen(
                                messages = chatMessages,
                                loading = isChatLoading,
                                viewModel = viewModel
                            )
                            "ALERTS" -> AlarmsCenterScreen(alerts = alerts, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    // New node enrolling dialog form
    if (showAddDeviceDialog) {
        AddDeviceEnrollmentDialog(
            onDismiss = { showAddDeviceDialog = false },
            onEnroll = { id, name, type, subType, connectivity, security, loc ->
                viewModel.addDevice(id, name, type, subType, connectivity, security, loc)
                showAddDeviceDialog = false
            }
        )
    }
}

// ==========================================
// SCREEN MODULE 1: DEVICES & DIGITAL TWIN
// ==========================================
@Composable
fun DeviceListAndTwinTwin(
    devices: List<IoTDevice>,
    selectedDevice: IoTDevice?,
    logs: List<TelemetryLog>,
    viewModel: IoTViewModel,
    onAddDeviceClick: () -> Unit,
    isWideScreen: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") } // "ALL", "CONSUMER", "INDUSTRIAL", "INFRASTRUCTURE", "VEHICLE", "AGRICULTURE"

    val filteredDevices = remember(devices, searchQuery, selectedCategoryFilter) {
        devices.filter { dev ->
            val matchQuery = dev.name.contains(searchQuery, ignoreCase = true) ||
                    dev.deviceId.contains(searchQuery, ignoreCase = true) ||
                    dev.subType.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategoryFilter == "ALL" || dev.type == selectedCategoryFilter
            matchQuery && matchCat
        }
    }

    if (isWideScreen) {
        // Landscape split: Twin Inventory on the left, Digital Twin Monitor on the right
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.40f)
                    .fillMaxHeight()
                    .border(1.dp, BorderMuted)
            ) {
                DeviceInventoryListPart(
                    filteredDevices = filteredDevices,
                    selectedDevice = selectedDevice,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedCategoryFilter = selectedCategoryFilter,
                    onFilterChange = { selectedCategoryFilter = it },
                    onDeviceSelect = { viewModel.selectDevice(it) },
                    onAddClick = onAddDeviceClick
                )
            }

            Box(
                modifier = Modifier
                    .weight(0.60f)
                    .fillMaxHeight()
            ) {
                if (selectedDevice != null) {
                    DigitalTwinDashboard(
                        device = selectedDevice,
                        logs = logs.filter { it.deviceId == selectedDevice.deviceId },
                        viewModel = viewModel
                    )
                } else {
                    EmptySelectionPlaceholder(text = "SELECT AN ACTIVE DEVICE ID FROM THE REGISTRY TREE TO LAUNCH DIGITAL TWIN PORT")
                }
            }
        }
    } else {
        // Narrow/Portrait responsive flow: list on top, dynamic twin modal sheet or bottom panel
        var showTwinBottomTab by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxWidth()
            ) {
                DeviceInventoryListPart(
                    filteredDevices = filteredDevices,
                    selectedDevice = selectedDevice,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedCategoryFilter = selectedCategoryFilter,
                    onFilterChange = { selectedCategoryFilter = it },
                    onDeviceSelect = {
                        viewModel.selectDevice(it)
                        showTwinBottomTab = true
                    },
                    onAddClick = onAddDeviceClick
                )
            }

            Divider(color = BorderMuted, thickness = 1.dp)

            Box(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxWidth()
            ) {
                if (selectedDevice != null) {
                    DigitalTwinDashboard(
                        device = selectedDevice,
                        logs = logs.filter { it.deviceId == selectedDevice.deviceId },
                        viewModel = viewModel
                    )
                } else {
                    EmptySelectionPlaceholder(text = "TAP ON AN ACTIVE NODE PATH IN REGISTRY VIEW")
                }
            }
        }
    }
}

@Composable
fun DeviceInventoryListPart(
    filteredDevices: List<IoTDevice>,
    selectedDevice: IoTDevice?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategoryFilter: String,
    onFilterChange: (String) -> Unit,
    onDeviceSelect: (IoTDevice) -> Unit,
    onAddClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SECURE REGISTER DATA",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BrightBrass,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
            )
            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(32.dp)
                    .background(BorderMuted, RoundedCornerShape(12.dp))
                    .testTag("add_device_dialog_trigger_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add node", tint = BrightBrass, modifier = Modifier.size(18.dp))
            }
        }

        // Tactical Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Filter physical nodes...", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            singleLine = true,
            textStyle = TextStyle(color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SlateGrayCard,
                unfocusedContainerColor = DarkGraphite,
                focusedIndicatorColor = BrightBrass,
                unfocusedIndicatorColor = BorderMuted,
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("device_search_input"),
            shape = RoundedCornerShape(14.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextDim, modifier = Modifier.size(16.dp)) }
        )

        // Type filter row
        val filterOptions = listOf("ALL", "INDUSTRIAL", "INFRASTRUCTURE", "AGRICULTURE", "VEHICLE", "CONSUMER")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filterOptions.forEach { opt ->
                val active = selectedCategoryFilter == opt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onFilterChange(opt) }
                        .border(
                            1.dp,
                            if (active) BrightBrass else BorderMuted,
                            RoundedCornerShape(20.dp)
                        )
                        .background(if (active) BrightBrass.copy(alpha = 0.15f) else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("filter_chip_$opt")
                ) {
                    Text(
                        text = opt,
                        fontSize = 9.sp,
                        color = if (active) BrightBrass else TextDim,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Devices List
        if (filteredDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO COMPATIBLE DEVICE TWINS REPORTING ON SELECTED SEGMENT",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = StatusAmber,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredDevices) { dev ->
                    val chosen = dev.deviceId == selectedDevice?.deviceId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (chosen) 1.5.dp else 1.dp,
                                color = if (chosen) BrightBrass else BorderMuted,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(
                                if (chosen) SlateGrayCard else DarkGraphite,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onDeviceSelect(dev) }
                            .padding(12.dp)
                            .testTag("device_item_${dev.deviceId}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Connection dot indicator
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (dev.status) {
                                        "ONLINE" -> StatusGreen
                                        "ALERT" -> StatusRed
                                        "MAINTENANCE" -> StatusAmber
                                        else -> TextDim
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dev.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "[${dev.deviceId}]",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = DiagnosticCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dev.subType,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDim
                                )
                                Text(
                                    text = dev.connectivityProfile,
                                    fontSize = 9.sp,
                                    color = BrightBrass,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            // Small telemetry snip
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DeepNavyBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = dev.latestTelemetry,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = StatusGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DigitalTwinDashboard(
    device: IoTDevice,
    logs: List<TelemetryLog>,
    viewModel: IoTViewModel
) {
    var editName by remember(device) { mutableStateOf(device.name) }
    var editLocation by remember(device) { mutableStateOf(device.locationName) }
    var editSecurity by remember(device) { mutableStateOf(device.securityProfile) }
    var showConfigEditor by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("digital_twin_panel")
    ) {
        // Digital twin active header banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DiagnosticCyan, RoundedCornerShape(20.dp))
                .background(SlateGrayCard, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "DIGITAL TWIN TELEMETRY DECK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = DiagnosticCyan,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Black
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = device.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "HW ID: ${device.deviceId} // AUTH: ${device.securityProfile}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextDim,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        when (device.status) {
                            "ONLINE" -> StatusGreen.copy(alpha = 0.2f)
                            "ALERT" -> StatusRed.copy(alpha = 0.2f)
                            "MAINTENANCE" -> StatusAmber.copy(alpha = 0.2f)
                            else -> TextDim.copy(alpha = 0.2f)
                        },
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        when (device.status) {
                            "ONLINE" -> StatusGreen
                            "ALERT" -> StatusRed
                            "MAINTENANCE" -> StatusAmber
                            else -> TextDim
                        },
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = device.status,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = when (device.status) {
                        "ONLINE" -> StatusGreen
                        "ALERT" -> StatusRed
                        "MAINTENANCE" -> StatusAmber
                        else -> TextWhite
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Canvas historical live chart (vibration/voltages index oscilloscope style)
        Text(
            text = "LIVE TELEMETRY SIGNAL (HISTORICAL LOGS)",
            style = MaterialTheme.typography.labelSmall.copy(
                color = BrightBrass,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.3.sp
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(DeepNavyBg, RoundedCornerShape(20.dp))
                .border(1.dp, BorderMuted, RoundedCornerShape(20.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            TelemetryOscilloscopeCanvas(logs = logs)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device twin variables
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkGraphite),
            border = BorderStroke(1.dp, BorderMuted),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "VIRTUAL STATE DATASET (DIGITAL TWIN)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BrightBrass,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.3.sp
                    ),
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                val telemetryPairs = remember(device.latestTelemetry) {
                    device.latestTelemetry.split(",").mapNotNull {
                        val elements = it.split(":")
                        if (elements.size == 2) elements[0].trim() to elements[1].trim() else null
                    }
                }

                if (telemetryPairs.isEmpty()) {
                    Text(
                        text = "Static register: State payload standard active",
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        telemetryPairs.forEach { (metricKey, metricValue) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DeepNavyBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = metricKey.uppercase(),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextDim,
                                    fontWeight = FontWeight.Black
                                )

                                Text(
                                    text = metricValue,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = StatusGreen,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vault signatures and certificate profiles
                Text(
                    text = "AURELIA SECURE IDENTITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BrightBrass,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.3.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepNavyBg, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PUBLIC DEVICE KEY:",
                        fontSize = 8.sp,
                        color = TextDim,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = device.publicKey,
                        fontSize = 9.sp,
                        color = DiagnosticCyan,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "FIRMWARE COMPILATION ID: ${device.firmwareVersion}",
                        fontSize = 10.sp,
                        color = TextWhite,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ROOT TRUST AUTHORITY: AURELIA SOVEREIGN VAULT SIGNED",
                        fontSize = 8.sp,
                        color = StatusGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tactical remote command module
        Text(
            text = "EDGE COMMAND CONTROL CENTRE",
            style = MaterialTheme.typography.labelSmall.copy(
                color = BrightBrass,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.3.sp
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.toggleDevicePower(device.deviceId) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (device.status == "OFFLINE") StatusGreen else StatusRed,
                    contentColor = DeepNavyBg
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("toggle_power_${device.deviceId}")
            ) {
                Text(
                    text = if (device.status == "OFFLINE") "BOOT POWER" else "DISCONNECT PW",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black
                )
            }

            Button(
                onClick = { viewModel.rebootDevice(device.deviceId) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BorderMuted,
                    contentColor = TextWhite
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("reboot_${device.deviceId}")
            ) {
                Text(
                    text = "REBOOT NODE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Toggle properties editor
        Button(
            onClick = { showConfigEditor = !showConfigEditor },
            colors = ButtonDefaults.buttonColors(
                containerColor = SlateGrayCard,
                contentColor = BrightBrass
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_twin_parameters_toggle")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (showConfigEditor) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showConfigEditor) "COLLAPSE CONFIG DECK" else "EXPAND CONFIG DECK",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black
                )
            }
        }

        if (showConfigEditor) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateGrayCard),
                border = BorderStroke(1.dp, BorderMuted),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "UPDATE REGISTRY DATA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BrightBrass,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.3.sp
                        )
                    )

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Device Name", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        textStyle = TextStyle(color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DeepNavyBg,
                            unfocusedContainerColor = DeepNavyBg,
                            focusedIndicatorColor = BrightBrass,
                            unfocusedIndicatorColor = BorderMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_device_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("Grid Placement Zone", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        textStyle = TextStyle(color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DeepNavyBg,
                            unfocusedContainerColor = DeepNavyBg,
                            focusedIndicatorColor = BrightBrass,
                            unfocusedIndicatorColor = BorderMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_device_location_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editSecurity,
                        onValueChange = { editSecurity = it },
                        label = { Text("Cryptographic Sec Profile", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        textStyle = TextStyle(color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DeepNavyBg,
                            unfocusedContainerColor = DeepNavyBg,
                            focusedIndicatorColor = BrightBrass,
                            unfocusedIndicatorColor = BorderMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_device_security_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateDeviceDetails(device.deviceId, editName, editLocation, editSecurity)
                                showConfigEditor = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrightBrass, contentColor = DeepNavyBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_device_params_btn")
                        ) {
                            Text("SAVE CONFIG", fontSize = 11.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // commissioning deletion button
        Button(
            onClick = { viewModel.removeDevice(device.deviceId) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = StatusRed
            ),
            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("delete_node_commission")
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("DECOMMISSION ASSET (DELETE FROM REGISTER)", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TelemetryOscilloscopeCanvas(logs: List<TelemetryLog>) {
    Canvas(modifier = Modifier.fillMaxSize().drawBehind {
        // Draw grid lines
        val lineCount = 6
        val gridColor = BorderMuted.copy(alpha = 0.3f)
        val strokeWidth = 1f

        for (i in 1..lineCount) {
            val y = size.height * (i.toFloat() / (lineCount + 1))
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth
            )
        }

        val vertCount = 8
        for (i in 1..vertCount) {
            val x = size.width * (i.toFloat() / (vertCount + 1))
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth
            )
        }
    }) {
        if (logs.isNotEmpty()) {
            val sortedLogs = logs.sortedBy { it.timestamp }
            val minVal = sortedLogs.minOfOrNull { it.value } ?: 0.0
            val maxVal = sortedLogs.maxOfOrNull { it.value } ?: 100.0
            val range = if (maxVal == minVal) 10.0 else maxVal - minVal

            val path = Path()
            sortedLogs.forEachIndexed { idx, log ->
                val x = (size.width / (sortedLogs.size - 1).coerceAtLeast(1)) * idx
                val normValue = ((log.value - minVal) / range).toFloat()
                val y = size.height - (normValue * (size.height - 20f) + 10f)

                if (idx == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }

                // Draw small dots at coordinates
                drawCircle(
                    color = BrightBrass,
                    radius = 3f,
                    center = Offset(x, y)
                )
            }

            drawPath(
                path = path,
                color = DiagnosticCyan,
                style = Stroke(width = 3f)
            )
        }
    }
}


// ==========================================
// SCREEN MODULE 2: SCADA FLOWS & SMART CITIES
// ==========================================
@Composable
fun ScadaMimicLayout(devices: List<IoTDevice>, viewModel: IoTViewModel) {
    val platformMode by viewModel.platformMode.collectAsStateWithLifecycle()

    var showControlPanelDeviceId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(
            text = "SOVEREIGN SYSTEM INFRASTRUCTURE SIMULATION",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = BrightBrass,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "INDUSTRIAL SCADA FLOWS & SMART CITY OVERLAYS",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Mode Toggler Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("STANDARD_MIMIC" to "STANDARD", "INDUSTRIAL_FLOW" to "INDUSTRIAL", "DYNAMIC_CITY" to "SMART_CITY").forEach { (label, modeKey) ->
                val active = platformMode == modeKey
                Button(
                    onClick = { viewModel.setPlatformMode(modeKey) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) BrightBrass else DarkGraphite,
                        contentColor = if (active) DeepNavyBg else TextWhite
                    ),
                    modifier = Modifier.weight(1f).testTag("mode_toggle_$modeKey"),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(text = label, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        when (platformMode) {
            "INDUSTRIAL" -> {
                IndustrialScadaCard(devices = devices, onDeviceClick = { showControlPanelDeviceId = it })
            }
            "SMART_CITY" -> {
                SmartCityGisCard(devices = devices, onDeviceClick = { showControlPanelDeviceId = it })
            }
            else -> {
                StandardTelemetryOverlay(devices = devices, onDeviceClick = { showControlPanelDeviceId = it })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Inline diagnostic pop-up controller
        if (showControlPanelDeviceId != null) {
            val deviceId = showControlPanelDeviceId!!
            val currentDevice = devices.find { it.deviceId == deviceId }

            if (currentDevice != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scada_control_deck_$deviceId"),
                    colors = CardDefaults.cardColors(containerColor = SlateGrayCard),
                    border = BorderStroke(1.dp, DiagnosticCyan)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MIMIC CONTROL OVERRIDE: $deviceId",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = DiagnosticCyan,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showControlPanelDeviceId = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close panel", tint = TextWhite)
                            }
                        }

                        Text(
                            text = "NAME: ${currentDevice.name}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "CURRENT TELEMETRY STATUS: ${currentDevice.latestTelemetry}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = StatusGreen,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.toggleDevicePower(deviceId) },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusAmber, contentColor = DeepNavyBg),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("TOGGLE POWER", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.rebootDevice(deviceId) },
                                colors = ButtonDefaults.buttonColors(containerColor = BorderMuted, contentColor = TextWhite),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("REBOOT NODE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                showControlPanelDeviceId = null
            }
        }
    }
}

@Composable
fun IndustrialScadaCard(devices: List<IoTDevice>, onDeviceClick: (String) -> Unit) {
    val pump = devices.find { it.deviceId == "IND-PLC-14" }
    val gate = devices.find { it.deviceId == "INF-WTR-VAL4" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scada_mimic_industrial_panel"),
        colors = CardDefaults.cardColors(containerColor = DarkGraphite),
        border = BorderStroke(1.dp, BorderMuted),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "HYDRAULIC FLOW AND RECTIFIER PROCESS SCHEMATIC (SCADA)",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = DiagnosticCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Flow graphics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(DeepNavyBg)
                    .border(1.dp, BorderMuted)
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw process tank
                    drawRect(
                        color = BorderMuted,
                        topLeft = Offset(40f, 20f),
                        size = androidx.compose.ui.geometry.Size(90f, 100f),
                        style = Stroke(width = 3f)
                    )

                    // Fill water level based on Reservoir gate value
                    val isGateAlert = gate?.status == "ALERT"
                    drawRect(
                        color = if (isGateAlert) StatusAmber.copy(alpha = 0.5f) else StatusGreen.copy(alpha = 0.5f),
                        topLeft = Offset(42f, 40f),
                        size = androidx.compose.ui.geometry.Size(86f, 78f)
                    )

                    // Piping lines
                    drawLine(
                        color = DiagnosticCyan,
                        start = Offset(130f, 80f),
                        end = Offset(240f, 80f),
                        strokeWidth = 6f
                    )

                    // Draw pump circle
                    drawCircle(
                        color = if (pump?.status == "ONLINE") StatusGreen else StatusRed,
                        radius = 24f,
                        center = Offset(240f, 80f)
                    )

                    // Exit line
                    drawLine(
                        color = DiagnosticCyan,
                        start = Offset(264f, 80f),
                        end = Offset(400f, 80f),
                        strokeWidth = 6f
                    )
                }

                // Mimic tags/buttons
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Tank Tag
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp)
                            .background(SlateGrayCard)
                            .border(1.dp, BorderMuted)
                            .padding(4.dp)
                    ) {
                        Text("RESERVOIR ALPHA", fontSize = 8.sp, color = TextWhite, fontFamily = FontFamily.Monospace)
                        Text(
                            text = if (gate?.status == "ALERT") "CRITICAL HEAD" else "PRESSURE NML",
                            fontSize = 8.sp,
                            color = if (gate?.status == "ALERT") StatusRed else StatusGreen,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Valve 4 tag click target
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = (-30).dp, y = 10.dp)
                            .background(SlateGrayCard)
                            .border(1.dp, DiagnosticCyan)
                            .clickable { onDeviceClick("INF-WTR-VAL4") }
                            .padding(4.dp)
                    ) {
                        Column {
                            Text("GATE VALVE 04", fontSize = 8.sp, color = BrightBrass, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(
                                text = gate?.latestTelemetry?.substringBefore(",") ?: "psi:N/A",
                                fontSize = 8.sp,
                                color = TextWhite,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Pump station 14
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp)
                            .background(SlateGrayCard)
                            .border(1.dp, DiagnosticCyan)
                            .clickable { onDeviceClick("IND-PLC-14") }
                            .padding(4.dp)
                    ) {
                        Column {
                            Text("PUMP STATION 14", fontSize = 8.sp, color = BrightBrass, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(
                                text = pump?.latestTelemetry?.substringAfter(",")?.substringBefore(",") ?: "vibration:N/A",
                                fontSize = 8.sp,
                                color = TextWhite,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DeepNavyBg)
                        .border(1.dp, BorderMuted)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "VIBRATION PROFILE:\nPump assembly vibration is at historical standard metric logic indices.",
                        fontSize = 10.sp,
                        color = TextDim,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DeepNavyBg)
                        .border(1.dp, BorderMuted)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "FAILSAFE STATUS:\nAutomatic bypass rules are currently armed and waiting telemetry event triggers.",
                        fontSize = 10.sp,
                        color = TextDim,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun SmartCityGisCard(devices: List<IoTDevice>, onDeviceClick: (String) -> Unit) {
    val transport = devices.find { it.deviceId == "VEH-FLEET-08" }
    val trafficSign = devices.find { it.deviceId == "INF-TRAF-SECTOR5" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scada_mimic_city_panel"),
        colors = CardDefaults.cardColors(containerColor = DarkGraphite),
        border = BorderStroke(1.dp, BorderMuted),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "GIS SMART TRANSPORT & PUBLIC SECTOR GEOLOCATION OVERLAY",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = DiagnosticCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Map mockup using Canvas drawings
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(DeepNavyBg)
                    .border(1.dp, BorderMuted)
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw intersecting road map layout
                    drawLine(
                        color = BorderMuted,
                        start = Offset(0f, 75f),
                        end = Offset(size.width, 75f),
                        strokeWidth = 32f
                    )
                    drawLine(
                        color = BorderMuted,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 32f
                    )

                    // Draw yellow road markings
                    drawLine(
                        color = Color.Yellow,
                        start = Offset(0f, 75f),
                        end = Offset(size.width, 75f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.Yellow,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 2f
                    )

                    // Geolocation pinning
                    drawCircle(
                        color = StatusGreen,
                        radius = 8f,
                        center = Offset(130f, 75f) // Fleet vehicle position
                    )

                    drawCircle(
                        color = DiagnosticCyan,
                        radius = 8f,
                        center = Offset(size.width / 2f + 40f, 40f) // Interstate signal position
                    )
                }

                // Absolute overlay tags
                Box(
                    modifier = Modifier
                        .offset(x = 24.dp, y = 14.dp)
                        .background(SlateGrayCard)
                        .border(1.dp, DiagnosticCyan)
                        .clickable { onDeviceClick("VEH-FLEET-08") }
                        .padding(4.dp)
                ) {
                    Column {
                        Text("TRANSPORTER 08", fontSize = 8.sp, color = BrightBrass, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(
                            text = transport?.latestTelemetry?.substringBefore(",") ?: "speed:N/A",
                            fontSize = 8.sp,
                            color = TextWhite,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(SlateGrayCard)
                        .border(1.dp, DiagnosticCyan)
                        .clickable { onDeviceClick("INF-TRAF-SECTOR5") }
                        .padding(4.dp)
                ) {
                    Column {
                        Text("TRANSIT SIGN SECTOR 5", fontSize = 8.sp, color = BrightBrass, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(
                            text = trafficSign?.latestTelemetry?.substringBefore(",") ?: "speed:N/A",
                            fontSize = 8.sp,
                            color = TextWhite,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Continuous fleet tracking parameters dynamically secured with cryptographic coordinate signing.",
                fontSize = 11.sp,
                color = TextDim,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun StandardTelemetryOverlay(devices: List<IoTDevice>, onDeviceClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scada_mimic_standard_panel"),
        colors = CardDefaults.cardColors(containerColor = DarkGraphite),
        border = BorderStroke(1.dp, BorderMuted),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "STANDARD GRID SECTOR FEED OVERVIEW",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = BrightBrass,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            devices.take(4).forEach { dev ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .background(DeepNavyBg)
                        .clickable { onDeviceClick(dev.deviceId) }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(dev.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("ZONE: ${dev.locationName}", fontSize = 9.sp, color = TextDim, fontFamily = FontFamily.Monospace)
                    }
                    Text(
                        dev.latestTelemetry,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = StatusGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


// ==========================================
// SCREEN MODULE 3: AUTOMATION RULES & ENGINE
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutomationEngineScreen(
    rules: List<AutomationRule>,
    devices: List<IoTDevice>,
    viewModel: IoTViewModel
) {
    var ruleName by remember { mutableStateOf("") }
    var triggerDeviceId by remember { mutableStateOf("") }
    var triggerMetric by remember { mutableStateOf("temp") }
    var triggerOp by remember { mutableStateOf(">") }
    var triggerVal by remember { mutableStateOf("") }
    var targetDeviceId by remember { mutableStateOf("") }
    var targetAction by remember { mutableStateOf("COOLDOWN") }

    if (triggerDeviceId.isEmpty() && devices.isNotEmpty()) {
        triggerDeviceId = devices.first().deviceId
    }
    if (targetDeviceId.isEmpty() && devices.isNotEmpty()) {
        targetDeviceId = devices.first().deviceId
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("automation_rules_deck")
    ) {
        Text(
            text = "EDGE COMPUTING EMBEDDED AUTOMATION SCRIPT compiler",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = BrightBrass,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "LOCAL DIRECTIVE ENGINE",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        // Custom script writer (Rule adding Form)
        Card(
            modifier = Modifier.fillMaxWidth().testTag("add_rule_form"),
            colors = CardDefaults.cardColors(containerColor = DarkGraphite),
            border = BorderStroke(1.dp, BorderMuted),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "COMPILE & FLASH NEW DIRECT RULE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = DiagnosticCyan,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    placeholder = { Text("e.g., Automatic Flood Overrides", color = TextDim, fontSize = 11.sp) },
                    label = { Text("Task Directive Name", color = TextWhite, fontSize = 11.sp) },
                    textStyle = TextStyle(color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DeepNavyBg,
                        unfocusedContainerColor = DeepNavyBg,
                        focusedIndicatorColor = BrightBrass,
                        unfocusedIndicatorColor = BorderMuted
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("rule_name_input")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Trigger device
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WHEN NODE ID:", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .background(DeepNavyBg)
                                .border(1.dp, BorderMuted)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                devices.forEach { d ->
                                    val match = d.deviceId == triggerDeviceId
                                    Box(
                                        modifier = Modifier
                                            .clickable { triggerDeviceId = d.deviceId }
                                            .background(if (match) BrightBrass else SlateGrayCard)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(d.deviceId, fontSize = 8.sp, color = if (match) DeepNavyBg else TextWhite, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }

                    // Field metric
                    OutlinedTextField(
                        value = triggerMetric,
                        onValueChange = { triggerMetric = it },
                        label = { Text("Metric", color = TextWhite, fontSize = 11.sp) },
                        textStyle = TextStyle(color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        colors = TextFieldDefaults.colors(focusedContainerColor = DeepNavyBg, unfocusedContainerColor = DeepNavyBg),
                        modifier = Modifier.weight(0.6f).testTag("rule_metric_input")
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Operator
                    Row(
                        modifier = Modifier.weight(0.8f).height(46.dp).background(DeepNavyBg).border(1.dp, BorderMuted),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(">", "<", "==").forEach { operand ->
                            Box(
                                modifier = Modifier
                                    .clickable { triggerOp = operand }
                                    .background(if (triggerOp == operand) DiagnosticCyan else Color.Transparent)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(operand, color = if (triggerOp == operand) DeepNavyBg else TextWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Value
                    OutlinedTextField(
                        value = triggerVal,
                        onValueChange = { triggerVal = it },
                        label = { Text("Target Val", color = TextWhite, fontSize = 11.sp) },
                        textStyle = TextStyle(color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        colors = TextFieldDefaults.colors(focusedContainerColor = DeepNavyBg, unfocusedContainerColor = DeepNavyBg),
                        modifier = Modifier.weight(1.2f).testTag("rule_val_input")
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Target device
                    Column(modifier = Modifier.weight(1f)) {
                        Text("THEN DISPATCH TO NODE:", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .background(DeepNavyBg)
                                .border(1.dp, BorderMuted)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                devices.forEach { d ->
                                    val match = d.deviceId == targetDeviceId
                                    Box(
                                        modifier = Modifier
                                            .clickable { targetDeviceId = d.deviceId }
                                            .background(if (match) BrightBrass else SlateGrayCard)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(d.deviceId, fontSize = 8.sp, color = if (match) DeepNavyBg else TextWhite, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }

                    // Action
                    OutlinedTextField(
                        value = targetAction,
                        onValueChange = { targetAction = it },
                        label = { Text("Action Exec", color = TextWhite, fontSize = 11.sp) },
                        textStyle = TextStyle(color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        colors = TextFieldDefaults.colors(focusedContainerColor = DeepNavyBg, unfocusedContainerColor = DeepNavyBg),
                        modifier = Modifier.weight(0.8f).testTag("rule_action_input")
                    )
                }

                // Compile/Submit Row
                Button(
                    onClick = {
                        val numVal = triggerVal.toDoubleOrNull() ?: 0.0
                        viewModel.addRule(ruleName, triggerDeviceId, triggerMetric, triggerOp, numVal, targetDeviceId, targetAction)
                        ruleName = ""
                        triggerVal = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightBrass, contentColor = DeepNavyBg),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("compile_rule_btn")
                ) {
                    Text("COMPILE & FLASH INTO LOCAL MESH MEMORY", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List of Active rules
        Text(
            text = "DEPLOYED LOCAL DIRECTIVE MEMORY BLOCKS",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TextDim,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (rules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("NO DIRECTIVES COMPILED. REGISTER IS EMPTY.", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = StatusAmber)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rules.forEach { rule ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkGraphite)
                            .border(1.dp, BorderMuted)
                            .padding(10.dp)
                            .testTag("rule_row_${rule.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rule.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )

                            Text(
                                text = "IF `${rule.triggerDeviceId}`.${rule.triggerField} ${rule.triggerOperator} ${rule.triggerValue} THEN -> `${rule.targetDeviceId}` execute ${rule.targetAction}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = StatusGreen,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // Switches
                        Switch(
                            checked = rule.isActive,
                            onCheckedChange = { viewModel.toggleRuleActive(rule) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepNavyBg,
                                checkedTrackColor = BrightBrass,
                                uncheckedThumbColor = TextDim,
                                uncheckedTrackColor = BorderMuted
                            ),
                            modifier = Modifier.scale(0.81f).testTag("toggle_rule_active_${rule.id}")
                        )

                        IconButton(
                            onClick = { viewModel.deleteRule(rule.id) },
                            modifier = Modifier.testTag("delete_rule_${rule.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}




// ==========================================
// SCREEN MODULE 4: MESH TOPOLOGY & NETWORK HEALTH
// ==========================================
@Composable
fun MeshTopologyScreen(devices: List<IoTDevice>) {
    val meshDevices = remember(devices) { devices.filter { it.isMeshNode } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("mesh_network_topology")
    ) {
        Text(
            text = "LOCAL MESH AD-HOC TOPOLOGY ROUTING MAP",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = BrightBrass,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "DISTRIBUTED COGNITIVE MESH",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Mesh Diagram Display Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkGraphite),
            border = BorderStroke(1.dp, BorderMuted),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "AURELIA LOCAL-FIRST AD-HOC MESH DIAGRAM",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = DiagnosticCyan,
                    modifier = Modifier.padding(bottom = 10.dp),
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(DeepNavyBg)
                        .border(1.dp, BorderMuted),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = size.height * 0.35f

                        // Draw Central Control Hub Gateway
                        drawCircle(
                            color = BrightBrass,
                            radius = 16f,
                            center = Offset(centerX, centerY)
                        )

                        // Draw peripheral sensor mesh links
                        meshDevices.forEachIndexed { index, device ->
                            val angle = (2 * Math.PI * index / meshDevices.size).toFloat()
                            val nodeX = centerX + radius * Math.cos(angle.toDouble()).toFloat()
                            val nodeY = centerY + radius * Math.sin(angle.toDouble()).toFloat()

                            // Draw Link
                            drawLine(
                                color = if (device.status == "OFFLINE") BorderMuted else DiagnosticCyan.copy(alpha = 0.5f),
                                start = Offset(centerX, centerY),
                                end = Offset(nodeX, nodeY),
                                strokeWidth = 2f
                            )

                            // Nodes
                            drawCircle(
                                color = if (device.status == "OFFLINE") TextDim else if (device.status == "ALERT") StatusRed else StatusGreen,
                                radius = 9f,
                                center = Offset(nodeX, nodeY)
                            )
                        }
                    }

                    // Floating description text top of mesh
                    Text(
                        text = "GATEWAY HUB\n[LOCAL ROOT TRUST]",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = BrightBrass,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).offset(y = 28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "System operates peer-to-peer over military grade Zigbee & Z-Wave fallback nodes, fully immune to cloud service latency spikes or satellite backhaul drops.",
                    fontSize = 11.sp,
                    color = TextDim,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnostic Metrics Grid
        Text(
            text = "MESH INTEGRITY DATA BLOCK",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = TextDim,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        meshDevices.forEach { node ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkGraphite)
                    .border(1.dp, BorderMuted)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(node.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text("NODE ID: ${node.deviceId} // PROTOCOL: ${node.connectivityProfile}", fontSize = 8.sp, color = TextDim, fontFamily = FontFamily.Monospace)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("LAT: ${if (node.status == "OFFLINE") "---" else "4.2ms"}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = DiagnosticCyan)
                    Text("LOSS: ${if (node.status == "OFFLINE") "100%" else "0.01%"}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = if (node.status == "OFFLINE") StatusRed else StatusGreen)
                }
            }
        }
    }
}


// ==========================================
// SCREEN MODULE 5: AI OPERATIONS ASSISTANT
// ==========================================
@Composable
fun AiOpsAssistantScreen(
    messages: List<Pair<String, String>>,
    loading: Boolean,
    viewModel: IoTViewModel
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("ai_operations_center")
    ) {
        Text(
            text = "AURELIA DEEP COGNITIVE AI OPERATIONS COMMAND",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = BrightBrass,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "AURELIA OPERATIONS ROOM CO COMPILER",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Chat display card scrollable column
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGraphite),
            border = BorderStroke(1.dp, BorderMuted),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(listState)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        messages.forEach { (sender, content) ->
                            val isAssistant = sender == "Assistant"
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isAssistant) DeepNavyBg else SlateGrayCard)
                                    .border(1.dp, if (isAssistant) BorderMuted else DiagnosticCyan.copy(alpha = 0.3f))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = if (isAssistant) "AURELIA COGNITIVE CO-INTELLIGENCE" else "OPERATOR COMMAND",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isAssistant) BrightBrass else DiagnosticCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = content,
                                    fontSize = 11.sp,
                                    color = TextWhite,
                                    fontFamily = if (isAssistant) FontFamily.Monospace else FontFamily.Default
                                )
                            }
                        }

                        if (loading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BrightBrass, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("PROBING SECURE RETROFIT TELEMETRY REGISTRY...", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = BrightBrass)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Input send field row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask operations room, e.g. 'Synthesize Pump 14 health'...", color = TextDim, fontSize = 11.sp) },
                singleLine = true,
                textStyle = TextStyle(color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkGraphite,
                    unfocusedContainerColor = DarkGraphite,
                    focusedIndicatorColor = BrightBrass,
                    unfocusedIndicatorColor = BorderMuted
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input"),
                shape = RoundedCornerShape(4.dp)
            )

            Button(
                onClick = {
                    val msg = textInput
                    if (msg.isNotBlank()) {
                        viewModel.submitChatMessage(msg)
                        textInput = ""
                        focusManager.clearFocus()
                        scope.launch {
                            delay(300)
                            listState.animateScrollTo(listState.maxValue)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrightBrass, contentColor = DeepNavyBg),
                shape = RoundedCornerShape(4.dp),
                enabled = !loading,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("ai_send_chat_btn")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Query AI")
            }
        }
    }
}


// ==========================================
// SCREEN MODULE 6: ALARMS & HISTORY LOGS
// ==========================================
@Composable
fun AlarmsCenterScreen(alerts: List<NotificationAlert>, viewModel: IoTViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("alarms_operations_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AURELIA CENTRAL OPERATIONAL HARMAN BREACH LOGS",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = BrightBrass,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ALARM COMMAND PORT",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Button(
                onClick = { viewModel.clearAllAlerts() },
                colors = ButtonDefaults.buttonColors(containerColor = BorderMuted, contentColor = StatusRed),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.testTag("clear_alerts_btn")
            ) {
                Text("CLEAR LOGS", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Alarms list
        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ALL SYSTEM SECTORS REPORT NOMINAL OPERATIONS",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = StatusGreen
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alerts) { alert ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (alert.isRead) DarkGraphite.copy(alpha = 0.5f) else DarkGraphite)
                            .border(
                                1.dp,
                                when (alert.severity) {
                                    "CRITICAL" -> StatusRed
                                    "WARNING" -> StatusAmber
                                    else -> BorderMuted
                                }
                            )
                            .clickable { viewModel.markAlertAsRead(alert.id) }
                            .padding(12.dp)
                            .testTag("alert_item_${alert.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (alert.severity) {
                                "CRITICAL" -> Icons.Default.Warning
                                "WARNING" -> Icons.Default.Info
                                else -> Icons.Default.Check
                            },
                            contentDescription = alert.severity,
                            tint = when (alert.severity) {
                                "CRITICAL" -> StatusRed
                                "WARNING" -> StatusAmber
                                else -> StatusGreen
                            },
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = alert.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alert.isRead) TextDim else TextWhite
                                )
                                Text(
                                    text = alert.severity,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = when (alert.severity) {
                                        "CRITICAL" -> StatusRed
                                        "WARNING" -> StatusAmber
                                        else -> StatusGreen
                                    }
                                )
                            }

                            Text(
                                text = alert.message,
                                fontSize = 10.sp,
                                color = TextDim,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// SCREEN MODULE 7: SECURE DIALOG FORM
// ==========================================
@Composable
fun AddDeviceEnrollmentDialog(
    onDismiss: () -> Unit,
    onEnroll: (id: String, name: String, type: String, subType: String, connectivity: String, security: String, location: String) -> Unit
) {
    var devId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("INDUSTRIAL") }
    var subType by remember { mutableStateOf("") }
    var selectedConn by remember { mutableStateOf("ZIGBEE") }
    var selectedSec by remember { mutableStateOf("mTLS (RSA-4096)") }
    var locationZone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkGraphite,
        title = {
            Text(
                "AURELIA ASSET ENROLLMENT PORT",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = BrightBrass,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = devId,
                    onValueChange = { devId = it },
                    label = { Text("Physical Asset ID", color = TextWhite, fontSize = 11.sp) },
                    textStyle = TextStyle(color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    colors = TextFieldDefaults.colors(focusedContainerColor = DeepNavyBg, unfocusedContainerColor = DeepNavyBg),
                    modifier = Modifier.fillMaxWidth().testTag("add_device_id_input")
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name", color = TextWhite, fontSize = 11.sp) },
                    textStyle = TextStyle(color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    colors = TextFieldDefaults.colors(focusedContainerColor = DeepNavyBg, unfocusedContainerColor = DeepNavyBg),
                    modifier = Modifier.fillMaxWidth().testTag("add_device_name_input")
                )

                // Type select strip
                Text("SECTOR TYPE:", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(DeepNavyBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("INDUSTRIAL", "INFRASTRUCTURE", "AGRICULTURE", "VEHICLE", "CONSUMER").forEach { t ->
                        val active = selectedType == t
                        Box(
                            modifier = Modifier
                                .clickable { selectedType = t }
                                .background(if (active) BrightBrass else SlateGrayCard)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("enrol_type_chip_$t")
                        ) {
                            Text(t, fontSize = 8.sp, color = if (active) DeepNavyBg else TextDim, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                OutlinedTextField(
                    value = subType,
                    onValueChange = { subType = it },
                    label = { Text("Asset subClass (e.g. PLC, Probe)", color = TextWhite, fontSize = 11.sp) },
                    textStyle = TextStyle(color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    colors = TextFieldDefaults.colors(focusedContainerColor = DeepNavyBg, unfocusedContainerColor = DeepNavyBg),
                    modifier = Modifier.fillMaxWidth().testTag("add_device_sub_input")
                )

                OutlinedTextField(
                    value = locationZone,
                    onValueChange = { locationZone = it },
                    label = { Text("Deployment Zone Zone", color = TextWhite, fontSize = 11.sp) },
                    textStyle = TextStyle(color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    colors = TextFieldDefaults.colors(focusedContainerColor = DeepNavyBg, unfocusedContainerColor = DeepNavyBg),
                    modifier = Modifier.fillMaxWidth().testTag("add_device_loc_input")
                )

                // Connectivity profiles
                Text("CONNECTIVITY:", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(DeepNavyBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("ZIGBEE", "LORAWAN", "WIFI", "CELLULAR", "BLE", "Z_WAVE").forEach { c ->
                        val active = selectedConn == c
                        Box(
                            modifier = Modifier
                                .clickable { selectedConn = c }
                                .background(if (active) DiagnosticCyan else SlateGrayCard)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(c, fontSize = 8.sp, color = if (active) DeepNavyBg else TextDim, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Security Profiles
                Text("CRYPTO VERIFICATION SYSTEM:", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextDim)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(DeepNavyBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("mTLS (RSA-4096)", "Aurelia Signature (ECC)", "Legacy SSH").forEach { s ->
                        val active = selectedSec == s
                        Box(
                            modifier = Modifier
                                .clickable { selectedSec = s }
                                .background(if (active) StatusGreen else SlateGrayCard)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(s, fontSize = 8.sp, color = if (active) DeepNavyBg else TextDim, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onEnroll(devId, name, selectedType, subType, selectedConn, selectedSec, locationZone) },
                colors = ButtonDefaults.buttonColors(containerColor = BrightBrass, contentColor = DeepNavyBg),
                modifier = Modifier.testTag("confirm_enrollment_btn")
            ) {
                Text("AUTHORIZE COMMISSION", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BorderMuted, contentColor = TextWhite)
            ) {
                Text("ABORT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun EmptySelectionPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Build, contentDescription = null, tint = BorderMuted, modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextDim,
                textAlign = TextAlign.Center
            )
        }
    }
}

package com.example.safar20.ui.screens.settings

import android.content.Context
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.data.local.entity.DeviceEntity
import com.example.safar20.data.network.LinkState
import com.example.safar20.ui.components.BrassCard
import com.example.safar20.ui.components.QrPairingDialog
import com.example.safar20.ui.components.SectionLabel
import com.example.safar20.ui.components.SegmentedToggle
import com.example.safar20.ui.theme.BebasNeue
import com.example.safar20.ui.theme.Inter
import com.example.safar20.ui.theme.PhosphorGreen
import com.example.safar20.ui.theme.Rajdhani
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.SignalRed
import com.example.safar20.viewModels.SafarViewModel
import kotlinx.coroutines.launch

/**
 * Settings Screen for Unit Conversion, Theme Switching, Device Management,
 * MQTT Broker Relay Configuration, Developer Options & App Information.
 */
@ExperimentalGetImage
@Composable
fun SettingScreen(
    viewModel: SafarViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allDevices by viewModel.allDevices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val speedUnit by viewModel.speedUnit.collectAsState()
    val isSimulatorRunning by viewModel.isSimulatorRunning.collectAsState()
    val linkState by viewModel.linkState.collectAsState()
    val isDevModeEnabled by viewModel.isDevModeEnabled.collectAsState()

    val mqttBrokerUri by viewModel.mqttBrokerUri.collectAsState()
    val mqttUsername by viewModel.mqttUsername.collectAsState()
    val mqttPassword by viewModel.mqttPassword.collectAsState()

    var hostInput by remember(mqttBrokerUri) { mutableStateOf(mqttBrokerUri) }
    var usernameInput by remember(mqttUsername) { mutableStateOf(mqttUsername) }
    var passwordInput by remember(mqttPassword) { mutableStateOf(mqttPassword) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var testConnectionState by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    var showQrDialog by remember { mutableStateOf(false) }
    var qrErrorMessage by remember { mutableStateOf<String?>(null) }
    var flashMessage by remember { mutableStateOf<String?>(null) }

    var deviceToDelete by remember { mutableStateOf<DeviceEntity?>(null) }
    var showClearFlashConfirm by remember { mutableStateOf(false) }

    var versionTapCount by remember { mutableIntStateOf(0) }

    val appVersion = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "2.0.0"
        } catch (_: Exception) {
            "2.0.0"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "COCKPIT SETTINGS",
            fontFamily = Rajdhani,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. APPEARANCE
        SectionLabel(text = "APPEARANCE")
        Spacer(modifier = Modifier.height(8.dp))
        BrassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "COCKPIT THEME",
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDarkTheme) "Midnight Moto Cockpit (Dark)" else "Classic Day Dial (Light)",
                            fontFamily = Inter,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                SegmentedToggle(
                    options = listOf("DARK", "LIGHT"),
                    selectedIndex = if (isDarkTheme) 0 else 1,
                    onSelect = { index ->
                        viewModel.toggleDarkTheme(index == 0)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. UNITS
        SectionLabel(text = "UNITS")
        Spacer(modifier = Modifier.height(8.dp))
        BrassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SPEED & DISTANCE",
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (speedUnit == "mph") "Imperial (Miles, MPH)" else "Metric (Kilometers, KM/H)",
                            fontFamily = Inter,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                SegmentedToggle(
                    options = listOf("KM/H", "MPH"),
                    selectedIndex = if (speedUnit == "mph") 1 else 0,
                    onSelect = { index ->
                        val targetUnit = if (index == 0) "km/h" else "mph"
                        if (speedUnit != targetUnit) {
                            viewModel.toggleSpeedUnit()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. MQTT RELAY BROKER
        SectionLabel(text = "MQTT RELAY BROKER")
        Spacer(modifier = Modifier.height(8.dp))
        BrassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CELLULAR & CLOUD RELAY CONFIG",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val isUriInvalid = hostInput.isNotBlank() && !hostInput.trim().startsWith("ssl://")

                OutlinedTextField(
                    value = hostInput,
                    onValueChange = {
                        hostInput = it
                        testConnectionState = null
                    },
                    label = { Text("Broker URI (ssl://...)") },
                    isError = isUriInvalid,
                    supportingText = {
                        if (isUriInvalid) {
                            Text("URI must start with ssl:// (e.g. ssl://broker.emqx.io:8883)", color = SignalRed)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = {
                        usernameInput = it
                        testConnectionState = null
                    },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        testConnectionState = null
                    },
                    label = { Text("Password") },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isUriInvalid || hostInput.isBlank()) {
                                testConnectionState = "Please enter a valid ssl:// broker URI"
                                return@OutlinedButton
                            }
                            isTestingConnection = true
                            testConnectionState = null
                            scope.launch {
                                val err = viewModel.testBrokerConnection(hostInput, usernameInput, passwordInput)
                                isTestingConnection = false
                                testConnectionState = err ?: "SUCCESS"
                            }
                        },
                        enabled = !isTestingConnection,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("TEST CONNECTION", fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (isUriInvalid) {
                                flashMessage = "Cannot save: Broker URI must start with ssl://"
                                return@Button
                            }
                            viewModel.saveMqttBrokerConfig(hostInput, usernameInput, passwordInput)
                            flashMessage = "Broker config saved & reconnecting..."
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("SAVE & RECONNECT", fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                if (testConnectionState != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (testConnectionState == "SUCCESS") "✓ Connected successfully!" else "✗ $testConnectionState",
                        color = if (testConnectionState == "SUCCESS") PhosphorGreen else SignalRed,
                        fontFamily = Inter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Link Status Line
                val statusText = when (linkState) {
                    is LinkState.Connected -> "Link Status: Connected to Relay"
                    is LinkState.Connecting -> "Link Status: Connecting..."
                    is LinkState.Error -> "Link Status: Error (${(linkState as LinkState.Error).message})"
                    is LinkState.Idle -> "Link Status: Idle"
                }
                Text(
                    text = statusText,
                    fontFamily = Inter,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. MY TRACKERS
        SectionLabel(text = "MY TRACKERS")
        Spacer(modifier = Modifier.height(8.dp))
        BrassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                if (allDevices.isEmpty()) {
                    Text(
                        text = "NO TRACKERS PAIRED",
                        fontFamily = Rajdhani,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    allDevices.forEachIndexed { index, device ->
                        val isSelected = (device.deviceId == selectedDevice?.deviceId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectDevice(device.deviceId) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = device.deviceName,
                                            fontFamily = Rajdhani,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(PhosphorGreen, shape = CircleShape)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "ID: ${device.deviceId} • SIM: ${device.simNumber.ifBlank { "N/A" }}",
                                        fontFamily = Inter,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { deviceToDelete = device }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = SignalRed
                                )
                            }
                        }
                        if (index < allDevices.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        qrErrorMessage = null
                        showQrDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ PAIR NEW TRACKER",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. DEVELOPER OPTIONS (Only visible when unlocked)
        if (isDevModeEnabled) {
            SectionLabel(text = "DEVELOPER OPTIONS")
            Spacer(modifier = Modifier.height(8.dp))
            BrassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "MOCK SIMULATOR",
                                fontFamily = Rajdhani,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Simulate live GPS stream along mock route",
                                fontFamily = Inter,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isSimulatorRunning,
                            onCheckedChange = { viewModel.toggleSimulator(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surface,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.seedDemoRides()
                                flashMessage = "Demo rides loaded"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("LOAD DEMO RIDES", fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.removeDemoRides()
                                flashMessage = "Demo rides removed"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("REMOVE DEMO RIDES", fontFamily = Rajdhani, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showClearFlashConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CLEAR TRACKER FLASH MEMORY",
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (flashMessage != null) {
            Text(
                text = flashMessage!!,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = Inter,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 6. ABOUT
        SectionLabel(text = "ABOUT")
        Spacer(modifier = Modifier.height(8.dp))
        BrassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            versionTapCount++
                            if (versionTapCount >= 7) {
                                versionTapCount = 0
                                val next = !isDevModeEnabled
                                viewModel.toggleDevMode(next)
                                flashMessage = if (next) "Developer Options Unlocked!" else "Developer Options Hidden"
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAFAR 2.0",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "v$appVersion",
                        fontFamily = BebasNeue,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Edge storage + relay-only server",
                    fontFamily = Inter,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Delete Tracker Confirmation Dialog
    if (deviceToDelete != null) {
        val dev = deviceToDelete!!
        AlertDialog(
            onDismissRequest = { deviceToDelete = null },
            title = { Text("Delete Tracker?", fontFamily = Rajdhani, fontWeight = FontWeight.Bold) },
            text = { Text("Delete tracker '${dev.deviceName}' and its local ride history? This will permanently remove device and all stored trip data.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDevice(dev.deviceId)
                        deviceToDelete = null
                        flashMessage = "Tracker ${dev.deviceName} deleted"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SignalRed)
                ) {
                    Text("DELETE", fontFamily = Rajdhani, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToDelete = null }) {
                    Text("CANCEL", fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Clear Flash Confirmation Dialog
    if (showClearFlashConfirm) {
        AlertDialog(
            onDismissRequest = { showClearFlashConfirm = false },
            title = { Text("Clear Tracker Flash?", fontFamily = Rajdhani, fontWeight = FontWeight.Bold) },
            text = { Text("Send CLEAR_FLASH command (0x06) to tracker? This will erase flash memory on the device up to current timestamp.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearFlashConfirm = false
                        val sent = viewModel.sendClearTrackerFlash(mode = 0)
                        flashMessage = if (sent) "Clear Flash command sent to tracker" else "Tracker not connected over MQTT"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CONFIRM CLEAR", fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearFlashConfirm = false }) {
                    Text("CANCEL", fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showQrDialog) {
        QrPairingDialog(
            existingDevices = allDevices,
            externalError = qrErrorMessage,
            onDismiss = {
                qrErrorMessage = null
                showQrDialog = false
            },
            onPairQrUri = { uri ->
                viewModel.pairDeviceFromQr(
                    qrUriString = uri,
                    onSuccess = {
                        qrErrorMessage = null
                        showQrDialog = false
                    },
                    onError = { err ->
                        qrErrorMessage = err
                    }
                )
            },
            onPairManual = { id, name, sim, pub, sec ->
                viewModel.addManualDevice(id, name, sim, pub, sec,
                    onSuccess = {
                        qrErrorMessage = null
                        showQrDialog = false
                    },
                    onError = { err ->
                        qrErrorMessage = err
                    }
                )
            }
        )
    }
}

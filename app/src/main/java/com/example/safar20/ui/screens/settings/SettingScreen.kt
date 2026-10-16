package com.example.safar20.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.ui.components.QrPairingDialog
import com.example.safar20.ui.theme.BronzeRim
import com.example.safar20.ui.theme.CrankcaseBlack
import com.example.safar20.ui.theme.GunmetalGauge
import com.example.safar20.ui.theme.MutedPewter
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.SignalRed
import com.example.safar20.ui.theme.WarmIvory
import com.example.safar20.viewModels.SafarViewModel

/**
 * Settings Screen for Unit Conversion, Theme Switching, Device Management & Telemetry Simulation.
 */
@Composable
fun SettingScreen(
    viewModel: SafarViewModel,
    modifier: Modifier = Modifier
) {
    val allDevices by viewModel.allDevices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val speedUnit by viewModel.speedUnit.collectAsState()
    val isSimulatorRunning by viewModel.isSimulatorRunning.collectAsState()

    var showQrDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CrankcaseBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "COCKPIT SETTINGS",
            color = WarmIvory,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Paired Motorcycles & Device Management
        SettingsSectionHeader(title = "PAIRED MOTORCYCLES", icon = Icons.Default.DirectionsBike)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BronzeRim, shape = RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = GunmetalGauge)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                allDevices.forEach { device ->
                    val isSelected = (device.deviceId == selectedDevice?.deviceId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.selectDevice(device.deviceId) },
                                colors = RadioButtonDefaults.colors(selectedColor = RoyalBrass)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(device.deviceName, color = WarmIvory, fontWeight = FontWeight.Bold)
                                Text("SIM: ${device.simNumber}", color = MutedPewter, fontSize = 11.sp)
                            }
                        }

                        IconButton(onClick = { viewModel.deleteDevice(device.deviceId) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SignalRed)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showQrDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBrass),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = GunmetalGauge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PAIR NEW TRACKER (QR)", color = GunmetalGauge, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Speed & Distance Unit Unit Conversion
        SettingsSectionHeader(title = "SPEED & DISTANCE UNITS", icon = Icons.Default.Speed)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BronzeRim, shape = RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = GunmetalGauge)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Metric vs Imperial", color = WarmIvory, fontWeight = FontWeight.Bold)
                    Text(
                        if (speedUnit == "mph") "Active: Miles / MPH" else "Active: Kilometers / KM/H",
                        color = MutedPewter,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { viewModel.toggleSpeedUnit() },
                    colors = ButtonDefaults.buttonColors(containerColor = BronzeRim)
                ) {
                    Text(
                        text = if (speedUnit == "mph") "SWITCH TO KM/H" else "SWITCH TO MPH",
                        color = RoyalBrass,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Cockpit Theme Selector
        SettingsSectionHeader(title = "COCKPIT THEME", icon = Icons.Default.Palette)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BronzeRim, shape = RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = GunmetalGauge)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (isDarkTheme) "Midnight Moto Cockpit (Dark)" else "Classic Parchment Day-Dial (Light)",
                        color = WarmIvory,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Persisted in DataStore", color = MutedPewter, fontSize = 12.sp)
                }

                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { viewModel.toggleDarkTheme(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RoyalBrass,
                        checkedTrackColor = BronzeRim
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. ESP32 Telemetry Simulator Control
        SettingsSectionHeader(title = "DEMO SIMULATOR CONTROLS", icon = Icons.Default.Speed)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BronzeRim, shape = RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = GunmetalGauge)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Simulates 16-byte binary GPS telemetry packets every 1 second along Nagpur roads.",
                    color = MutedPewter,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.toggleSimulator(!isSimulatorRunning) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSimulatorRunning) SignalRed else RoyalBrass
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSimulatorRunning) "⏹ STOP LIVE SIMULATOR" else "▶ START LIVE SIMULATOR",
                        color = GunmetalGauge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showQrDialog) {
        QrPairingDialog(
            onDismiss = { showQrDialog = false },
            onPairQrUri = { uri ->
                viewModel.pairDeviceFromQr(
                    qrUriString = uri,
                    onSuccess = { showQrDialog = false },
                    onError = { }
                )
            },
            onPairManual = { id, name, sim ->
                viewModel.addManualDevice(id, name, sim) {
                    showQrDialog = false
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = RoyalBrass)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = RoyalBrass,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

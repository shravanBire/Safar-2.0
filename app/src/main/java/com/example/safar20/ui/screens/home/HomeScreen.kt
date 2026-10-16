package com.example.safar20.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.ui.components.QrPairingDialog
import com.example.safar20.ui.components.TelemetryHeaderPill
import com.example.safar20.ui.components.TripperSpeedometer
import com.example.safar20.ui.theme.CrankcaseBlack
import com.example.safar20.ui.theme.GunmetalGauge
import com.example.safar20.ui.theme.PhosphorGreen
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.SignalRed
import com.example.safar20.ui.theme.WarmIvory
import com.example.safar20.viewModels.SafarViewModel

/**
 * SAFAR 2.0 HomeScreen: Live Retro-Moto Cockpit Dashboard.
 * Integrates TelemetryHeaderPill, MapLibre CockpitMapView, TripperSpeedometer,
 * and live ESP32 mock simulator toggle control.
 */
@Composable
fun HomeScreen(
    viewModel: SafarViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val allDevices by viewModel.allDevices.collectAsState()
    val liveTelemetry by viewModel.liveTelemetry.collectAsState()
    val isSimulatorRunning by viewModel.isSimulatorRunning.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val speedUnit by viewModel.speedUnit.collectAsState()

    var showDeviceMenu by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    // Lat/Lon fallback to Nagpur center or last known location
    val currentLat = liveTelemetry?.latitude ?: selectedDevice?.lastLat ?: 21.1458
    val currentLon = liveTelemetry?.longitude ?: selectedDevice?.lastLon ?: 79.0882
    val currentSpeed = liveTelemetry?.speedKmh ?: selectedDevice?.lastSpeed ?: 0.0f
    val currentHeading = liveTelemetry?.heading ?: 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CrankcaseBlack)
    ) {
        // 1. MapLibre Background Cockpit Map
        CockpitMapView(
            latitude = currentLat,
            longitude = currentLon,
            heading = currentHeading,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Overlay Top Cockpit Bar & Speedometer
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Cockpit Bar
            Box {
                TelemetryHeaderPill(
                    activeDeviceName = selectedDevice?.deviceName ?: "No Device Selected",
                    connectionState = connectionState,
                    latencyMs = if (isSimulatorRunning) 18 else 38,
                    onDeviceClick = { showDeviceMenu = true }
                )

                // Device Switcher Dropdown
                DropdownMenu(
                    expanded = showDeviceMenu,
                    onDismissRequest = { showDeviceMenu = false },
                    modifier = Modifier.background(GunmetalGauge)
                ) {
                    allDevices.forEach { device ->
                        DropdownMenuItem(
                            text = { Text(device.deviceName, color = WarmIvory) },
                            onClick = {
                                viewModel.selectDevice(device.deviceId)
                                showDeviceMenu = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("➕ Pair New Tracker", color = RoyalBrass, fontWeight = FontWeight.Bold) },
                        onClick = {
                            showDeviceMenu = false
                            showQrDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Center Circular Tripper Speedometer Gauge
            TripperSpeedometer(
                speedKmh = currentSpeed,
                unit = speedUnit,
                odoDistanceKm = 18.4,
                gaugeSize = 250.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Simulator Start / Stop Toggle Button
            Button(
                onClick = {
                    viewModel.toggleSimulator(!isSimulatorRunning)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSimulatorRunning) SignalRed else PhosphorGreen
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = if (isSimulatorRunning) "⏹ STOP MOCK SIMULATOR" else "▶ START MOCK SIMULATOR",
                    color = CrankcaseBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // QR Code Pairing Dialog Modal
        if (showQrDialog) {
            QrPairingDialog(
                onDismiss = { showQrDialog = false },
                onPairQrUri = { uri ->
                    viewModel.pairDeviceFromQr(
                        qrUriString = uri,
                        onSuccess = { showQrDialog = false },
                        onError = { /* Error handled */ }
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
}

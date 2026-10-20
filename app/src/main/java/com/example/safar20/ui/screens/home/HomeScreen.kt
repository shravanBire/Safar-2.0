package com.example.safar20.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.ui.components.QrPairingDialog
import com.example.safar20.ui.components.TelemetryHeaderPill
import com.example.safar20.ui.components.TripperSpeedometer
import com.example.safar20.ui.theme.PhosphorGreen
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.SignalRed
import com.example.safar20.viewModels.SafarViewModel
import java.util.Locale

/**
 * SAFAR 2.0 HomeScreen: Live Retro-Moto Cockpit Dashboard.
 * Integrates TelemetryHeaderPill, MapLibre CockpitMapView, Recenter FAB,
 * Retro-Moto Telemetry Mini-Strip, TripperSpeedometer, and live ESP32 simulator toggle.
 */
@Composable
fun HomeScreen(
    viewModel: SafarViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val allDevices by viewModel.allDevices.collectAsState()
    val liveTelemetry by viewModel.liveTelemetry.collectAsState()
    val liveOdoKm by viewModel.liveOdoDistanceKm.collectAsState()
    val isSimulatorRunning by viewModel.isSimulatorRunning.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val speedUnit by viewModel.speedUnit.collectAsState()

    var showDeviceMenu by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    val currentLat = liveTelemetry?.latitude ?: selectedDevice?.lastLat ?: 21.1458
    val currentLon = liveTelemetry?.longitude ?: selectedDevice?.lastLon ?: 79.0882
    val currentSpeed = liveTelemetry?.speedKmh ?: selectedDevice?.lastSpeed ?: 0.0f
    val currentHeading = liveTelemetry?.heading ?: 210
    val satCount = liveTelemetry?.satCount ?: 14

    var mapCenterLat by remember { mutableDoubleStateOf(currentLat) }
    var mapCenterLon by remember { mutableDoubleStateOf(currentLon) }

    LaunchedEffect(currentLat, currentLon) {
        mapCenterLat = currentLat
        mapCenterLon = currentLon
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. MapLibre Background Cockpit Map
        CockpitMapView(
            latitude = mapCenterLat,
            longitude = mapCenterLon,
            heading = currentHeading,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Recenter Map Floating Action Button
        FloatingActionButton(
            onClick = {
                mapCenterLat = currentLat
                mapCenterLon = currentLon
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = RoyalBrass,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 16.dp)
                .size(42.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Recenter Map"
            )
        }

        // 3. Overlay Content Stack
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
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    allDevices.forEach { device ->
                        DropdownMenuItem(
                            text = { Text(device.deviceName, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                viewModel.selectDevice(device.deviceId)
                                showDeviceMenu = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = RoyalBrass)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pair New Tracker", color = RoyalBrass, fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = {
                            showDeviceMenu = false
                            showQrDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Sleek Retro-Moto Telemetry Mini-Strip
            TelemetryMiniStrip(
                heading = currentHeading,
                satellites = satCount,
                latitude = currentLat,
                longitude = currentLon
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Center Circular Tripper Speedometer Gauge with Live Odometer
            TripperSpeedometer(
                speedKmh = currentSpeed,
                unit = speedUnit,
                odoDistanceKm = liveOdoKm,
                gaugeSize = 250.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

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
                    .fillMaxWidth(0.85f)
                    .padding(bottom = 20.dp)
            ) {
                Icon(
                    imageVector = if (isSimulatorRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.background
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSimulatorRunning) "STOP MOCK SIMULATOR" else "START MOCK SIMULATOR",
                    color = MaterialTheme.colorScheme.background,
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
}

@Composable
private fun TelemetryMiniStrip(
    heading: Int,
    satellites: Int,
    latitude: Double,
    longitude: Double
) {
    val compassDir = getCompassDirection(heading)
    val formattedCoords = String.format(Locale.US, "%.4f, %.4f", latitude, longitude)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HDG: $heading° $compassDir",
                color = RoyalBrass,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "SATS: $satellites",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = formattedCoords,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun getCompassDirection(heading: Int): String {
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = ((heading + 22.5) / 45.0).toInt() % 8
    return directions[index]
}

package com.example.safar20.ui.screens.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.data.local.UnitConverter
import com.example.safar20.ui.components.BrassStatBadge
import com.example.safar20.ui.components.ConnectionState
import com.example.safar20.ui.components.QrPairingDialog
import com.example.safar20.ui.components.TelemetryHeaderPill
import com.example.safar20.ui.components.TripperSpeedometer
import com.example.safar20.ui.theme.BronzeRim
import com.example.safar20.ui.theme.CrankcaseBlack
import com.example.safar20.ui.theme.GunmetalGauge
import com.example.safar20.ui.theme.PhosphorGreen
import com.example.safar20.ui.theme.Rajdhani
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.WarmIvory
import com.example.safar20.viewModels.SafarViewModel

/**
 * SAFAR 2.0 HomeScreen: Live Retro-Moto Instrument-Cluster Dashboard.
 * Vertically scrollable dashboard layout with TelemetryHeaderPill,
 * TripperSpeedometer with radial glow, Trip stat badges, and inset map view.
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
    var isMapExpanded by remember { mutableStateOf(false) }

    val currentLat = liveTelemetry?.latitude ?: selectedDevice?.lastLat ?: 21.1458
    val currentLon = liveTelemetry?.longitude ?: selectedDevice?.lastLon ?: 79.0882
    val currentSpeed = liveTelemetry?.speedKmh ?: selectedDevice?.lastSpeed ?: 0.0f
    val currentHeading = liveTelemetry?.heading ?: 210

    var mapCenterLat by remember { mutableDoubleStateOf(currentLat) }
    var mapCenterLon by remember { mutableDoubleStateOf(currentLon) }

    LaunchedEffect(currentLat, currentLon) {
        mapCenterLat = currentLat
        mapCenterLon = currentLon
    }

    val mapHeight by animateDpAsState(
        targetValue = if (isMapExpanded) 380.dp else 200.dp,
        label = "MapHeightAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CrankcaseBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Telemetry Header Pill
            Box(modifier = Modifier.fillMaxWidth()) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Dial zone: TripperSpeedometer with soft radial glow
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    RoyalBrass.copy(alpha = 0.10f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                TripperSpeedometer(
                    speedKmh = currentSpeed,
                    unit = speedUnit,
                    odoDistanceKm = liveOdoKm,
                    gaugeSize = 300.dp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Stat row: TRIP, AVG, MAX
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrassStatBadge(
                    value = UnitConverter.formatDistance(18.4, speedUnit, 1),
                    label = "TRIP",
                    modifier = Modifier.weight(1f)
                )
                BrassStatBadge(
                    value = UnitConverter.formatSpeed(31.0, speedUnit, 0),
                    label = "AVG",
                    modifier = Modifier.weight(1f)
                )
                BrassStatBadge(
                    value = UnitConverter.formatSpeed(78.0, speedUnit, 0),
                    label = "MAX",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Map inset with expandable height and LIVE indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(mapHeight)
                    .border(1.dp, BronzeRim, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(GunmetalGauge)
                    .clickable { isMapExpanded = !isMapExpanded }
            ) {
                CockpitMapView(
                    latitude = mapCenterLat,
                    longitude = mapCenterLon,
                    heading = currentHeading,
                    modifier = Modifier.fillMaxSize()
                )

                val isLive = connectionState == ConnectionState.P2P_DIRECT || connectionState == ConnectionState.SIMULATOR_SYNCING
                if (isLive) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(CrankcaseBlack.copy(alpha = 0.8f), shape = RoundedCornerShape(12.dp))
                            .border(1.dp, BronzeRim, shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(PhosphorGreen, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE",
                            color = PhosphorGreen,
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
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

package com.example.safar20.ui.screens.settings

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.ui.components.BrassCard
import com.example.safar20.ui.components.QrPairingDialog
import com.example.safar20.ui.components.SectionLabel
import com.example.safar20.ui.components.SegmentedToggle
import com.example.safar20.ui.theme.BebasNeue
import com.example.safar20.ui.theme.BronzeRim
import com.example.safar20.ui.theme.CrankcaseBlack
import com.example.safar20.ui.theme.GunmetalGauge
import com.example.safar20.ui.theme.Inter
import com.example.safar20.ui.theme.MutedPewter
import com.example.safar20.ui.theme.PhosphorGreen
import com.example.safar20.ui.theme.Rajdhani
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
            fontFamily = Rajdhani,
            fontWeight = FontWeight.Bold,
            color = WarmIvory,
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
                            color = WarmIvory
                        )
                        Text(
                            text = if (isDarkTheme) "Midnight Moto Cockpit (Dark)" else "Classic Parchment Day-Dial (Light)",
                            fontFamily = Inter,
                            fontSize = 12.sp,
                            color = MutedPewter
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
                            color = WarmIvory
                        )
                        Text(
                            text = if (speedUnit == "mph") "Imperial (Miles, MPH)" else "Metric (Kilometers, KM/H)",
                            fontFamily = Inter,
                            fontSize = 12.sp,
                            color = MutedPewter
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

        // 3. MY TRACKERS
        SectionLabel(text = "MY TRACKERS")
        Spacer(modifier = Modifier.height(8.dp))
        BrassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                if (allDevices.isEmpty()) {
                    Text(
                        text = "NO TRACKERS PAIRED",
                        fontFamily = Rajdhani,
                        fontSize = 12.sp,
                        color = MutedPewter,
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
                                    tint = if (isSelected) RoyalBrass else MutedPewter,
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
                                            color = WarmIvory
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
                                        text = "ID: ${device.deviceId} • SIM: ${device.simNumber}",
                                        fontFamily = Inter,
                                        fontSize = 11.sp,
                                        color = MutedPewter
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.deleteDevice(device.deviceId) }) {
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
                                color = BronzeRim
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showQrDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBrass),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RoyalBrass
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = RoyalBrass,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ PAIR NEW TRACKER",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = RoyalBrass
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. DEVELOPER
        SectionLabel(text = "DEVELOPER")
        Spacer(modifier = Modifier.height(8.dp))
        BrassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "MOCK ESP32 SIMULATOR",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        color = WarmIvory
                    )
                    Text(
                        text = "Simulate live GPS telemetry stream along Nagpur route",
                        fontFamily = Inter,
                        fontSize = 12.sp,
                        color = MutedPewter
                    )
                }
                Switch(
                    checked = isSimulatorRunning,
                    onCheckedChange = { viewModel.toggleSimulator(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CrankcaseBlack,
                        checkedTrackColor = RoyalBrass,
                        uncheckedThumbColor = MutedPewter,
                        uncheckedTrackColor = GunmetalGauge,
                        uncheckedBorderColor = BronzeRim
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. ABOUT
        SectionLabel(text = "ABOUT")
        Spacer(modifier = Modifier.height(8.dp))
        BrassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAFAR 2.0",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 2.sp,
                        color = RoyalBrass
                    )
                    Text(
                        text = "v2.0.4",
                        fontFamily = BebasNeue,
                        fontSize = 16.sp,
                        color = WarmIvory
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Serverless P2P - Zero cloud",
                    fontFamily = Inter,
                    fontSize = 12.sp,
                    color = MutedPewter
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
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

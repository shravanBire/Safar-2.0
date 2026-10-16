package com.example.safar20.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.safar20.ui.theme.BronzeRim
import com.example.safar20.ui.theme.GunmetalGauge
import com.example.safar20.ui.theme.MutedPewter
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.SignalRed
import com.example.safar20.ui.theme.WarmIvory

/**
 * QR Code Pairing Dialog supporting QR payload parsing, manual entry,
 * and a 1-tap "Load Demo Classic 350" preset.
 */
@Composable
fun QrPairingDialog(
    onDismiss: () -> Unit,
    onPairQrUri: (String) -> Unit,
    onPairManual: (id: String, name: String, sim: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = QR Payload, 1 = Manual

    var qrPayloadText by remember { mutableStateOf("") }
    var manualId by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualSim by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val demoPayload = "safar://pair?id=SAFAR_01&name=RE%20Classic%20350&pub=4d8a1c92b8e3f7a1029c83b4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4&sec=a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90&sim=+919876543210"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = GunmetalGauge,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BronzeRim, shape = RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dialog Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Pair Tracker",
                        tint = RoyalBrass
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PAIR SAFAR TRACKER",
                        color = WarmIvory,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs: QR Scan vs Manual Entry
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = GunmetalGauge,
                    contentColor = RoyalBrass
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("QR Payload", color = if (selectedTab == 0) RoyalBrass else MutedPewter) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Manual Entry", color = if (selectedTab == 1) RoyalBrass else MutedPewter) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // QR Payload Tab
                    OutlinedTextField(
                        value = qrPayloadText,
                        onValueChange = {
                            qrPayloadText = it
                            errorMessage = null
                        },
                        label = { Text("Paste QR URI (safar://pair?...)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBrass,
                            unfocusedBorderColor = BronzeRim,
                            focusedTextColor = WarmIvory,
                            unfocusedTextColor = WarmIvory
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Demo Tracker Preset Button
                    Button(
                        onClick = {
                            qrPayloadText = demoPayload
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BronzeRim
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Demo Tracker",
                            tint = RoyalBrass
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("⚡ Load Demo Classic 350 Tracker", color = WarmIvory, fontSize = 12.sp)
                    }
                } else {
                    // Manual Entry Tab
                    OutlinedTextField(
                        value = manualId,
                        onValueChange = { manualId = it },
                        label = { Text("Device ID (e.g. SAFAR_01)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBrass,
                            unfocusedBorderColor = BronzeRim,
                            focusedTextColor = WarmIvory,
                            unfocusedTextColor = WarmIvory
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("Device Name (e.g. Royal Enfield Classic 350)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBrass,
                            unfocusedBorderColor = BronzeRim,
                            focusedTextColor = WarmIvory,
                            unfocusedTextColor = WarmIvory
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualSim,
                        onValueChange = { manualSim = it },
                        label = { Text("GSM SIM Number (e.g. +919876543210)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBrass,
                            unfocusedBorderColor = BronzeRim,
                            focusedTextColor = WarmIvory,
                            unfocusedTextColor = WarmIvory
                        )
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = SignalRed,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MutedPewter)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (selectedTab == 0) {
                                if (qrPayloadText.isBlank()) {
                                    errorMessage = "Please enter or load a valid QR payload URI"
                                } else {
                                    onPairQrUri(qrPayloadText)
                                }
                            } else {
                                if (manualId.isBlank() || manualName.isBlank()) {
                                    errorMessage = "Device ID and Name are required"
                                } else {
                                    onPairManual(manualId, manualName, manualSim)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalBrass
                        )
                    ) {
                        Text("PAIR NOW", color = GunmetalGauge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

package com.example.safar20.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.safar20.data.local.entity.DeviceEntity
import com.example.safar20.ui.theme.Rajdhani
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.SignalRed
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Styled QR Code Pairing Dialog using UiKit components.
 * Supports CameraX QR scanning with 2s retry on invalid QR, inline manual validation,
 * camera settings redirect, and replace confirmation for duplicate device IDs.
 */
@ExperimentalGetImage
@Composable
fun QrPairingDialog(
    existingDevices: List<DeviceEntity> = emptyList(),
    onDismiss: () -> Unit,
    onPairQrUri: (String) -> Unit,
    onPairManual: (id: String, name: String, sim: String, pubKey: String, secretHex: String) -> Unit,
    externalError: String? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Camera Scanner, 1 = QR Text, 2 = Manual

    var qrPayloadText by remember { mutableStateOf("") }
    var manualId by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualSim by remember { mutableStateOf("") }
    var manualPubKey by remember { mutableStateOf("") }
    var manualSecretHex by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingReplaceAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val displayedError = externalError ?: errorMessage

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Pair Tracker",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PAIR SAFAR TRACKER",
                        fontFamily = Rajdhani,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Segmented Toggle using UiKit style
                SegmentedToggle(
                    options = listOf("CAMERA", "QR TEXT", "MANUAL"),
                    selectedIndex = selectedTab,
                    onSelect = {
                        selectedTab = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> {
                        CameraXScannerTab(
                            onQrScanned = { scannedUri ->
                                errorMessage = null
                                val extractedId = extractIdFromUri(scannedUri)
                                if (extractedId != null && existingDevices.any { it.deviceId == extractedId }) {
                                    pendingReplaceAction = { onPairQrUri(scannedUri) }
                                } else {
                                    onPairQrUri(scannedUri)
                                }
                            },
                            onError = { err -> errorMessage = err }
                        )
                    }
                    1 -> {
                        OutlinedTextField(
                            value = qrPayloadText,
                            onValueChange = {
                                qrPayloadText = it
                                errorMessage = null
                            },
                            label = { Text("Paste QR URI (safar://pair?...)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                    2 -> {
                        OutlinedTextField(
                            value = manualId,
                            onValueChange = {
                                manualId = it
                                errorMessage = null
                            },
                            label = { Text("Tracker ID (e.g. SAFAR_01)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualName,
                            onValueChange = {
                                manualName = it
                                errorMessage = null
                            },
                            label = { Text("Tracker Name (e.g. RE Classic 350)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualPubKey,
                            onValueChange = {
                                manualPubKey = it
                                errorMessage = null
                            },
                            label = { Text("Public Key (64 Hex Characters)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualSecretHex,
                            onValueChange = {
                                manualSecretHex = it
                                errorMessage = null
                            },
                            label = { Text("Shared Secret (64 Hex Characters)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualSim,
                            onValueChange = {
                                manualSim = it
                                errorMessage = null
                            },
                            label = { Text("GSM SIM Number (+919876543210, Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (displayedError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SignalRed.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, SignalRed, shape = RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = displayedError,
                            color = SignalRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (selectedTab != 0) {
                        Button(
                            onClick = {
                                if (selectedTab == 1) {
                                    if (qrPayloadText.isBlank()) {
                                        errorMessage = "Please enter a valid QR payload URI"
                                    } else {
                                        val extractedId = extractIdFromUri(qrPayloadText)
                                        if (extractedId != null && existingDevices.any { it.deviceId == extractedId }) {
                                            pendingReplaceAction = { onPairQrUri(qrPayloadText) }
                                        } else {
                                            onPairQrUri(qrPayloadText)
                                        }
                                    }
                                } else if (selectedTab == 2) {
                                    if (manualId.isBlank() || manualName.isBlank()) {
                                        errorMessage = "Tracker ID and Name are required"
                                        return@Button
                                    }
                                    val cleanPub = manualPubKey.trim()
                                    val cleanSec = manualSecretHex.trim()
                                    if (cleanPub.length != 64 || !cleanPub.matches(Regex("^[0-9a-fA-F]{64}$"))) {
                                        errorMessage = "Public Key must be a valid 64-character hex string"
                                        return@Button
                                    }
                                    if (cleanSec.length != 64 || !cleanSec.matches(Regex("^[0-9a-fA-F]{64}$"))) {
                                        errorMessage = "Shared Secret must be a valid 64-character hex string"
                                        return@Button
                                    }

                                    val targetId = manualId.trim()
                                    if (existingDevices.any { it.deviceId == targetId }) {
                                        pendingReplaceAction = {
                                            onPairManual(targetId, manualName.trim(), manualSim.trim(), cleanPub, cleanSec)
                                        }
                                    } else {
                                        onPairManual(targetId, manualName.trim(), manualSim.trim(), cleanPub, cleanSec)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("PAIR NOW", color = MaterialTheme.colorScheme.onPrimary, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (pendingReplaceAction != null) {
        AlertDialog(
            onDismissRequest = { pendingReplaceAction = null },
            title = { Text("Replace Existing Tracker?", fontFamily = Rajdhani, fontWeight = FontWeight.Bold) },
            text = { Text("A tracker with this ID already exists. Pairing will overwrite its configuration and keys.") },
            confirmButton = {
                Button(
                    onClick = {
                        val action = pendingReplaceAction
                        pendingReplaceAction = null
                        action?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("REPLACE", fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingReplaceAction = null }) {
                    Text("CANCEL", fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

private fun extractIdFromUri(qrUriString: String): String? {
    return try {
        val uri = Uri.parse(qrUriString)
        uri.getQueryParameter("id") ?: uri.getQueryParameter("device_id")
    } catch (_: Exception) {
        null
    }
}

@ExperimentalGetImage
@Composable
private fun CameraXScannerTab(
    onQrScanned: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasScanned by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = ContextCompat.getMainExecutor(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = CameraPreview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val scanner = BarcodeScanning.getClient()
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !hasScanned) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        if (!hasScanned) {
                                            for (barcode in barcodes) {
                                                barcode.rawValue?.let { raw ->
                                                    if (raw.startsWith("safar://")) {
                                                        hasScanned = true
                                                        onQrScanned(raw)
                                                        break
                                                    } else {
                                                        hasScanned = true
                                                        onError("Invalid QR Code Scheme. Expected safar://pair")
                                                        scope.launch {
                                                            delay(2000)
                                                            hasScanned = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (_: Exception) {
                        }
                    }, executor)

                    previewView
                },
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = RoyalBrass)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Camera permission required to scan QR code", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("GRANT PERMISSION", fontSize = 11.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("APP SETTINGS", fontSize = 11.sp, fontFamily = Rajdhani, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

package com.example.safar20.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.ui.theme.BronzeRim
import com.example.safar20.ui.theme.GunmetalGauge
import com.example.safar20.ui.theme.MutedPewter
import com.example.safar20.ui.theme.PhosphorGreen
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.SignalRed
import com.example.safar20.ui.theme.WarmIvory

enum class ConnectionState {
    P2P_DIRECT,
    SIMULATOR_SYNCING,
    PARKED,
    OFFLINE
}

/**
 * Top Cockpit Bar with Bike Selector Pill (Left) and Live P2P Connection Status Badge (Right).
 */
@Composable
fun TelemetryHeaderPill(
    activeDeviceName: String = "RE Classic 350",
    connectionState: ConnectionState = ConnectionState.P2P_DIRECT,
    latencyMs: Int? = 38,
    syncProgressPercent: Int? = null,
    lastSeenText: String? = "10m ago",
    onDeviceClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Pill — Active Motorcycle Dropdown Selector
        Row(
            modifier = Modifier
                .background(GunmetalGauge, shape = RoundedCornerShape(20.dp))
                .border(1.dp, BronzeRim, shape = RoundedCornerShape(20.dp))
                .clickable { onDeviceClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.TwoWheeler,
                contentDescription = "Motorcycle",
                tint = RoyalBrass,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = activeDeviceName,
                color = WarmIvory,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Device",
                tint = MutedPewter,
                modifier = Modifier.size(18.dp)
            )
        }

        // Right Pill — Live P2P / Telemetry Status Badge
        val (dotColor, badgeText) = when (connectionState) {
            ConnectionState.P2P_DIRECT -> {
                val msText = if (latencyMs != null) " (${latencyMs}ms)" else ""
                PhosphorGreen to "P2P DIRECT$msText"
            }
            ConnectionState.SIMULATOR_SYNCING -> {
                val percentText = if (syncProgressPercent != null) " ($syncProgressPercent%)" else ""
                RoyalBrass to "SIMULATOR$percentText"
            }
            ConnectionState.PARKED -> {
                val lastText = if (!lastSeenText.isNullOrEmpty()) " ($lastSeenText)" else ""
                MutedPewter to "PARKED$lastText"
            }
            ConnectionState.OFFLINE -> {
                SignalRed to "OFFLINE"
            }
        }

        Row(
            modifier = Modifier
                .background(GunmetalGauge, shape = RoundedCornerShape(20.dp))
                .border(1.dp, BronzeRim, shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing Status Dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, shape = CircleShape)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = badgeText.uppercase(),
                color = dotColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

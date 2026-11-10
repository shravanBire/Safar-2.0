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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.ui.theme.PhosphorGreen
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.SignalRed

sealed interface ConnectionState {
    object NO_DEVICE : ConnectionState
    object BROKER_NOT_CONFIGURED : ConnectionState
    object CONNECTING : ConnectionState
    data class BROKER_ERROR(val message: String) : ConnectionState
    object WAITING_FOR_TRACKER : ConnectionState
    object LIVE : ConnectionState
    data class SYNCING(val progressPercent: Int) : ConnectionState
    data class PARKED(val lastSeenTimestamp: Long?) : ConnectionState
}

/**
 * Top Cockpit Bar with Bike Selector Pill (Left) and Live Telemetry Status Badge (Right).
 */
@Composable
fun TelemetryHeaderPill(
    activeDeviceName: String = "No Tracker",
    connectionState: ConnectionState = ConnectionState.NO_DEVICE,
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
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(20.dp))
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
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Device",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        // Right Pill — Live Telemetry Status Badge
        val (dotColor, badgeText) = when (connectionState) {
            is ConnectionState.LIVE -> {
                PhosphorGreen to "● LIVE"
            }
            is ConnectionState.SYNCING -> {
                RoyalBrass to "● SYNCING LOGS (${connectionState.progressPercent}%)"
            }
            is ConnectionState.PARKED -> {
                val timeAgo = formatTimeAgo(connectionState.lastSeenTimestamp)
                val label = if (timeAgo != null) "● PARKED • LAST SEEN $timeAgo" else "● PARKED"
                MaterialTheme.colorScheme.onSurfaceVariant to label
            }
            is ConnectionState.CONNECTING -> {
                RoyalBrass to "● CONNECTING…"
            }
            is ConnectionState.WAITING_FOR_TRACKER -> {
                RoyalBrass to "● WAITING FOR TRACKER"
            }
            is ConnectionState.BROKER_ERROR -> {
                SignalRed to "● BROKER ERROR"
            }
            is ConnectionState.BROKER_NOT_CONFIGURED -> {
                MaterialTheme.colorScheme.onSurfaceVariant to "● SET UP BROKER"
            }
            is ConnectionState.NO_DEVICE -> {
                MaterialTheme.colorScheme.onSurfaceVariant to "● NO TRACKER"
            }
        }

        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

private fun formatTimeAgo(timestampSeconds: Long?): String? {
    if (timestampSeconds == null || timestampSeconds <= 0L) return null
    val diffSec = (System.currentTimeMillis() / 1000) - timestampSeconds
    if (diffSec < 60) return "JUST NOW"
    val minutes = diffSec / 60
    if (minutes < 60) return "${minutes}m AGO"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h AGO"
    val days = hours / 24
    return "${days}d AGO"
}

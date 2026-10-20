package com.example.safar20.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.data.local.UnitConverter
import com.example.safar20.data.local.entity.TripSummaryEntity
import com.example.safar20.ui.theme.RoyalBrass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Vintage Highway Logbook Ticket Card displaying ride details, duration badge,
 * and 3-column brass stat grid with unit conversion support.
 */
@Composable
fun RideTicketCard(
    trip: TripSummaryEntity,
    speedUnit: String = "km/h",
    isSelected: Boolean = false,
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) RoyalBrass else MaterialTheme.colorScheme.outline
    val borderWidth = if (isSelected) 2.dp else 1.dp

    val formattedTimeRange = formatTimeRange(trip.startTimestamp, trip.endTimestamp)
    val rideNumber = trip.tripId.takeLast(2).uppercase()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(borderWidth, borderColor, shape = RoundedCornerShape(12.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Row: Ride ID + Time Range + Duration Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RIDE #$rideNumber",
                        color = RoyalBrass,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedTimeRange,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Duration Badge
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${trip.durationMinutes} MINS",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 3-Column Brass Stat Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Column 1: Distance
                StatBox(
                    label = "DISTANCE",
                    value = UnitConverter.formatDistance(trip.distanceKm, speedUnit, decimals = 1),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Column 2: Avg Speed
                StatBox(
                    label = "AVG SPEED",
                    value = UnitConverter.formatSpeed(trip.avgSpeedKmh, speedUnit, decimals = 0),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Column 3: Max Speed
                StatBox(
                    label = "MAX SPEED",
                    value = UnitConverter.formatSpeed(trip.maxSpeedKmh, speedUnit, decimals = 0),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), shape = RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatTimeRange(startTimestampSec: Long, endTimestampSec: Long): String {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    val startTime = timeFormat.format(Date(startTimestampSec * 1000))
    val endTime = timeFormat.format(Date(endTimestampSec * 1000))
    return "$startTime -> $endTime"
}

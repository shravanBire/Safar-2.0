package com.example.safar20.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.data.local.UnitConverter
import com.example.safar20.data.local.entity.LocationPointEntity
import com.example.safar20.data.local.entity.TripSummaryEntity
import com.example.safar20.ui.screens.home.CockpitMapView
import com.example.safar20.ui.theme.BebasNeue
import com.example.safar20.ui.theme.BronzeRim
import com.example.safar20.ui.theme.CrankcaseBlack
import com.example.safar20.ui.theme.GunmetalGauge
import com.example.safar20.ui.theme.Inter
import com.example.safar20.ui.theme.MutedPewter
import com.example.safar20.ui.theme.Rajdhani
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.WarmIvory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketShape(
    private val cornerRadius: Dp = 12.dp,
    private val notchRadius: Dp = 8.dp,
    private val notchOffsetY: Dp = 54.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val cRad = with(density) { cornerRadius.toPx() }
            val nRad = with(density) { notchRadius.toPx() }
            val nY = with(density) { notchOffsetY.toPx() }

            // Top left corner
            moveTo(cRad, 0f)
            lineTo(size.width - cRad, 0f)
            quadraticTo(size.width, 0f, size.width, cRad)

            // Right side down to notch
            lineTo(size.width, nY - nRad)
            // Right notch arc
            arcTo(
                rect = Rect(
                    left = size.width - nRad,
                    top = nY - nRad,
                    right = size.width + nRad,
                    bottom = nY + nRad
                ),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            // Right side down to bottom
            lineTo(size.width, size.height - cRad)
            quadraticTo(size.width, size.height, size.width - cRad, size.height)

            // Bottom edge
            lineTo(cRad, size.height)
            quadraticTo(0f, size.height, 0f, size.height - cRad)

            // Left side up to notch
            lineTo(0f, nY + nRad)
            // Left notch arc
            arcTo(
                rect = Rect(
                    left = -nRad,
                    top = nY - nRad,
                    right = nRad,
                    bottom = nY + nRad
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            // Left side up to top
            lineTo(0f, cRad)
            quadraticTo(0f, 0f, cRad, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Vintage Highway Logbook Ticket Card displaying ride details, duration badge,
 * dashed divider with notch cutouts, 3-column stats, and expandable route map.
 */
@Composable
fun RideTicketCard(
    trip: TripSummaryEntity,
    speedUnit: String = "km/h",
    isSelected: Boolean = false,
    routePoints: List<LocationPointEntity> = emptyList(),
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ticketShape = remember { TicketShape(cornerRadius = 12.dp, notchRadius = 8.dp, notchOffsetY = 54.dp) }
    val borderColor = if (isSelected) RoyalBrass else BronzeRim
    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    val formattedTimeRange = formatTimeRange(trip.startTimestamp, trip.endTimestamp)
    val rideNumber = trip.tripId.takeLast(2).uppercase()

    val mapLat = routePoints.firstOrNull()?.latitude ?: 21.1458
    val mapLon = routePoints.firstOrNull()?.longitude ?: 79.0882

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(borderWidth, borderColor, ticketShape)
            .clip(ticketShape)
            .clickable { onCardClick() },
        shape = ticketShape,
        color = GunmetalGauge
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(vertical = 12.dp)
        ) {
            // Top Header Row: Ride ID + Time Range + Duration Chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RIDE #$rideNumber",
                        fontFamily = BebasNeue,
                        fontSize = 20.sp,
                        color = RoyalBrass,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = formattedTimeRange,
                        fontFamily = Inter,
                        fontSize = 12.sp,
                        color = WarmIvory
                    )
                }

                // Duration Chip
                Box(
                    modifier = Modifier
                        .background(CrankcaseBlack, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, BronzeRim, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${trip.durationMinutes} MINS",
                        color = WarmIvory,
                        fontFamily = BebasNeue,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dashed Divider
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
            ) {
                drawLine(
                    color = BronzeRim,
                    start = Offset(14.dp.toPx(), 0f),
                    end = Offset(size.width - 14.dp.toPx(), 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Columns: DISTANCE / AVG SPEED / MAX SPEED
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TicketStatColumn(
                    label = "DISTANCE",
                    value = UnitConverter.formatDistance(trip.distanceKm, speedUnit, decimals = 1),
                    modifier = Modifier.weight(1f)
                )
                TicketStatColumn(
                    label = "AVG SPEED",
                    value = UnitConverter.formatSpeed(trip.avgSpeedKmh, speedUnit, decimals = 0),
                    modifier = Modifier.weight(1f)
                )
                TicketStatColumn(
                    label = "MAX SPEED",
                    value = UnitConverter.formatSpeed(trip.maxSpeedKmh, speedUnit, decimals = 0),
                    modifier = Modifier.weight(1f)
                )
            }

            // Expanded Route Map Preview
            if (isSelected && routePoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(180.dp)
                        .border(1.dp, BronzeRim, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(CrankcaseBlack)
                ) {
                    CockpitMapView(
                        latitude = mapLat,
                        longitude = mapLon,
                        routePoints = routePoints,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketStatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label.uppercase(),
            fontFamily = Rajdhani,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = MutedPewter,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            fontFamily = BebasNeue,
            fontSize = 22.sp,
            color = WarmIvory,
            lineHeight = 24.sp
        )
    }
}

private fun formatTimeRange(startTimestampSec: Long, endTimestampSec: Long): String {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    val startTime = timeFormat.format(Date(startTimestampSec * 1000))
    val endTime = timeFormat.format(Date(endTimestampSec * 1000))
    return "$startTime -> $endTime"
}

package com.example.safar20.ui.screens.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.data.local.entity.TripSummaryEntity
import com.example.safar20.ui.components.RideTicketCard
import com.example.safar20.ui.components.SectionLabel
import com.example.safar20.ui.theme.BebasNeue
import com.example.safar20.ui.theme.Rajdhani
import com.example.safar20.viewModels.SafarViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Logbook Screen displaying vintage highway ride tickets, 14-day horizontal date selector,
 * and expandable route map previews.
 */
@Composable
fun TripsScreen(
    viewModel: SafarViewModel,
    modifier: Modifier = Modifier
) {
    val tripHistory by viewModel.tripHistory.collectAsState()
    val selectedTripId by viewModel.selectedTripId.collectAsState()
    val selectedTripPoints by viewModel.selectedTripPoints.collectAsState()
    val speedUnit by viewModel.speedUnit.collectAsState()

    // Generate last 14 days calendar list
    val dateList = remember {
        (0..13).map { daysAgo ->
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
    }

    // 0 = ALL, 1..14 = dateList[0..13]
    var selectedDateIndex by remember { mutableIntStateOf(0) }

    val selectedDateText = remember(selectedDateIndex) {
        if (selectedDateIndex == 0) {
            "ALL HISTORICAL RIDES"
        } else {
            val cal = dateList[selectedDateIndex - 1]
            val fmt = SimpleDateFormat("dd MMM yyyy", Locale.US)
            fmt.format(cal.time).uppercase()
        }
    }

    // Sample/fake rides fallback if db is empty
    val effectiveTrips = remember(tripHistory) {
        if (tripHistory.isNotEmpty()) {
            tripHistory
        } else {
            val now = System.currentTimeMillis() / 1000
            val oneDaySec = 86400L
            listOf(
                TripSummaryEntity(
                    tripId = "TRIP_01",
                    deviceId = "SAFAR_01",
                    startTimestamp = now - 3600 * 2,
                    endTimestamp = now - 3600 * 1,
                    durationMinutes = 60,
                    distanceKm = 42.5,
                    avgSpeedKmh = 42.5,
                    maxSpeedKmh = 88.0
                ),
                TripSummaryEntity(
                    tripId = "TRIP_02",
                    deviceId = "SAFAR_01",
                    startTimestamp = now - oneDaySec - 3600 * 2,
                    endTimestamp = now - oneDaySec - 3600 * 1,
                    durationMinutes = 35,
                    distanceKm = 18.4,
                    avgSpeedKmh = 31.5,
                    maxSpeedKmh = 78.0
                )
            )
        }
    }

    // Auto-select first trip on load
    LaunchedEffect(effectiveTrips) {
        if (selectedTripId == null && effectiveTrips.isNotEmpty()) {
            viewModel.selectTripForMapPreview(effectiveTrips.first().tripId)
        }
    }

    // Strict filter for selected date (without fallback when empty)
    val filteredTrips = remember(effectiveTrips, selectedDateIndex) {
        if (selectedDateIndex == 0) {
            effectiveTrips
        } else {
            val cal = dateList[selectedDateIndex - 1]
            val startSec = cal.timeInMillis / 1000
            val endSec = startSec + 86400L
            effectiveTrips.filter { it.startTimestamp in startSec..endSec }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Screen Header: SectionLabel + Bebas Date Title
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SectionLabel("LOGBOOK")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = selectedDateText,
                fontFamily = BebasNeue,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 2. Horizontal Scrolling Date Chip Strip with "ALL" at Index 0
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(1 + dateList.size) { index ->
                val isSelected = (index == selectedDateIndex)

                val dayOfWeek = if (index == 0) "RIDES" else SimpleDateFormat("EEE", Locale.US).format(dateList[index - 1].time).uppercase()
                val dayNum = if (index == 0) "ALL" else SimpleDateFormat("dd", Locale.US).format(dateList[index - 1].time)

                Box(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .clickable { selectedDateIndex = index }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayOfWeek,
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dayNum,
                            fontFamily = BebasNeue,
                            fontSize = 20.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Logbook Ride Tickets or Empty State
        if (filteredTrips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "NO RIDES LOGGED",
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = filteredTrips,
                    key = { it.tripId }
                ) { trip ->
                    val isSelected = (trip.tripId == selectedTripId)
                    RideTicketCard(
                        trip = trip,
                        speedUnit = speedUnit,
                        isSelected = isSelected,
                        routePoints = if (isSelected) selectedTripPoints else emptyList(),
                        onCardClick = {
                            viewModel.selectTripForMapPreview(
                                if (isSelected) null else trip.tripId
                            )
                        }
                    )
                }
            }
        }
    }
}

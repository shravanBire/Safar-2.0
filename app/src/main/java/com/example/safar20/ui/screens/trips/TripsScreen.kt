package com.example.safar20.ui.screens.trips

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.ui.components.RideTicketCard
import com.example.safar20.ui.screens.home.CockpitMapView
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.viewModels.SafarViewModel
import java.util.Calendar

/**
 * Trips Logbook Screen with date filter bar, auto-selected first trip polyline,
 * and MaterialTheme color scheme support.
 */
@Composable
fun TripsScreen(
    viewModel: SafarViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val tripHistory by viewModel.tripHistory.collectAsState()
    val selectedTripId by viewModel.selectedTripId.collectAsState()
    val selectedTripPoints by viewModel.selectedTripPoints.collectAsState()
    val speedUnit by viewModel.speedUnit.collectAsState()

    var activeFilter by remember { mutableStateOf("ALL") } // "ALL", "YESTERDAY", "CUSTOM"
    var customDateCalendar by remember { mutableStateOf<Calendar?>(null) }

    // Auto-select first trip on initial load
    LaunchedEffect(tripHistory) {
        if (selectedTripId == null && tripHistory.isNotEmpty()) {
            viewModel.selectTripForMapPreview(tripHistory.first().tripId)
        }
    }

    // Filter tripHistory based on activeFilter
    val filteredTrips = remember(tripHistory, activeFilter, customDateCalendar) {
        val now = System.currentTimeMillis() / 1000
        val oneDaySec = 86400L

        when (activeFilter) {
            "YESTERDAY" -> {
                val yesterdayStart = now - (2 * oneDaySec)
                val yesterdayEnd = now
                tripHistory.filter { it.startTimestamp in yesterdayStart..yesterdayEnd }
            }
            "CUSTOM" -> {
                val cal = customDateCalendar
                if (cal != null) {
                    val startSec = cal.timeInMillis / 1000
                    val endSec = startSec + oneDaySec
                    tripHistory.filter { it.startTimestamp in startSec..endSec }
                } else tripHistory
            }
            else -> tripHistory
        }
    }

    val mapCenterLat = selectedTripPoints.firstOrNull()?.latitude ?: selectedDevice?.lastLat ?: 21.1458
    val mapCenterLon = selectedTripPoints.firstOrNull()?.longitude ?: selectedDevice?.lastLon ?: 79.0882

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RIDE LOGBOOK",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Text(
                text = selectedDevice?.deviceName ?: "No Device",
                color = RoyalBrass,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Upper Section: Map Preview with Route Polyline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .padding(horizontal = 16.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(12.dp))
        ) {
            CockpitMapView(
                latitude = mapCenterLat,
                longitude = mapCenterLon,
                routePoints = selectedTripPoints,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Date Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = activeFilter == "ALL",
                onClick = { activeFilter = "ALL" },
                label = { Text("ALL RIDES", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RoyalBrass,
                    selectedLabelColor = MaterialTheme.colorScheme.background
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilterChip(
                selected = activeFilter == "YESTERDAY",
                onClick = { activeFilter = "YESTERDAY" },
                label = { Text("RECENT", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RoyalBrass,
                    selectedLabelColor = MaterialTheme.colorScheme.background
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilterChip(
                selected = activeFilter == "CUSTOM",
                onClick = {
                    val calendar = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selectedCal = Calendar.getInstance()
                            selectedCal.set(year, month, dayOfMonth, 0, 0, 0)
                            customDateCalendar = selectedCal
                            activeFilter = "CUSTOM"
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp).height(12.dp)
                        )
                        Text("PICK DATE", fontSize = 11.sp)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RoyalBrass,
                    selectedLabelColor = MaterialTheme.colorScheme.background
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Lower Section: Ride Cards List
        if (filteredTrips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NO HISTORICAL RIDES FOUND",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.toggleSimulator(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBrass)
                    ) {
                        Text("GENERATE SAMPLE RIDES", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = filteredTrips,
                    key = { it.tripId }
                ) { trip ->
                    RideTicketCard(
                        trip = trip,
                        speedUnit = speedUnit,
                        isSelected = (trip.tripId == selectedTripId),
                        onCardClick = {
                            viewModel.selectTripForMapPreview(
                                if (selectedTripId == trip.tripId) null else trip.tripId
                            )
                        }
                    )
                }
            }
        }
    }
}

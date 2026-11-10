package com.example.safar20.ui.screens.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.safar20.ui.components.RideTicketCard
import com.example.safar20.ui.components.SectionLabel
import com.example.safar20.ui.theme.BebasNeue
import com.example.safar20.ui.theme.PhosphorGreen
import com.example.safar20.ui.theme.Rajdhani
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.viewModels.SafarViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Logbook Screen displaying vintage highway ride tickets, 14-day horizontal date selector,
 * indicator dots for dates with rides, DEMO DATA badge, and route map previews.
 */
@Composable
fun TripsScreen(
    viewModel: SafarViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val tripHistory by viewModel.tripHistory.collectAsState()
    val selectedTripId by viewModel.selectedTripId.collectAsState()
    val selectedTripPoints by viewModel.selectedTripPoints.collectAsState()
    val speedUnit by viewModel.speedUnit.collectAsState()
    val historySyncProgress by viewModel.historySyncProgress.collectAsState()

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

    val isDemoData = remember(tripHistory) {
        tripHistory.any { it.tripId.startsWith("DEMO_") }
    }

    // Auto-select first trip on load
    LaunchedEffect(tripHistory) {
        if (selectedTripId == null && tripHistory.isNotEmpty()) {
            viewModel.selectTripForMapPreview(tripHistory.first().tripId)
        }
    }

    // Filter trips for selected date
    val filteredTrips = remember(tripHistory, selectedDateIndex) {
        if (selectedDateIndex == 0) {
            tripHistory
        } else {
            val cal = dateList[selectedDateIndex - 1]
            val startSec = cal.timeInMillis / 1000
            val endSec = startSec + 86400L
            tripHistory.filter { it.startTimestamp in startSec..endSec }
        }
    }

    // Map of dates having rides
    val datesWithRidesSet = remember(tripHistory) {
        tripHistory.map { trip ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = trip.startTimestamp * 1000L
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }.toSet()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Screen Header: SectionLabel + DEMO DATA Badge + Bebas Date Title
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("LOGBOOK")
                if (isDemoData) {
                    Box(
                        modifier = Modifier
                            .background(RoyalBrass.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, RoyalBrass, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DEMO DATA",
                            color = RoyalBrass,
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = selectedDateText,
                fontFamily = BebasNeue,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 2. Horizontal Scrolling Date Chip Strip with Indicator Dots
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(1 + dateList.size) { index ->
                val isSelected = (index == selectedDateIndex)
                val hasRidesOnDate = if (index == 0) {
                    tripHistory.isNotEmpty()
                } else {
                    datesWithRidesSet.contains(dateList[index - 1].timeInMillis)
                }

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = dayOfWeek,
                                fontFamily = Rajdhani,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (hasRidesOnDate) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else PhosphorGreen, shape = CircleShape)
                                )
                            }
                        }
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
        when {
            selectedDevice == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO TRACKER SELECTED",
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pair or select a tracker to view stored ride logs",
                            fontFamily = Rajdhani,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            historySyncProgress != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = RoyalBrass,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "SYNCING RIDE LOGS (${historySyncProgress}%)",
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CircularProgressIndicator(
                            color = RoyalBrass,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            filteredTrips.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedDateIndex == 0) "NO RIDES LOGGED YET" else "NO RIDES FOR THIS DATE",
                            fontFamily = Rajdhani,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
            else -> {
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
}

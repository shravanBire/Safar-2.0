package com.example.safar20.ui.screens.trips

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.ui.components.RideTicketCard
import com.example.safar20.ui.screens.home.CockpitMapView
import com.example.safar20.ui.theme.BronzeRim
import com.example.safar20.ui.theme.CrankcaseBlack
import com.example.safar20.ui.theme.GunmetalGauge
import com.example.safar20.ui.theme.MutedPewter
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.WarmIvory
import com.example.safar20.viewModels.SafarViewModel

/**
 * Trips Logbook Screen showing historical ride cards and rendering trip route polylines on MapLibre.
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

    val mapCenterLat = selectedTripPoints.firstOrNull()?.latitude ?: selectedDevice?.lastLat ?: 21.1458
    val mapCenterLon = selectedTripPoints.firstOrNull()?.longitude ?: selectedDevice?.lastLon ?: 79.0882

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CrankcaseBlack)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RIDE LOGBOOK",
                color = WarmIvory,
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
                .fillMaxHeight(0.38f)
                .padding(horizontal = 16.dp)
                .border(1.dp, BronzeRim, shape = RoundedCornerShape(12.dp))
        ) {
            CockpitMapView(
                latitude = mapCenterLat,
                longitude = mapCenterLon,
                routePoints = selectedTripPoints,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Lower Section: Ride Cards List
        if (tripHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NO HISTORICAL RIDES FOUND",
                        color = MutedPewter,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.toggleSimulator(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBrass)
                    ) {
                        Text("⚡ GENERATE SAMPLE RIDES", color = GunmetalGauge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = tripHistory,
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

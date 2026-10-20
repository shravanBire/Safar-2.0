package com.example.safar20.ui.screens.home

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.safar20.data.local.entity.LocationPointEntity
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * MapLibre Native MapView for Cockpit & Trips preview.
 * Draws markers and route polylines in the update block per SAFAR 2.0 specs.
 */
@Composable
fun CockpitMapView(
    latitude: Double,
    longitude: Double,
    heading: Int = 0,
    routePoints: List<LocationPointEntity> = emptyList(),
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val context = LocalContext.current

    remember {
        try {
            MapLibre.getInstance(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentMarker by remember { mutableStateOf<Marker?>(null) }
    var currentPolyline by remember { mutableStateOf<Polyline?>(null) }
    var startMarker by remember { mutableStateOf<Marker?>(null) }
    var endMarker by remember { mutableStateOf<Marker?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                onCreate(null)
                getMapAsync { map ->
                    map.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/liberty")) {
                        map.uiSettings.isCompassEnabled = false
                        map.uiSettings.isAttributionEnabled = false
                        map.uiSettings.isLogoEnabled = false

                        val initialPos = LatLng(latitude, longitude)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(initialPos, 14.0))
                    }
                }
            }
        },
        update = { view ->
            view.getMapAsync { map ->
                map.getStyle {
                    if (routePoints.isNotEmpty()) {
                        currentMarker?.let { map.removeAnnotation(it) }
                        currentMarker = null

                        currentPolyline?.let { map.removeAnnotation(it) }
                        startMarker?.let { map.removeAnnotation(it) }
                        endMarker?.let { map.removeAnnotation(it) }

                        val latLngList = routePoints.map { LatLng(it.latitude, it.longitude) }

                        if (latLngList.size >= 2) {
                            val polylineOptions = PolylineOptions()
                                .addAll(latLngList)
                                .color(Color.parseColor("#C86D51"))
                                .width(6f)

                            currentPolyline = map.addPolyline(polylineOptions)

                            startMarker = map.addMarker(
                                MarkerOptions()
                                    .position(latLngList.first())
                                    .title("Trip Start")
                            )
                            endMarker = map.addMarker(
                                MarkerOptions()
                                    .position(latLngList.last())
                                    .title("Trip End")
                            )

                            try {
                                val boundsBuilder = LatLngBounds.Builder()
                                latLngList.forEach { boundsBuilder.include(it) }
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
                            } catch (e: Exception) {
                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngList.first(), 14.0))
                            }
                        }
                    } else if (latitude != 0.0 && longitude != 0.0) {
                        currentPolyline?.let { map.removeAnnotation(it) }
                        startMarker?.let { map.removeAnnotation(it) }
                        endMarker?.let { map.removeAnnotation(it) }
                        currentPolyline = null
                        startMarker = null
                        endMarker = null

                        val currentPos = LatLng(latitude, longitude)

                        val marker = currentMarker
                        if (marker != null) {
                            marker.position = currentPos
                        } else {
                            currentMarker = map.addMarker(
                                MarkerOptions()
                                    .position(currentPos)
                                    .title("Royal Enfield Classic 350")
                            )
                        }

                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPos, 15.0))
                    }
                }
            }
        },
        modifier = modifier
    )
}

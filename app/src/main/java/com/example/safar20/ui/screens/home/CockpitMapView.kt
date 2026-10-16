package com.example.safar20.ui.screens.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.safar20.data.local.entity.LocationPointEntity
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * MapLibre Native MapView for Cockpit & Trips preview.
 * Polyline and marker updates occur strictly inside the AndroidView `update` block per SAFAR 2.0 specs.
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
    remember { MapLibre.getInstance(context) }

    val mapView = remember { MapView(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

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
                getMapAsync { map ->
                    map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")) {
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
                        val latLngList = routePoints.map { LatLng(it.latitude, it.longitude) }

                        if (latLngList.size >= 2) {
                            try {
                                val boundsBuilder = LatLngBounds.Builder()
                                latLngList.forEach { boundsBuilder.include(it) }
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
                            } catch (e: Exception) {
                                // Fallback
                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngList.first(), 14.0))
                            }
                        }
                    } else if (latitude != 0.0 && longitude != 0.0) {
                        val currentPos = LatLng(latitude, longitude)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPos, 15.0))
                    }
                }
            }
        },
        modifier = modifier
    )
}

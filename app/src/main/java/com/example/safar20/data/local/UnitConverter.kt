package com.example.safar20.data.local

import java.util.Locale

object UnitConverter {

    const val KM_TO_MILES_FACTOR: Double = 0.621371
    const val UNIT_KMH = "km/h"
    const val UNIT_MPH = "mph"

    /**
     * Converts speed from km/h to selected unit (mph if unit is "mph").
     */
    fun convertSpeed(speedKmh: Double, unit: String): Double {
        return if (isMph(unit)) {
            speedKmh * KM_TO_MILES_FACTOR
        } else {
            speedKmh
        }
    }

    /**
     * Converts Float speed from km/h to selected unit (mph if unit is "mph").
     */
    fun convertSpeed(speedKmh: Float, unit: String): Float {
        return if (isMph(unit)) {
            (speedKmh * KM_TO_MILES_FACTOR).toFloat()
        } else {
            speedKmh
        }
    }

    /**
     * Converts distance from km to selected unit (miles if unit is "mph").
     */
    fun convertDistance(distanceKm: Double, unit: String): Double {
        return if (isMph(unit)) {
            distanceKm * KM_TO_MILES_FACTOR
        } else {
            distanceKm
        }
    }

    /**
     * Formats speed value with unit label (e.g. "65 km/h" or "40 mph").
     */
    fun formatSpeed(speedKmh: Double, unit: String, decimals: Int = 0): String {
        val converted = convertSpeed(speedKmh, unit)
        val label = getSpeedUnitLabel(unit)
        return String.format(Locale.US, "%.${decimals}f %s", converted, label)
    }

    /**
     * Formats distance value with unit label (e.g. "12.4 km" or "7.7 mi").
     */
    fun formatDistance(distanceKm: Double, unit: String, decimals: Int = 1): String {
        val converted = convertDistance(distanceKm, unit)
        val label = getDistanceUnitLabel(unit)
        return String.format(Locale.US, "%.${decimals}f %s", converted, label)
    }

    /**
     * Returns speed unit display label ("km/h" or "mph").
     */
    fun getSpeedUnitLabel(unit: String): String {
        return if (isMph(unit)) "mph" else "km/h"
    }

    /**
     * Returns distance unit display label ("km" or "mi").
     */
    fun getDistanceUnitLabel(unit: String): String {
        return if (isMph(unit)) "mi" else "km"
    }

    private fun isMph(unit: String): Boolean {
        return unit.equals(UNIT_MPH, ignoreCase = true)
    }
}

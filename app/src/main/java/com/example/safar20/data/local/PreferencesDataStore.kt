package com.example.safar20.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safar_preferences")

class PreferencesDataStore(private val context: Context) {

    companion object {
        val KEY_IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val KEY_SPEED_UNIT = stringPreferencesKey("speed_unit")

        const val UNIT_KMH = "km/h"
        const val UNIT_MPH = "mph"
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_IS_DARK_THEME] ?: true // Default to Midnight Moto Cockpit Dark Theme
        }

    val speedUnit: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SPEED_UNIT] ?: UNIT_KMH
        }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_DARK_THEME] = isDark
        }
    }

    suspend fun setSpeedUnit(unit: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SPEED_UNIT] = if (unit == UNIT_MPH) UNIT_MPH else UNIT_KMH
        }
    }

    suspend fun toggleSpeedUnit() {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_SPEED_UNIT] ?: UNIT_KMH
            preferences[KEY_SPEED_UNIT] = if (current == UNIT_KMH) UNIT_MPH else UNIT_KMH
        }
    }
}

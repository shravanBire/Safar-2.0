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
        val KEY_MQTT_BROKER_URI = stringPreferencesKey("mqtt_broker_uri")
        val KEY_MQTT_USERNAME = stringPreferencesKey("mqtt_username")
        val KEY_MQTT_PASSWORD = stringPreferencesKey("mqtt_password")
        val KEY_IS_DEV_MODE = booleanPreferencesKey("is_dev_mode")

        const val UNIT_KMH = "km/h"
        const val UNIT_MPH = "mph"
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_IS_DARK_THEME] ?: true
        }

    val speedUnit: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SPEED_UNIT] ?: UNIT_KMH
        }

    val mqttBrokerUri: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_MQTT_BROKER_URI] ?: ""
        }

    val mqttUsername: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_MQTT_USERNAME] ?: ""
        }

    val mqttPassword: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_MQTT_PASSWORD] ?: ""
        }

    val isDevModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_IS_DEV_MODE] ?: false
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

    suspend fun setMqttBrokerUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MQTT_BROKER_URI] = uri.trim()
        }
    }

    suspend fun setMqttUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MQTT_USERNAME] = username.trim()
        }
    }

    suspend fun setMqttPassword(password: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MQTT_PASSWORD] = password
        }
    }

    suspend fun setDevModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_DEV_MODE] = enabled
        }
    }
}

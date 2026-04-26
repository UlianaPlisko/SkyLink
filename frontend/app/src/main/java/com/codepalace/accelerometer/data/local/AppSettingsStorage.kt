package com.codepalace.accelerometer.data.local

import android.content.Context

class AppSettingsStorage(context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    var redModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_RED_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_RED_MODE, value).apply()

    var automaticSensors: Boolean
        get() = prefs.getBoolean(KEY_AUTOMATIC_SENSORS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTOMATIC_SENSORS, value).apply()

    var autoLocationEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LOCATION, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LOCATION, value).apply()

    var latitude: String
        get() = prefs.getString(KEY_LATITUDE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_LATITUDE, value).apply()

    var longitude: String
        get() = prefs.getString(KEY_LONGITUDE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_LONGITUDE, value).apply()

    var cityName: String
        get() = prefs.getString(KEY_CITY_NAME, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_CITY_NAME, value).apply()

    fun manualLatitude(): Double? = latitude.toDoubleOrNull()

    fun manualLongitude(): Double? = longitude.toDoubleOrNull()

    companion object {
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_RED_MODE = "red_mode"
        private const val KEY_AUTOMATIC_SENSORS = "automatic_sensors"
        private const val KEY_AUTO_LOCATION = "auto_location"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_CITY_NAME = "city_name"
    }
}

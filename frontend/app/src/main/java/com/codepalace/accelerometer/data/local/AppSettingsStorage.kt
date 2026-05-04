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
        get() = coordinateTextOrDefault(
            prefs.getString(KEY_LATITUDE, DEFAULT_LATITUDE_TEXT),
            DEFAULT_LATITUDE_TEXT,
            MIN_LATITUDE,
            MAX_LATITUDE
        )
        set(value) = prefs.edit().putString(KEY_LATITUDE, value.trim()).apply()

    var longitude: String
        get() = coordinateTextOrDefault(
            prefs.getString(KEY_LONGITUDE, DEFAULT_LONGITUDE_TEXT),
            DEFAULT_LONGITUDE_TEXT,
            MIN_LONGITUDE,
            MAX_LONGITUDE
        )
        set(value) = prefs.edit().putString(KEY_LONGITUDE, value.trim()).apply()

//    var cityName: String
//        get() = prefs.getString(KEY_CITY_NAME, "").orEmpty()
//        set(value) = prefs.edit().putString(KEY_CITY_NAME, value).apply()

    fun manualLatitude(): Double? = parseLatitude(latitude)

    fun manualLongitude(): Double? = parseLongitude(longitude)

    fun useDefaultLocation() {
        prefs.edit()
            .putBoolean(KEY_AUTO_LOCATION, false)
            .putString(KEY_LATITUDE, DEFAULT_LATITUDE_TEXT)
            .putString(KEY_LONGITUDE, DEFAULT_LONGITUDE_TEXT)
            .apply()
    }

    companion object {
        const val DEFAULT_LOCATION_NAME = "Bratislava"
        const val DEFAULT_LATITUDE = 48.1486
        const val DEFAULT_LONGITUDE = 17.1077
        const val DEFAULT_LATITUDE_TEXT = "48.1486"
        const val DEFAULT_LONGITUDE_TEXT = "17.1077"

        private const val MIN_LATITUDE = -90.0
        private const val MAX_LATITUDE = 90.0
        private const val MIN_LONGITUDE = -180.0
        private const val MAX_LONGITUDE = 180.0

        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_RED_MODE = "red_mode"
        private const val KEY_AUTOMATIC_SENSORS = "automatic_sensors"
        private const val KEY_AUTO_LOCATION = "auto_location"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
//        private const val KEY_CITY_NAME = "city_name"

        fun parseLatitude(value: String?): Double? {
            return parseCoordinate(value, MIN_LATITUDE, MAX_LATITUDE)
        }

        fun parseLongitude(value: String?): Double? {
            return parseCoordinate(value, MIN_LONGITUDE, MAX_LONGITUDE)
        }

        private fun coordinateTextOrDefault(
            value: String?,
            defaultValue: String,
            minValue: Double,
            maxValue: Double
        ): String {
            val normalized = value.orEmpty().trim().replace(',', '.')
            val coordinate = parseCoordinate(normalized, minValue, maxValue)
            return if (coordinate == null) defaultValue else normalized
        }

        private fun parseCoordinate(value: String?, minValue: Double, maxValue: Double): Double? {
            val coordinate = value.orEmpty().trim().replace(',', '.').toDoubleOrNull()
            return coordinate?.takeIf { it in minValue..maxValue }
        }
    }
}

package com.codepalace.accelerometer.ui.activity

import com.codepalace.accelerometer.R
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.local.AppSettingsStorage
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage

class LocationSettingsActivity : AppCompatActivity() {

    private lateinit var settingsStorage: AppSettingsStorage
    private lateinit var switchAutoLocation: SwitchCompat
    private lateinit var etLatitude: EditText
    private lateinit var etLongitude: EditText
    private lateinit var etCityName: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        settingsStorage = AppSettingsStorage(this)
        setContentView(R.layout.activity_location_settings)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        switchAutoLocation = findViewById(R.id.switchAutoLocation)
        etLatitude = findViewById(R.id.etLatitude)
        etLongitude = findViewById(R.id.etLongitude)
        etCityName = findViewById(R.id.etCityName)

        switchAutoLocation.isChecked = settingsStorage.autoLocationEnabled
        etLatitude.setText(settingsStorage.latitude)
        etLongitude.setText(settingsStorage.longitude)
        etCityName.setText(settingsStorage.cityName)

        updateManualFieldsState()

        switchAutoLocation.setOnCheckedChangeListener { _, checked ->
            settingsStorage.autoLocationEnabled = checked
            updateManualFieldsState()
        }

        findViewById<Button>(R.id.btnSaveLocation).setOnClickListener {
            saveLocation()
        }
    }

    private fun updateManualFieldsState() {
        val manualEnabled = !switchAutoLocation.isChecked
        etLatitude.isEnabled = manualEnabled
        etLongitude.isEnabled = manualEnabled
        etCityName.isEnabled = manualEnabled
        etLatitude.alpha = if (manualEnabled) 1f else 0.55f
        etLongitude.alpha = if (manualEnabled) 1f else 0.55f
        etCityName.alpha = if (manualEnabled) 1f else 0.55f
    }

    private fun saveLocation() {
        val autoLocation = switchAutoLocation.isChecked
        val latitude = etLatitude.text.toString().trim()
        val longitude = etLongitude.text.toString().trim()

        if (!autoLocation) {
            val latValue = latitude.toDoubleOrNull()
            val lonValue = longitude.toDoubleOrNull()

            when {
                latValue == null || latValue !in -90.0..90.0 -> {
                    etLatitude.error = "Latitude must be from -90 to 90"
                    return
                }

                lonValue == null || lonValue !in -180.0..180.0 -> {
                    etLongitude.error = "Longitude must be from -180 to 180"
                    return
                }
            }
        }

        settingsStorage.autoLocationEnabled = autoLocation
        settingsStorage.latitude = latitude
        settingsStorage.longitude = longitude
        settingsStorage.cityName = etCityName.text.toString().trim()

        showAppMessage("Location settings saved.", MessageKind.SUCCESS)
    }
}

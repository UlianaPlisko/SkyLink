package com.codepalace.accelerometer.ui.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.codepalace.accelerometer.R
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.api.ApiErrorMapper
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.model.dto.CreateEventRequest
import com.codepalace.accelerometer.data.model.enums.EventType
import com.codepalace.accelerometer.data.repository.EventRepository
import com.codepalace.accelerometer.ui.MessageKind
import com.codepalace.accelerometer.ui.showAppMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
class CreateEventActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var etEventName: EditText
    private lateinit var etDescription: EditText
    private lateinit var spinnerEventType: Spinner
    private lateinit var etDate: EditText
    private lateinit var etStartTime: EditText
    private lateinit var etEndTime: EditText
    private lateinit var etLocation: EditText
    private lateinit var etCapacity: EditText
    private lateinit var switchChatRoom: Switch
    private lateinit var layoutChatRoomName: LinearLayout
    private lateinit var etChatRoomName: EditText
    private lateinit var btnSaveEvent: Button

    private lateinit var eventRepository: EventRepository

    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedStartTime: LocalTime? = null
    private var selectedEndTime: LocalTime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        if (!ApiClient.getSessionStorage().isLoggedIn()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        // Initialize repository
        eventRepository = EventRepository(
            api = ApiClient.eventApi,
            database = AppDatabase.getDatabase(application)
        )

        // Find views
        btnBack = findViewById(R.id.btnBack)
        etEventName = findViewById(R.id.etEventName)
        etDescription = findViewById(R.id.etDescription)
        spinnerEventType = findViewById(R.id.spinnerEventType)
        etDate = findViewById(R.id.etDate)
        etStartTime = findViewById(R.id.etStartTime)
        etEndTime = findViewById(R.id.etEndTime)
        etLocation = findViewById(R.id.etLocation)
        etCapacity = findViewById(R.id.etCapacity)
        switchChatRoom = findViewById(R.id.switchChatRoom)
        layoutChatRoomName = findViewById(R.id.layoutChatRoomName)
        etChatRoomName = findViewById(R.id.etChatRoomName)
        btnSaveEvent = findViewById(R.id.btnSaveEvent)

        // Setup Event Type Spinner
        setupEventTypeSpinner()

        // Back button
        btnBack.setOnClickListener { finish() }

        // Chat room toggle
        switchChatRoom.setOnCheckedChangeListener { _, isChecked ->
            layoutChatRoomName.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Date picker (default = today)
        etDate.setOnClickListener { showDatePicker() }
        updateDateField()

        // Time pickers
        etStartTime.setOnClickListener { showTimePicker(isStartTime = true) }
        etEndTime.setOnClickListener { showTimePicker(isStartTime = false) }

        // Save button
        btnSaveEvent.setOnClickListener { createEvent() }
    }

    private fun setupEventTypeSpinner() {
        val types = EventType.values().map { it.name.replace("_", " ") }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEventType.adapter = adapter
    }

    private fun updateDateField() {
        etDate.setText(selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            selectedDate = LocalDate.of(y, m + 1, d)
            updateDateField()
        }, year, month, day).show()
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, h, m ->
            val time = LocalTime.of(h, m)
            if (isStartTime) {
                selectedStartTime = time
                etStartTime.setText(time.format(DateTimeFormatter.ofPattern("HH:mm")))
            } else {
                selectedEndTime = time
                etEndTime.setText(time.format(DateTimeFormatter.ofPattern("HH:mm")))
            }
        }, hour, minute, true).show()
    }

    private fun createEvent() {
        // Validation
        if (etEventName.text.isBlank()) {
            etEventName.error = "Event name is required"
            return
        }
        if (etDescription.text.isBlank()) {
            etDescription.error = "Description is required"
            return
        }
        if (etDate.text.isBlank() || selectedStartTime == null) {
            showAppMessage("Date and start time are required", MessageKind.ERROR)
            return
        }

        val title = etEventName.text.toString().trim()
        val description = etDescription.text.toString().trim()

        // Get selected EventType
        val selectedTypeName = spinnerEventType.selectedItem.toString().replace(" ", "_")
        val eventType = EventType.valueOf(selectedTypeName)

        // Build Instant strings (with Z)
        val startDateTime = LocalDateTime.of(selectedDate, selectedStartTime!!)
        val localZone = java.time.ZoneId.systemDefault()
        val startAt = startDateTime.atZone(localZone).toInstant().toString()

        val endAt = selectedEndTime?.let {
            val endDateTime = LocalDateTime.of(selectedDate, it)
            endDateTime.atZone(localZone).toInstant().toString()
        }

        val chatRoomName = if (switchChatRoom.isChecked) {
            etChatRoomName.text.toString().trim().ifBlank { null }
        } else null

        val request = CreateEventRequest(
            title = title,
            description = description,
            eventType = eventType,
            startAt = startAt,
            endAt = endAt,
            location = etLocation.text.toString().trim().ifBlank { null },
            maxCapacity = etCapacity.text.toString().trim().toIntOrNull(), // renamed
            chatRoomName = chatRoomName
        )

        lifecycleScope.launch {
            try {
                val createdEvent = eventRepository.createEvent(request)
                showAppMessage("Event created successfully!", MessageKind.INFO)
                delay(1000)
                finish()
            } catch (e: HttpException) {
                showAppMessage(
                    ApiErrorMapper.fromHttpException(e, "Failed to create event"),
                    MessageKind.ERROR
                )
            } catch (e: IOException) {
                showAppMessage("You are offline. Try again later.", MessageKind.INFO)
            } catch (e: Exception) {
                showAppMessage("Something went wrong. Please try again.", MessageKind.ERROR)
            }
        }
    }
}
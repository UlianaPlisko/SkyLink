package com.codepalace.accelerometer.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codepalace.accelerometer.api.ApiClient
import com.codepalace.accelerometer.data.local.AppDatabase
import com.codepalace.accelerometer.data.model.calendar.ScheduledEvent
import com.codepalace.accelerometer.data.model.calendar.WeekDay
import com.codepalace.accelerometer.data.repository.CelestialRepository
import com.codepalace.accelerometer.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@RequiresApi(Build.VERSION_CODES.O)
class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EventRepository(
        api = ApiClient.eventApi,
        database = AppDatabase.getDatabase(application)
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _weekDays = MutableStateFlow<List<WeekDay>>(emptyList())
    val weekDays: StateFlow<List<WeekDay>> = _weekDays

    private val _scheduledEvents = MutableStateFlow<List<ScheduledEvent>>(emptyList())
    val scheduledEvents: StateFlow<List<ScheduledEvent>> = _scheduledEvents

    init {
        updateWeekDays()
        loadEvents()
    }

    suspend fun syncPendingEventActions() {
        repository.syncPendingEventActions()
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        updateWeekDays()
        loadEvents()
    }

    fun previousWeek() {
        _currentDate.value = _currentDate.value.minusWeeks(1)
        updateWeekDays()
    }

    fun nextWeek() {
        _currentDate.value = _currentDate.value.plusWeeks(1)
        updateWeekDays()
    }

    private fun updateWeekDays() {
        val monday = _currentDate.value.with(
            TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)
        )

        _weekDays.value = (0..6).map {
            val date = monday.plusDays(it.toLong())

            WeekDay(
                date = date,
                dayName = date.dayOfWeek.getDisplayName(
                    java.time.format.TextStyle.SHORT,
                    java.util.Locale.getDefault()
                ),
                dayNumber = date.dayOfMonth.toString(),
                isSelected = date == _selectedDate.value
            )
        }
    }

    private fun loadEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dateStr = _selectedDate.value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                Log.d("CalendarViewModel", "Loading events for date: $dateStr")

                val events = repository.refreshEventsByDate(dateStr)
                Log.d("CalendarViewModel", "Loaded ${events.size} events")

                _scheduledEvents.value = events
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Failed to load events", e)
                _scheduledEvents.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun enroll(eventId: Long) {
        repository.enroll(eventId)
        updateEventOptimistically(eventId, isEnrolled = true)   // ← instant UI
    }

    suspend fun signOut(eventId: Long) {
        repository.signOut(eventId)
        updateEventOptimistically(eventId, isEnrolled = false)  // ← instant UI
    }

    private fun updateEventOptimistically(eventId: Long, isEnrolled: Boolean) {
        val currentList = _scheduledEvents.value
        val updatedList = currentList.map { event ->
            if (event.id.toLong() == eventId) {
                val newCount = if (isEnrolled) {
                    event.participantsCount + 1
                } else {
                    (event.participantsCount - 1).coerceAtLeast(0)
                }
                event.copy(
                    isEnrolled = isEnrolled,
                    participantsCount = newCount
                )
            } else {
                event
            }
        }
        _scheduledEvents.value = updatedList
    }
}
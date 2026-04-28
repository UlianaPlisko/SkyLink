package com.codepalace.accelerometer.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.codepalace.accelerometer.data.model.calendar.ScheduledEvent
import com.codepalace.accelerometer.data.model.calendar.WeekDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@RequiresApi(Build.VERSION_CODES.O)
class CalendarViewModel : ViewModel() {

    // Current date being viewed
    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate

    // Selected date (highlighted)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    // Week days (Mon-Sun) for the current week
    private val _weekDays = MutableStateFlow<List<WeekDay>>(emptyList())
    val weekDays: StateFlow<List<WeekDay>> = _weekDays

    // Scheduled events for the selected date
    private val _scheduledEvents = MutableStateFlow<List<ScheduledEvent>>(emptyList())
    val scheduledEvents: StateFlow<List<ScheduledEvent>> = _scheduledEvents

    init {
        // Initialize with today's date and this week's days
        updateWeekDays()
        updateScheduledEvents()
    }

    /**
     * Select a specific date and update the scheduled events for that date
     */
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        updateScheduledEvents()
    }

    /**
     * Navigate to the previous week
     */
    fun previousWeek() {
        val newDate = _currentDate.value.minusWeeks(1)
        _currentDate.value = newDate
        updateWeekDays()
    }

    /**
     * Navigate to the next week
     */
    fun nextWeek() {
        val newDate = _currentDate.value.plusWeeks(1)
        _currentDate.value = newDate
        updateWeekDays()
    }

    /**
     * Update the week days for the current week
     * Week starts on Monday
     */
    private fun updateWeekDays() {
        val mondayOfWeek = _currentDate.value.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))

        val weekDays = mutableListOf<WeekDay>()
        for (i in 0..6) {
            val date = mondayOfWeek.plusDays(i.toLong())
            val dayOfWeek = date.dayOfWeek.getDisplayName(
                java.time.format.TextStyle.SHORT,
                java.util.Locale.getDefault()
            )

            weekDays.add(
                WeekDay(
                    date = date,
                    dayName = dayOfWeek,
                    dayNumber = date.dayOfMonth.toString(),
                    isSelected = date == _selectedDate.value
                )
            )
        }

        _weekDays.value = weekDays
    }

    /**
     * Update scheduled events for the selected date
     * (Placeholder - will be replaced with actual API calls)
     */
    private fun updateScheduledEvents() {
        // For now, return empty list
        // Later, this will fetch events from your backend
        _scheduledEvents.value = emptyList()
    }
}
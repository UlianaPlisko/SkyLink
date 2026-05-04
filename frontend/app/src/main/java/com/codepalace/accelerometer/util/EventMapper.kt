package com.codepalace.accelerometer.util

import android.os.Build
import androidx.annotation.RequiresApi
import com.codepalace.accelerometer.data.model.calendar.ScheduledEvent
import com.codepalace.accelerometer.data.model.dto.EventResponse
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
fun EventResponse.toScheduledEvent(): ScheduledEvent {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val start = startAt.atZone(ZoneId.systemDefault())
    val end = endAt?.atZone(ZoneId.systemDefault())

    return ScheduledEvent(
        id = id.toString(),
        name = title,
        description = description,
        location = eventType,
        startTime = start.format(formatter),
        endTime = end?.format(formatter),
        participantsCount = participantsCount,
        isEnrolled = participant,          // ← now correctly mapped
        maxCapacity = maxCapacity
    )
}
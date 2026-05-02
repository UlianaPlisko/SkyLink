package com.codepalace.accelerometer.util

import android.os.Build
import androidx.annotation.RequiresApi
import com.codepalace.accelerometer.data.local.EventEntity
import com.codepalace.accelerometer.data.model.calendar.ScheduledEvent
import com.codepalace.accelerometer.data.model.dto.EventResponse
import java.time.Instant

fun EventResponse.toEntity() = EventEntity(
    id = id,
    title = title,
    description = description,
    eventType = eventType,
    startAt = startAt.toString(),
    endAt = endAt?.toString(),
    creatorId = creatorId,
    participantsCount = participantsCount,
    isParticipant = participant
)

@RequiresApi(Build.VERSION_CODES.O)
fun EventEntity.toDomain(): ScheduledEvent {
    val eventResponse = EventResponse(
        id = id,
        title = title,
        description = description,
        eventType = eventType,
        startAt = Instant.parse(startAt),
        endAt = endAt?.let { Instant.parse(it) },
        creatorId = creatorId,
        participantsCount = participantsCount,
        participant = isParticipant   // ✅ IMPORTANT
    )

    return eventResponse.toScheduledEvent(isParticipant)
}
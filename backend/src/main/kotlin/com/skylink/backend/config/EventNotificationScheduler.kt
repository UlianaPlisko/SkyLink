package com.skylink.backend.config

import com.skylink.backend.repository.EventParticipantRepository
import com.skylink.backend.repository.EventRepository
import com.skylink.backend.repository.UserRepository
import com.skylink.backend.service.FcmNotificationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset

@Component
class EventNotificationScheduler(
    private val eventRepository: EventRepository,
    private val eventParticipantRepository: EventParticipantRepository,
    private val userRepository: UserRepository,
    private val fcmNotificationService: FcmNotificationService
) {

    private val log = LoggerFactory.getLogger(EventNotificationScheduler::class.java)

    @Scheduled(fixedRate = 60_000)
    fun sendReminders() {
        val now = Instant.now()
        val offsets = listOf(5L, 30L, 720L, 1440L)

        log.info("=== Scheduler tick at $now ===")

        for (minutesBefore in offsets) {
            val windowStart = now.plusSeconds((minutesBefore - 1) * 60)
            val windowEnd   = now.plusSeconds(minutesBefore * 60)

            log.info("[$minutesBefore min] Checking window: $windowStart → $windowEnd")

            val events = eventRepository.findByStartAtBetween(windowStart, windowEnd)
            log.info("[$minutesBefore min] Found ${events.size} event(s)")

            for (event in events) {
                val eventId = event.id ?: run {
                    log.warn("Event has null id, skipping")
                    continue
                }

                log.info("[$minutesBefore min] Processing event $eventId '${event.title}' startAt=${event.startAt}")

                val participants = eventParticipantRepository.findByIdEventId(eventId)
                log.info("[$minutesBefore min] Event $eventId has ${participants.size} participant(s)")

                if (participants.isEmpty()) {
                    log.info("[$minutesBefore min] No participants for event $eventId, skipping")
                    continue
                }

                val eventDate = event.startAt
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                    .toString()

                for (participant in participants) {
                    val userId = participant.id.userId
                    log.info("[$minutesBefore min] Looking up user $userId")

                    val user = userRepository.findById(userId).orElse(null)
                    if (user == null) {
                        log.warn("[$minutesBefore min] User $userId not found in DB, skipping")
                        continue
                    }

                    log.info("[$minutesBefore min] User ${user.id} (${user.email}) fcmToken=${
                        if (user.fcmToken.isNullOrBlank()) "NULL/EMPTY" else "present (${user.fcmToken!!.take(20)}...)"
                    }")

                    val token = user.fcmToken
                    if (token.isNullOrBlank()) {
                        log.warn("[$minutesBefore min] No FCM token for user ${user.id}, skipping")
                        continue
                    }

                    try {
                        fcmNotificationService.sendEventReminder(
                            fcmToken = token,
                            eventTitle = event.title,
                            eventId = eventId,
                            eventDate = eventDate,
                            minutesBefore = minutesBefore
                        )
                        log.info("[$minutesBefore min] ✅ Notification sent to user ${user.id} for event $eventId")
                    } catch (e: Exception) {
                        log.error("[$minutesBefore min] ❌ Failed to notify user ${user.id} for event $eventId: ${e.message}", e)
                    }
                }
            }
        }

        log.info("=== Scheduler tick done ===")
    }
}
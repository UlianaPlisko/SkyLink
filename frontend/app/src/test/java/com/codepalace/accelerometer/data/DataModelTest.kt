package com.codepalace.accelerometer.data

import com.codepalace.accelerometer.data.model.SpaceObjectDetail
import com.codepalace.accelerometer.data.model.SpaceObjectSummary
import com.codepalace.accelerometer.data.model.calendar.ScheduledEvent
import com.codepalace.accelerometer.data.model.calendar.WeekDay
import com.codepalace.accelerometer.data.model.dto.ChatListItem
import com.codepalace.accelerometer.data.model.dto.MessageUi
import com.codepalace.accelerometer.data.model.enums.ChatRoomType
import com.codepalace.accelerometer.data.model.enums.EventType
import com.codepalace.accelerometer.data.model.enums.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DataModelTest {

    @Test
    fun `scheduled event capacity display handles limited and unlimited events`() {
        val limited = ScheduledEvent("1", "Meteor", null, "OTHER", "20:00", null, true, 3, 5)
        val unlimited = limited.copy(maxCapacity = null)

        assertEquals("3/5", limited.capacityDisplay)
        assertNull(unlimited.capacityDisplay)
    }

    @Test
    fun `data classes expose copy equality and defaults`() {
        val weekDay = WeekDay(LocalDate.of(2026, 5, 23), "Sat", "23")
        val summary = SpaceObjectSummary(1L, "Vega", 0.03, "STAR", 279.2, 38.8)
        val detail = SpaceObjectDetail(1L, "Mars", null, 1.0, 2.0, null, null, null)
        val message = MessageUi(7L, "hello", "08:15", true, createdAt = "2026-05-23T08:15:00Z")
        val item = ChatListItem.Message(message)
        val header = ChatListItem.DateHeader("Today")

        assertEquals(false, weekDay.isSelected)
        assertEquals("bright", summary.copy(description = "bright").description)
        assertNull(detail.imageUrl)
        assertNull(message.senderDisplayName)
        assertEquals(message, item.message)
        assertEquals("Today", header.formattedDate)
    }

    @Test
    fun `enum values remain stable`() {
        assertEquals(listOf("REGIONAL", "EVENT", "GLOBAL"), ChatRoomType.entries.map { it.name })
        assertEquals("METEOR_SHOWER", EventType.METEOR_SHOWER.name)
        assertEquals("ADMIN", UserRole.ADMIN.name)
    }
}

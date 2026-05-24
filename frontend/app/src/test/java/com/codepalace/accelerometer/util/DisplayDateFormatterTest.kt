package com.codepalace.accelerometer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DisplayDateFormatterTest {

    @Test
    fun `format added on trims blanks parses iso dates and falls back cleanly`() {
        assertNull(DisplayDateFormatter.formatAddedOn(null))
        assertNull(DisplayDateFormatter.formatAddedOn("   "))

        val formatted = DisplayDateFormatter.formatAddedOn("2026-05-23T08:15:12.123456Z")
        assertEquals(true, formatted?.startsWith("Added on 23 May at "))

        assertEquals(
            "Added on not-a-date",
            DisplayDateFormatter.formatAddedOn("not-a-date")
        )
    }

    @Test
    fun `format event start parses supported shapes and falls back`() {
        assertNull(DisplayDateFormatter.formatEventStart(""))
        assertEquals(true, DisplayDateFormatter.formatEventStart("2026-05-23T08:15:12Z")?.contains("23 May at"))
        assertEquals("not-a-date", DisplayDateFormatter.formatEventStart("not-a-date"))
    }

    @Test
    fun `format enum label handles underscores hyphens spaces and blanks`() {
        assertEquals("Meteor Shower", DisplayDateFormatter.formatEnumLabel("METEOR_SHOWER"))
        assertEquals("Planet Alignment", DisplayDateFormatter.formatEnumLabel("planet-alignment"))
        assertEquals("Deep Sky Object", DisplayDateFormatter.formatEnumLabel(" deep   sky_object "))
        assertEquals("", DisplayDateFormatter.formatEnumLabel("___"))
    }
}

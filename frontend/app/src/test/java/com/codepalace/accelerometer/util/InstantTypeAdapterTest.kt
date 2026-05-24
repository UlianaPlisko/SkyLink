package com.codepalace.accelerometer.util

import com.google.gson.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class InstantTypeAdapterTest {

    private val adapter = InstantTypeAdapter()

    @Test
    fun `serialize writes iso instant and rejects null source`() {
        val instant = Instant.parse("2026-05-23T08:15:00Z")

        assertEquals("2026-05-23T08:15:00Z", adapter.serialize(instant, null, null).asString)
        assertThrows(NullPointerException::class.java) {
            adapter.serialize(null, null, null)
        }
    }

    @Test
    fun `deserialize parses instant and keeps null null`() {
        assertEquals(
            Instant.parse("2026-05-23T08:15:00Z"),
            adapter.deserialize(JsonPrimitive("2026-05-23T08:15:00Z"), null, null)
        )
        assertNull(adapter.deserialize(null, null, null))
    }
}

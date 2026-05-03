package com.codepalace.accelerometer.util

import java.text.SimpleDateFormat
import java.util.Locale

object DisplayDateFormatter {
    fun formatAddedOn(rawDate: String?): String? {
        val value = rawDate?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return "Added on ${formatInstantText(value) ?: cleanFallback(value)}"
    }

    fun formatEventStart(rawDate: String?): String? {
        val value = rawDate?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return formatInstantText(value) ?: cleanFallback(value)
    }

    fun formatEnumLabel(rawValue: String): String {
        return rawValue
            .split('_', '-', ' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase(Locale.US)
                    .replaceFirstChar { it.titlecase(Locale.US) }
            }
    }

    private fun formatInstantText(value: String): String? {
        val normalized = normalizeFraction(value)
        val parsers = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )

        val date = parsers.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                }.parse(normalized)
            }.getOrNull()
        } ?: return null

        return SimpleDateFormat("d MMMM 'at' HH:mm", Locale.ENGLISH).format(date)
    }

    private fun normalizeFraction(value: String): String {
        val dotIndex = value.indexOf('.')
        if (dotIndex == -1) return value

        val tail = value.substring(dotIndex + 1)
        val zoneIndex = tail.indexOfFirst { it == 'Z' || it == '+' || it == '-' }
        val fraction = if (zoneIndex == -1) tail else tail.substring(0, zoneIndex)
        val zone = if (zoneIndex == -1) "" else tail.substring(zoneIndex)
        val millis = fraction.take(3).padEnd(3, '0')

        return value.substring(0, dotIndex + 1) + millis + zone
    }

    private fun cleanFallback(value: String): String {
        return value
            .substringBefore('.')
            .removeSuffix("Z")
            .replace('T', ' ')
    }
}

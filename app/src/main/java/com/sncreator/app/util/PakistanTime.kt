package com.sncreator.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object PakistanTime {
    val KARACHI_ZONE: ZoneId = ZoneId.of("Asia/Karachi")
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val DISPLAY_RESET_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, hh:mm a 'PKT'", Locale.US)

    /**
     * Returns the current calendar date string (YYYY-MM-DD) in Pakistan Standard Time (Asia/Karachi).
     */
    fun currentPktDateString(now: Instant = Instant.now()): String {
        return ZonedDateTime.ofInstant(now, KARACHI_ZONE).toLocalDate().format(DATE_FORMATTER)
    }

    /**
     * Returns the exact Instant of the next midnight (00:00:00) in Asia/Karachi.
     */
    fun nextMidnightPkt(now: Instant = Instant.now()): ZonedDateTime {
        val currentPkt = ZonedDateTime.ofInstant(now, KARACHI_ZONE)
        val tomorrow: LocalDate = currentPkt.toLocalDate().plusDays(1)
        return tomorrow.atStartOfDay(KARACHI_ZONE)
    }

    /**
     * Formats an ISO-8601 reset timestamp (or calculates next PKT midnight fallback) for user display.
     */
    fun formatResetDisplay(resetsAtIso: String?, now: Instant = Instant.now()): String {
        if (!resetsAtIso.isNullOrBlank()) {
            runCatching {
                val instant = Instant.parse(resetsAtIso)
                return ZonedDateTime.ofInstant(instant, KARACHI_ZONE).format(DISPLAY_RESET_FORMATTER)
            }
        }
        return nextMidnightPkt(now).format(DISPLAY_RESET_FORMATTER)
    }
}

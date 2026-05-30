package com.lexease.notifications;

import com.lexease.shared.api.ApiException;
import com.lexease.shared.api.ErrorCode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ReminderNextRunCalculator {
    public Instant nextRunAt(List<DayOfWeek> daysOfWeek, LocalTime localTime, String timezone, Instant now) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REMINDER, "Reminder days of week cannot be empty");
        }
        ZoneId zoneId = parseZone(timezone);
        ZonedDateTime zonedNow = now.atZone(zoneId);
        LocalDate today = zonedNow.toLocalDate();
        return daysOfWeek.stream()
                .distinct()
                .map(day -> candidate(today, day, localTime, zoneId, zonedNow))
                .min(Comparator.naturalOrder())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REMINDER, "Reminder days of week cannot be empty"))
                .toInstant();
    }

    ZoneId parseZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (ZoneRulesException | NullPointerException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REMINDER, "Invalid reminder timezone");
        }
    }

    private ZonedDateTime candidate(LocalDate today, DayOfWeek day, LocalTime localTime, ZoneId zoneId, ZonedDateTime zonedNow) {
        int daysUntil = Math.floorMod(day.getValue() - today.getDayOfWeek().getValue(), 7);
        ZonedDateTime candidate = today.plusDays(daysUntil).atTime(localTime).atZone(zoneId);
        if (!candidate.isAfter(zonedNow)) {
            candidate = candidate.plusWeeks(1);
        }
        return candidate;
    }
}

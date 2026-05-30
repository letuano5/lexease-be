package com.lexease.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReminderNextRunCalculatorTests {
    private final ReminderNextRunCalculator calculator = new ReminderNextRunCalculator();

    @Test
    void returnsTodayWhenLocalTimeStillAhead() {
        Instant now = Instant.parse("2026-05-25T11:00:00Z");

        Instant nextRunAt = calculator.nextRunAt(
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(19, 30),
                "Asia/Ho_Chi_Minh",
                now);

        assertThat(nextRunAt).isEqualTo(Instant.parse("2026-05-25T12:30:00Z"));
    }

    @Test
    void skipsToNextWeekWhenLocalTimeAlreadyPassedToday() {
        Instant now = Instant.parse("2026-05-25T13:00:00Z");

        Instant nextRunAt = calculator.nextRunAt(
                List.of(DayOfWeek.MONDAY),
                LocalTime.of(19, 30),
                "Asia/Ho_Chi_Minh",
                now);

        assertThat(nextRunAt).isEqualTo(Instant.parse("2026-06-01T12:30:00Z"));
    }

    @Test
    void selectsEarliestAcrossMultipleDays() {
        Instant now = Instant.parse("2026-05-25T13:00:00Z");

        Instant nextRunAt = calculator.nextRunAt(
                List.of(DayOfWeek.FRIDAY, DayOfWeek.WEDNESDAY),
                LocalTime.of(19, 30),
                "Asia/Ho_Chi_Minh",
                now);

        assertThat(nextRunAt).isEqualTo(Instant.parse("2026-05-27T12:30:00Z"));
    }
}

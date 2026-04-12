package com.example.studysync_project.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class StudyTimeWindowTest {

    @Test
    public void dayBoundaries_matchExpectedMidnightRange() {
        ZoneId zone = ZoneId.of("UTC");
        long noon = ZonedDateTime.of(2026, 4, 12, 12, 0, 0, 0, zone).toInstant().toEpochMilli();

        long start = StudyTimeWindow.startOfDayMillis(noon, zone);
        long end = StudyTimeWindow.endOfDayMillis(noon, zone);

        long expectedStart = ZonedDateTime.of(2026, 4, 12, 0, 0, 0, 0, zone).toInstant().toEpochMilli();
        long expectedEnd = ZonedDateTime.of(2026, 4, 13, 0, 0, 0, 0, zone).toInstant().toEpochMilli();

        assertEquals(expectedStart, start);
        assertEquals(expectedEnd, end);
        assertEquals(24L * 60L * 60L * 1000L, end - start);
    }

    @Test
    public void dayBoundaries_areInclusiveExclusive() {
        ZoneId zone = ZoneId.of("UTC");
        long now = ZonedDateTime.of(2026, 4, 12, 8, 30, 0, 0, zone).toInstant().toEpochMilli();
        long start = StudyTimeWindow.startOfDayMillis(now, zone);
        long end = StudyTimeWindow.endOfDayMillis(now, zone);

        assertTrue(start <= now);
        assertTrue(now < end);
    }
}

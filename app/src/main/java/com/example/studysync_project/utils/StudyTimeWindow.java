package com.example.studysync_project.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class StudyTimeWindow {

    private StudyTimeWindow() {
    }

    public static long startOfDayMillis(long nowMillis) {
        return startOfDayMillis(nowMillis, ZoneId.systemDefault());
    }

    public static long endOfDayMillis(long nowMillis) {
        return endOfDayMillis(nowMillis, ZoneId.systemDefault());
    }

    public static long startOfDayMillis(long nowMillis, ZoneId zoneId) {
        LocalDate day = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate();
        return day.atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

    public static long endOfDayMillis(long nowMillis, ZoneId zoneId) {
        LocalDate day = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate();
        return day.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli();
    }
}

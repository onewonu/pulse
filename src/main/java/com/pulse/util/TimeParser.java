package com.pulse.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class TimeParser {

    private static final DateTimeFormatter HH_MM_SS_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TimeParser() {}

    public static LocalTime parseHHmmss(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }

        try {
            return LocalTime.parse(timeStr, HH_MM_SS_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static LocalTime parseHHmmssWithNormalization(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }

        try {
            String normalizedTime = SubwayTimeNormalizer.normalize(timeStr);
            return LocalTime.parse(normalizedTime, HH_MM_SS_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

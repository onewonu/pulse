package com.pulse.util;

public final class SubwayTimeNormalizer {

    private SubwayTimeNormalizer() {}

    public static String normalize(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return timeString;
        }

        if (
                timeString.startsWith("24:") ||
                timeString.startsWith("25:") ||
                timeString.startsWith("26:") ||
                timeString.startsWith("27:")
        ) {
            int hour = Integer.parseInt(timeString.substring(0, 2));
            if (hour >= 24) {
                int normalizedHour = hour - 24;
                return String.format("%02d:%s", normalizedHour, timeString.substring(3));
            }
        }

        return timeString;
    }
}

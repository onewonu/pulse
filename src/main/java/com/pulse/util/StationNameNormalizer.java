package com.pulse.util;

public final class StationNameNormalizer {

    private StationNameNormalizer() {}

    public static String normalize(String stationName) {
        if (stationName == null || stationName.isBlank()) {
            return stationName;
        }

        String trimmed = stationName.trim();
        int parenthesisIndex = trimmed.indexOf('(');

        if (parenthesisIndex > 0) {
            return trimmed.substring(0, parenthesisIndex);
        }

        return trimmed;
    }
}

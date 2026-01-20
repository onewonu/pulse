package com.pulse.util;

import java.util.Set;

public final class LineDirectionResolver {

    private static final String[] STRAIGHT_LINE_DIRECTIONS = {"상행", "하행"};
    private static final String[] CIRCULAR_LINE_DIRECTIONS = {"내선", "외선"};

    private static final Set<String> CIRCULAR_LINES = Set.of("2호선", "6호선");

    private LineDirectionResolver() {}

    public static String[] getValidDirections(String lineName) {
        if (lineName == null || lineName.isBlank()) {
            return STRAIGHT_LINE_DIRECTIONS;
        }

        String trimmed = lineName.trim();

        if (isCircularLine(trimmed)) {
            return CIRCULAR_LINE_DIRECTIONS;
        }

        return STRAIGHT_LINE_DIRECTIONS;
    }

    public static boolean isCircularLine(String lineName) {
        if (lineName == null || lineName.isBlank()) {
            return false;
        }

        String trimmed = lineName.trim();
        return CIRCULAR_LINES.contains(trimmed);
    }
}

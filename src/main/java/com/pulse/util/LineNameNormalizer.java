package com.pulse.util;

import java.util.Map;

public final class LineNameNormalizer {

    private static final String LINE_GYEONGUI_JUNGANG = "경의중앙선";
    private static final String LINE_SUIN_BUNDANG = "수인분당선";
    private static final String LINE_4 = "4호선";
    private static final String LINE_3 = "3호선";
    private static final String LINE_9 = "9호선";

    private static final Map<String, String> NORMALIZATION_RULES = Map.ofEntries(
            Map.entry("경원선", LINE_GYEONGUI_JUNGANG),
            Map.entry("중앙선", LINE_GYEONGUI_JUNGANG),
            Map.entry("경의선", LINE_GYEONGUI_JUNGANG),
            Map.entry("장항선", LINE_GYEONGUI_JUNGANG),
            Map.entry("분당선", LINE_SUIN_BUNDANG),
            Map.entry("수인선", LINE_SUIN_BUNDANG),
            Map.entry("안산선", LINE_4),
            Map.entry("과천선", LINE_4),
            Map.entry("일산선", LINE_3),
            Map.entry("9호선2~3단계", LINE_9),
            Map.entry("9호선2단계", LINE_9),
            Map.entry("9호선3단계", LINE_9)
    );

    private LineNameNormalizer() {}
    
    public static String normalize(String rawLineName) {
        if (rawLineName == null || rawLineName.isBlank()) {
            return rawLineName;
        }

        String trimmed = rawLineName.trim();
        return NORMALIZATION_RULES.getOrDefault(trimmed, trimmed);
    }
}

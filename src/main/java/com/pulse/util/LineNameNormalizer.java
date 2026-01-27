package com.pulse.util;

import java.util.Map;

public final class LineNameNormalizer {

    private static final String LINE_GYEONGUI_JUNGANG = "경의중앙선";
    private static final String LINE_SUIN_BUNDANG = "수도권 수인.분당선";
    private static final String LINE_1 = "수도권 1호선";
    private static final String LINE_2 = "수도권 2호선";
    private static final String LINE_3 = "수도권 3호선";
    private static final String LINE_4 = "수도권 4호선";
    private static final String LINE_5 = "수도권 5호선";
    private static final String LINE_6 = "수도권 6호선";
    private static final String LINE_7 = "수도권 7호선";
    private static final String LINE_8 = "수도권 8호선";
    private static final String LINE_9 = "수도권 9호선";
    private static final String LINE_GYEONGCHUN = "수도권 경춘선";
    private static final String LINE_GYEONGGANG = "수도권 경강선";
    private static final String LINE_AIRPORT = "수도권 공항철도";
    private static final String LINE_GIMPO_GOLD = "수도권 김포골드라인";
    private static final String LINE_SEOHAE = "수도권 서해선";
    private static final String LINE_SILLIM = "수도권 신림선";
    private static final String LINE_SINBUNDANG = "수도권 신분당선";
    private static final String LINE_EVERLINE = "수도권 에버라인";
    private static final String LINE_UI_SINSEOL = "수도권 우이신설경전철";
    private static final String LINE_UIJEONGBU = "수도권 의정부경전철";
    private static final String LINE_GTX_A = "수도권 GTX-A";
    private static final String LINE_INCHEON_1 = "인천 1호선";
    private static final String LINE_INCHEON_2 = "인천 2호선";

    private static final Map<String, String> NORMALIZATION_RULES = Map.ofEntries(
            Map.entry("1호선", LINE_1),
            Map.entry("2호선", LINE_2),
            Map.entry("3호선", LINE_3),
            Map.entry("4호선", LINE_4),
            Map.entry("5호선", LINE_5),
            Map.entry("6호선", LINE_6),
            Map.entry("7호선", LINE_7),
            Map.entry("8호선", LINE_8),
            Map.entry("9호선", LINE_9),
            Map.entry("경원선", LINE_GYEONGUI_JUNGANG),
            Map.entry("경부선", LINE_1),
            Map.entry("경인선", LINE_1),
            Map.entry("장항선", LINE_1),
            Map.entry("중앙선", LINE_GYEONGUI_JUNGANG),
            Map.entry("경의선", LINE_GYEONGUI_JUNGANG),
            Map.entry("분당선", LINE_SUIN_BUNDANG),
            Map.entry("수인선", LINE_SUIN_BUNDANG),
            Map.entry("수인.분당선", LINE_SUIN_BUNDANG),
            Map.entry("안산선", LINE_4),
            Map.entry("과천선", LINE_4),
            Map.entry("일산선", LINE_3),
            Map.entry("9호선2~3단계", LINE_9),
            Map.entry("9호선2단계", LINE_9),
            Map.entry("9호선3단계", LINE_9),
            Map.entry("경춘선", LINE_GYEONGCHUN),
            Map.entry("경강선", LINE_GYEONGGANG),
            Map.entry("공항철도", LINE_AIRPORT),
            Map.entry("김포골드라인", LINE_GIMPO_GOLD),
            Map.entry("서해선", LINE_SEOHAE),
            Map.entry("신림선", LINE_SILLIM),
            Map.entry("신분당선", LINE_SINBUNDANG),
            Map.entry("에버라인", LINE_EVERLINE),
            Map.entry("우이신설경전철", LINE_UI_SINSEOL),
            Map.entry("우이신설선", LINE_UI_SINSEOL),
            Map.entry("의정부경전철", LINE_UIJEONGBU),
            Map.entry("의정부선", LINE_UIJEONGBU),
            Map.entry("GTX-A", LINE_GTX_A),
            Map.entry("인천1호선", LINE_INCHEON_1),
            Map.entry("인천2호선", LINE_INCHEON_2)
    );

    private LineNameNormalizer() {}

    public static String normalize(String rawLineName) {
        if (rawLineName == null || rawLineName.isBlank()) {
            return rawLineName;
        }

        String trimmed = rawLineName.trim();
        return NORMALIZATION_RULES.getOrDefault(trimmed, trimmed);
    }

    public static String denormalize(String normalizedLineName) {
        if (normalizedLineName == null || normalizedLineName.isBlank()) {
            return normalizedLineName;
        }

        String trimmed = normalizedLineName.trim();

        if (trimmed.equals(LINE_GYEONGUI_JUNGANG)) {
            return LINE_GYEONGUI_JUNGANG;
        }

        if (trimmed.startsWith("수도권 ")) {
            return trimmed.substring(4);
        }

        if (trimmed.startsWith("인천 ")) {
            return trimmed.substring(3);
        }

        return trimmed;
    }
}

package com.pulse.dto;

import java.time.LocalDateTime;

public record DataLoadResult(
    boolean success,
    String dataCategory,
    int totalCount,
    String message,
    LocalDateTime loadedAt
) {
    public static DataLoadResult success(String dataCategory, int count) {
        return new DataLoadResult(
                true,
                dataCategory,
                count,
                count + " Loading completed",
                LocalDateTime.now()
        );
    }

    public static DataLoadResult failure(String dataCategory, String errorMessage) {
        return new DataLoadResult(false, dataCategory, 0, errorMessage, LocalDateTime.now());
    }
}

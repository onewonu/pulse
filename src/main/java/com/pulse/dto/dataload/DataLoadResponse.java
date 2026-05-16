package com.pulse.dto.dataload;

import java.time.LocalDateTime;

public record DataLoadResponse(
    boolean success,
    String dataCategory,
    int totalCount,
    String message,
    LocalDateTime loadedAt
) {
    public static DataLoadResponse success(String dataCategory, int count) {
        return new DataLoadResponse(
                true,
                dataCategory,
                count,
                count + " Loading completed",
                LocalDateTime.now()
        );
    }

    public static DataLoadResponse failure(String dataCategory, String errorMessage) {
        return new DataLoadResponse(false, dataCategory, 0, errorMessage, LocalDateTime.now());
    }
}

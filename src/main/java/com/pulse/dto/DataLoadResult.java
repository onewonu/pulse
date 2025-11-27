package com.pulse.dto;

import java.time.LocalDateTime;

public class DataLoadResult {

    private final boolean success;
    private final String dataCategory;
    private final int totalCount;
    private final String message;
    private final LocalDateTime loadedAt;

    private DataLoadResult(
            boolean success,
            String dataCategory,
            int totalCount,
            String message,
            LocalDateTime loadedAt
    ) {
        this.success = success;
        this.dataCategory = dataCategory;
        this.totalCount = totalCount;
        this.message = message;
        this.loadedAt = loadedAt;
    }

    public static DataLoadResult success(String dataCategory, int count) {
        return new DataLoadResult(
                true,
                dataCategory,
                count,
                count + "Loading completed",
                LocalDateTime.now()
        );
    }

    public static DataLoadResult failure(String dataCategory, String errorMessage) {
        return new DataLoadResult(false, dataCategory, 0, errorMessage, LocalDateTime.now());
    }

    public boolean isSuccess() {
        return success;
    }

    public String getDataCategory() {
        return dataCategory;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getLoadedAt() {
        return loadedAt;
    }
}

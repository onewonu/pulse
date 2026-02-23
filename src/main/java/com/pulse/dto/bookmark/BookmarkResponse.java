package com.pulse.dto.bookmark;

import com.pulse.entity.bookmark.Bookmark;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record BookmarkResponse(
    Long id,
    String name,
    Integer departureStationId,
    Integer arrivalStationId,
    String departureStationName,
    String arrivalStationName,
    String departureLineName,
    String departureLineColor,
    String arrivalLineName,
    String arrivalLineColor,
    LocalTime startTime,
    LocalTime endTime,
    Integer displayOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static BookmarkResponse of(
        Bookmark bookmark,
        String departureStationName,
        String arrivalStationName,
        String departureLineName,
        String departureLineColor,
        String arrivalLineName,
        String arrivalLineColor
    ) {
        return new BookmarkResponse(
            bookmark.getId(),
            bookmark.getName(),
            bookmark.getDepartureStationId(),
            bookmark.getArrivalStationId(),
            departureStationName,
            arrivalStationName,
            departureLineName,
            departureLineColor,
            arrivalLineName,
            arrivalLineColor,
            bookmark.getStartTime(),
            bookmark.getEndTime(),
            bookmark.getDisplayOrder(),
            bookmark.getCreatedAt(),
            bookmark.getUpdatedAt()
        );
    }
}

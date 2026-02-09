package com.pulse.dto.bookmark;

import com.pulse.entity.bookmark.Bookmark;

import java.time.LocalDateTime;

public record BookmarkResponse(
    Long id,
    String name,
    Integer departureStationId,
    Integer arrivalStationId,
    Integer displayOrder,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static BookmarkResponse of(Bookmark bookmark) {
        return new BookmarkResponse(
            bookmark.getId(),
            bookmark.getName(),
            bookmark.getDepartureStationId(),
            bookmark.getArrivalStationId(),
            bookmark.getDisplayOrder(),
            bookmark.getCreatedAt(),
            bookmark.getUpdatedAt()
        );
    }
}

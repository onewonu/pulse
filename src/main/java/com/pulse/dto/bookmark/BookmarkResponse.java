package com.pulse.dto.bookmark;

import com.pulse.entity.bookmark.Bookmark;

import java.time.LocalDateTime;

public class BookmarkResponse {

    private final Long id;
    private final String name;
    private final Integer departureStationId;
    private final Integer arrivalStationId;
    private final Integer displayOrder;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private BookmarkResponse(
            Long id,
            String name,
            Integer departureStationId,
            Integer arrivalStationId,
            Integer displayOrder,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.departureStationId = departureStationId;
        this.arrivalStationId = arrivalStationId;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getDepartureStationId() {
        return departureStationId;
    }

    public Integer getArrivalStationId() {
        return arrivalStationId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

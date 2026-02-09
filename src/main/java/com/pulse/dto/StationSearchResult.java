package com.pulse.dto;

import java.util.List;

public record StationSearchResult(
    int totalCount,
    List<StationItem> stations
) {
    public record StationItem(
        String stationName,
        String stationID,
        Double x,
        Double y,
        String laneName,
        String lineColor
    ) {}
}

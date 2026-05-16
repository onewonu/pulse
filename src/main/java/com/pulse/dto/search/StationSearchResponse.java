package com.pulse.dto.search;

import java.util.List;

public record StationSearchResponse(
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

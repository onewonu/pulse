package com.pulse.dto.masterdata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record StationExportData(
    @JsonProperty("exportedAt")
    String exportedAt,

    @JsonProperty("totalStations")
    Integer totalStations,

    @JsonProperty("totalResults")
    Integer totalResults,

    @JsonProperty("stationSearchResults")
    List<StationSearchResult> stationSearchResults
) {}

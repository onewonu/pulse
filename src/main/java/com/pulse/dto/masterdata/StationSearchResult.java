package com.pulse.dto.masterdata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record StationSearchResult(
    @JsonProperty("searchedStationName")
    String searchedStationName,

    @JsonProperty("totalCount")
    Integer totalCount,

    @JsonProperty("results")
    List<StationMasterData> results
) {}

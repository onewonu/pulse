package com.pulse.dto.masterdata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class StationExportData {

    @JsonProperty("exportedAt")
    private String exportedAt;

    @JsonProperty("totalStations")
    private Integer totalStations;

    @JsonProperty("totalResults")
    private Integer totalResults;

    @JsonProperty("stationSearchResults")
    private List<StationSearchResult> stationSearchResults;

    public String getExportedAt() {
        return exportedAt;
    }

    public Integer getTotalStations() {
        return totalStations;
    }

    public Integer getTotalResults() {
        return totalResults;
    }

    public List<StationSearchResult> getStationSearchResults() {
        return stationSearchResults;
    }

    public void setExportedAt(String exportedAt) {
        this.exportedAt = exportedAt;
    }

    public void setTotalStations(Integer totalStations) {
        this.totalStations = totalStations;
    }

    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }

    public void setStationSearchResults(List<StationSearchResult> stationSearchResults) {
        this.stationSearchResults = stationSearchResults;
    }
}

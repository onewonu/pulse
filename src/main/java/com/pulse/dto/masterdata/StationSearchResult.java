package com.pulse.dto.masterdata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class StationSearchResult {

    @JsonProperty("searchedStationName")
    private String searchedStationName;

    @JsonProperty("totalCount")
    private Integer totalCount;

    @JsonProperty("results")
    private List<StationMasterData> results;

    public String getSearchedStationName() {
        return searchedStationName;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public List<StationMasterData> getResults() {
        return results;
    }

    public void setSearchedStationName(String searchedStationName) {
        this.searchedStationName = searchedStationName;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public void setResults(List<StationMasterData> results) {
        this.results = results;
    }
}

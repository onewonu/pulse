package com.pulse.dto.masterdata;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StationMasterData(
    @JsonProperty("stationName")
    String stationName,

    @JsonProperty("stationID")
    String stationID,

    @JsonProperty("x")
    String x,

    @JsonProperty("y")
    String y,

    @JsonProperty("laneName")
    String laneName
) {
    public Double getLongitude() {
        try {
            return x != null ? Double.parseDouble(x) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double getLatitude() {
        try {
            return y != null ? Double.parseDouble(y) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

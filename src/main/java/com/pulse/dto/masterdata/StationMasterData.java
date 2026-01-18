package com.pulse.dto.masterdata;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StationMasterData {

    @JsonProperty("stationName")
    private String stationName;

    @JsonProperty("stationID")
    private String stationID;

    @JsonProperty("x")
    private String x;

    @JsonProperty("y")
    private String y;

    @JsonProperty("laneName")
    private String laneName;


    public String getStationName() {
        return stationName;
    }

    public String getStationID() {
        return stationID;
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    public String getLaneName() {
        return laneName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

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

    public void setStationID(String stationID) {
        this.stationID = stationID;
    }

    public void setX(String x) {
        this.x = x;
    }

    public void setY(String y) {
        this.y = y;
    }

    public void setLaneName(String laneName) {
        this.laneName = laneName;
    }
}

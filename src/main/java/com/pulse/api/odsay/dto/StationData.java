package com.pulse.api.odsay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StationData {

    @JsonProperty("stationClass")
    private Integer stationClass;

    @JsonProperty("stationName")
    private String stationName;

    @JsonProperty("stationID")
    private String stationID;

    @JsonProperty("localStationID")
    private String localStationID;

    @JsonProperty("arsID")
    private String arsID;

    @JsonProperty("ebid")
    private String ebid;

    @JsonProperty("x")
    private Double x;

    @JsonProperty("y")
    private Double y;

    @JsonProperty("type")
    private Integer type;

    @JsonProperty("laneName")
    private String laneName;

    @JsonProperty("CID")
    private Integer cid;

    @JsonProperty("cityName")
    private String cityName;

    public Integer getStationClass() {
        return stationClass;
    }

    public String getStationName() {
        return stationName;
    }

    public String getStationID() {
        return stationID;
    }

    public String getLocalStationID() {
        return localStationID;
    }

    public String getArsID() {
        return arsID;
    }

    public String getEbid() {
        return ebid;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public Integer getType() {
        return type;
    }

    public String getLaneName() {
        return laneName;
    }

    public Integer getCid() {
        return cid;
    }

    public String getCityName() {
        return cityName;
    }

    public void setStationClass(Integer stationClass) {
        this.stationClass = stationClass;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public void setStationID(String stationID) {
        this.stationID = stationID;
    }

    public void setLocalStationID(String localStationID) {
        this.localStationID = localStationID;
    }

    public void setArsID(String arsID) {
        this.arsID = arsID;
    }

    public void setEbid(String ebid) {
        this.ebid = ebid;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public void setLaneName(String laneName) {
        this.laneName = laneName;
    }

    public void setCid(Integer cid) {
        this.cid = cid;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }
}

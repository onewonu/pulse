package com.pulse.entity.subway;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SubwayLineStationId implements Serializable {

    @Column(name = "line_name", length = 50)
    private String lineName;

    @Column(name = "station_name", length = 100)
    private String stationName;

    protected SubwayLineStationId() {}

    private SubwayLineStationId(String lineName, String stationName) {
        this.lineName = lineName;
        this.stationName = stationName;
    }

    public static SubwayLineStationId of(String lineName, String stationName) {
        return new SubwayLineStationId(lineName, stationName);
    }

    public String getLineName() {
        return lineName;
    }

    public String getStationName() {
        return stationName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubwayLineStationId that = (SubwayLineStationId) o;
        return Objects.equals(lineName, that.lineName) &&
                Objects.equals(stationName, that.stationName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineName, stationName);
    }
}

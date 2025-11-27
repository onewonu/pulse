package com.pulse.entity.bus;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class BusRouteStopId implements Serializable {

    @Column(name = "route_number", length = 50)
    private String routeNumber;

    @Column(name = "stop_id", length = 50)
    private String stopId;

    protected BusRouteStopId() {}

    private BusRouteStopId(String routeNumber, String stopId) {
        this.routeNumber = routeNumber;
        this.stopId = stopId;
    }

    public static BusRouteStopId of(String routeNumber, String stopId) {
        return new BusRouteStopId(routeNumber, stopId);
    }

    public String getRouteNumber() {
        return routeNumber;
    }

    public String getStopId() {
        return stopId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BusRouteStopId that = (BusRouteStopId) o;
        return Objects.equals(routeNumber, that.routeNumber) &&
                Objects.equals(stopId, that.stopId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeNumber, stopId);
    }
}

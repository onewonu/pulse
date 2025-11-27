package com.pulse.entity.bus;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "bus_route_stops")
@EntityListeners(AuditingEntityListener.class)
public class BusRouteStop {

    @EmbeddedId
    private BusRouteStopId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("routeNumber")
    @JoinColumn(name = "route_number")
    private BusRoute busRoute;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("stopId")
    @JoinColumn(name = "stop_id")
    private BusStop busStop;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BusRouteStop() {}

    private BusRouteStop(BusRoute busRoute, BusStop busStop) {
        this.id = BusRouteStopId.of(busRoute.getRouteNumber(), busStop.getStopId());
        this.busRoute = busRoute;
        this.busStop = busStop;
    }

    public static BusRouteStop of(BusRoute busRoute, BusStop busStop) {
        return new BusRouteStop(busRoute, busStop);
    }

    public BusRouteStopId getId() {
        return id;
    }

    public BusRoute getBusRoute() {
        return busRoute;
    }

    public BusStop getBusStop() {
        return busStop;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

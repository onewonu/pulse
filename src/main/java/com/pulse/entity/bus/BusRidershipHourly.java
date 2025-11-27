package com.pulse.entity.bus;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bus_ridership_hourly",
        indexes = {
                @Index(name = "idx_stat_date", columnList = "stat_date"),
                @Index(name = "idx_route_stop", columnList = "route_number, stop_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bus_stat",
                        columnNames = {"stat_date", "route_number", "stop_id", "hour_slot"}
                )
        })
public class BusRidershipHourly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_number", nullable = false)
    private BusRoute busRoute;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_id", nullable = false)
    private BusStop busStop;

    @Column(name = "hour_slot", nullable = false)
    private Byte hourSlot;

    @Column(name = "boarding_count", nullable = false)
    private Integer boardingCount;

    @Column(name = "alighting_count", nullable = false)
    private Integer alightingCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BusRidershipHourly() {}

    private BusRidershipHourly(
            LocalDate statDate
            , BusRoute busRoute
            , BusStop busStop
            , Byte hourSlot
            , Integer boardingCount
            , Integer alightingCount
    ) {
        this.statDate = statDate;
        this.busRoute = busRoute;
        this.busStop = busStop;
        this.hourSlot = hourSlot;
        this.boardingCount = boardingCount;
        this.alightingCount = alightingCount;
    }

    public static BusRidershipHourly of(
            LocalDate statDate
            , BusRoute busRoute
            , BusStop busStop
            , Byte hourSlot
            , Integer boardingCount
            , Integer alightingCount
    ) {
        return new BusRidershipHourly(statDate, busRoute, busStop, hourSlot, boardingCount, alightingCount);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public BusRoute getBusRoute() {
        return busRoute;
    }

    public BusStop getBusStop() {
        return busStop;
    }

    public Byte getHourSlot() {
        return hourSlot;
    }

    public Integer getBoardingCount() {
        return boardingCount;
    }

    public Integer getAlightingCount() {
        return alightingCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

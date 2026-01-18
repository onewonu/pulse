package com.pulse.entity.subway;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subway_passenger_hourly",
        indexes = {
                @Index(name = "idx_stat_date", columnList = "stat_date"),
                @Index(name = "idx_station", columnList = "station_id"),
                @Index(name = "idx_station_hour", columnList = "station_id, hour_slot")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subway_stat",
                        columnNames = {"stat_date", "station_id", "hour_slot"}
                )
        })
public class SubwayPassengerHourly {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subway_passenger_hourly_seq")
    @SequenceGenerator(
            name = "subway_passenger_hourly_seq",
            sequenceName = "subway_passenger_hourly_seq",
            allocationSize = 500
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private SubwayStation subwayStation;

    @Column(name = "hour_slot", nullable = false)
    private Byte hourSlot;

    @Column(name = "boarding_count", nullable = false)
    private Integer boardingCount;

    @Column(name = "alighting_count", nullable = false)
    private Integer alightingCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SubwayPassengerHourly() {}

    private SubwayPassengerHourly(
            LocalDate statDate,
            SubwayStation subwayStation,
            Byte hourSlot,
            Integer boardingCount,
            Integer alightingCount
    ) {
        this.statDate = statDate;
        this.subwayStation = subwayStation;
        this.hourSlot = hourSlot;
        this.boardingCount = boardingCount;
        this.alightingCount = alightingCount;
    }

    public static SubwayPassengerHourly of(
            LocalDate statDate,
            SubwayStation subwayStation,
            Byte hourSlot,
            Integer boardingCount,
            Integer alightingCount
    ) {
        return new SubwayPassengerHourly(statDate, subwayStation, hourSlot, boardingCount, alightingCount);
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

    public SubwayStation getSubwayStation() {
        return subwayStation;
    }

    public SubwayLine getSubwayLine() {
        return subwayStation != null ? subwayStation.getSubwayLine() : null;
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
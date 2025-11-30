package com.pulse.entity.subway;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subway_ridership_hourly",
        indexes = {
                @Index(name = "idx_stat_date", columnList = "stat_date"),
                @Index(name = "idx_line_station", columnList = "line_name, station_name")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subway_stat",
                        columnNames = {"stat_date", "line_name", "station_name", "hour_slot"}
                )
        })
public class SubwayRidershipHourly {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subway_ridership_hourly_seq")
    @SequenceGenerator(
            name = "subway_ridership_hourly_seq",
            sequenceName = "subway_ridership_hourly_seq",
            allocationSize = 500
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_name", nullable = false)
    private SubwayLine subwayLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_name", nullable = false)
    private SubwayStation subwayStation;

    @Column(name = "hour_slot", nullable = false)
    private Byte hourSlot;

    @Column(name = "boarding_count", nullable = false)
    private Integer boardingCount;

    @Column(name = "alighting_count", nullable = false)
    private Integer alightingCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SubwayRidershipHourly() {}

    private SubwayRidershipHourly(
            LocalDate statDate,
            SubwayLine subwayLine,
            SubwayStation subwayStation,
            Byte hourSlot,
            Integer boardingCount,
            Integer alightingCount
    ) {
        this.statDate = statDate;
        this.subwayLine = subwayLine;
        this.subwayStation = subwayStation;
        this.hourSlot = hourSlot;
        this.boardingCount = boardingCount;
        this.alightingCount = alightingCount;
    }

    public static SubwayRidershipHourly of(
            LocalDate statDate,
            SubwayLine subwayLine,
            SubwayStation subwayStation,
            Byte hourSlot,
            Integer boardingCount,
            Integer alightingCount
    ) {
        return new SubwayRidershipHourly(statDate, subwayLine, subwayStation, hourSlot, boardingCount, alightingCount);
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

    public SubwayLine getSubwayLine() {
        return subwayLine;
    }

    public SubwayStation getSubwayStation() {
        return subwayStation;
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
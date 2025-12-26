package com.pulse.entity.subway;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "subway_train_schedule",
        indexes = {
                @Index(name = "idx_departure_arrival_day",
                        columnList = "departure_station_name, arrival_station_name, day_type, departure_time"),
                @Index(name = "idx_valid_period",
                        columnList = "valid_from, valid_to")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_train_schedule",
                        columnNames = {"train_no", "departure_station_name", "departure_time", "day_type"}
                )
        })
public class SubwayTrainSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "subway_train_schedule_seq")
    @SequenceGenerator(
            name = "subway_train_schedule_seq",
            sequenceName = "subway_train_schedule_seq",
            allocationSize = 500
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "train_no", length = 50)
    private String trainNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_station_name", nullable = false)
    private SubwayStation departureStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_station_name", nullable = false)
    private SubwayStation arrivalStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_name", nullable = false)
    private SubwayLine line;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(name = "updown_type", length = 10)
    private String updownType;

    @Column(name = "day_type", length = 10, nullable = false)
    private String dayType;

    @Column(name = "is_express")
    private Boolean isExpress;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SubwayTrainSchedule() {}

    private SubwayTrainSchedule(
            String trainNo,
            SubwayStation departureStation,
            SubwayStation arrivalStation,
            SubwayLine line,
            LocalTime departureTime,
            LocalTime arrivalTime,
            String updownType,
            String dayType,
            Boolean isExpress,
            LocalDateTime validFrom,
            LocalDateTime validTo
    ) {
        this.trainNo = trainNo;
        this.departureStation = departureStation;
        this.arrivalStation = arrivalStation;
        this.line = line;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.updownType = updownType;
        this.dayType = dayType;
        this.isExpress = isExpress;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public static SubwayTrainSchedule of(
            String trainNo,
            SubwayStation departureStation,
            SubwayStation arrivalStation,
            SubwayLine line,
            LocalTime departureTime,
            LocalTime arrivalTime,
            String updownType,
            String dayType,
            Boolean isExpress,
            LocalDateTime validFrom,
            LocalDateTime validTo
    ) {
        return new SubwayTrainSchedule(
                trainNo,
                departureStation,
                arrivalStation,
                line,
                departureTime,
                arrivalTime,
                updownType,
                dayType,
                isExpress,
                validFrom,
                validTo
        );
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTrainNo() {
        return trainNo;
    }

    public SubwayStation getDepartureStation() {
        return departureStation;
    }

    public SubwayStation getArrivalStation() {
        return arrivalStation;
    }

    public SubwayLine getLine() {
        return line;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public String getUpdownType() {
        return updownType;
    }

    public String getDayType() {
        return dayType;
    }

    public Boolean getIsExpress() {
        return isExpress;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

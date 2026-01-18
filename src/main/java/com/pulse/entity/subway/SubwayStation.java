package com.pulse.entity.subway;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "subway_stations",
        indexes = {
                @Index(name = "idx_station_name", columnList = "station_name"),
                @Index(name = "idx_station_name_line", columnList = "station_name, line_name")
        })
@EntityListeners(AuditingEntityListener.class)
public class SubwayStation {

    @Id
    @Column(name = "station_id", length = 20)
    private String stationId;

    @Column(name = "station_name", length = 100, nullable = false)
    private String stationName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_name", nullable = false)
    private SubwayLine subwayLine;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SubwayStation() {}

    private SubwayStation(
            String stationId,
            String stationName,
            SubwayLine subwayLine,
            Double latitude,
            Double longitude
    ) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.subwayLine = subwayLine;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static SubwayStation of(
            String stationId,
            String stationName,
            SubwayLine subwayLine,
            Double latitude,
            Double longitude
    ) {
        return new SubwayStation(stationId, stationName, subwayLine, latitude, longitude);
    }

    public String getStationId() {
        return stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public SubwayLine getSubwayLine() {
        return subwayLine;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

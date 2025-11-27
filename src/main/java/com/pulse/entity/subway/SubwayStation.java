package com.pulse.entity.subway;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "subway_stations")
@EntityListeners(AuditingEntityListener.class)
public class SubwayStation {

    @Id
    @Column(name = "station_name", length = 100)
    private String stationName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SubwayStation() {}

    private SubwayStation(String stationName) {
        this.stationName = stationName;
    }

    public static SubwayStation of(String stationName) {
        return new SubwayStation(stationName);
    }

    public String getStationName() {
        return stationName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

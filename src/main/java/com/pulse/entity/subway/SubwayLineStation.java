package com.pulse.entity.subway;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "subway_line_stations")
@EntityListeners(AuditingEntityListener.class)
public class SubwayLineStation {

    @EmbeddedId
    private SubwayLineStationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("lineName")
    @JoinColumn(name = "line_name")
    private SubwayLine subwayLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("stationName")
    @JoinColumn(name = "station_name")
    private SubwayStation subwayStation;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SubwayLineStation() {}

    private SubwayLineStation(SubwayLine subwayLine, SubwayStation subwayStation) {
        this.id = SubwayLineStationId.of(subwayLine.getLineName(), subwayStation.getStationName());
        this.subwayLine = subwayLine;
        this.subwayStation = subwayStation;
    }

    public static SubwayLineStation of(SubwayLine subwayLine, SubwayStation subwayStation) {
        return new SubwayLineStation(subwayLine, subwayStation);
    }

    public SubwayLineStationId getId() {
        return id;
    }

    public SubwayLine getSubwayLine() {
        return subwayLine;
    }

    public SubwayStation getSubwayStation() {
        return subwayStation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

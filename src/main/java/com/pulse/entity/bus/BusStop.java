package com.pulse.entity.bus;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "bus_stops")
@EntityListeners(AuditingEntityListener.class)
public class BusStop {

    @Id
    @Column(name = "stop_id", length = 50)
    private String stopId;

    @Column(name = "ars_number", length = 20)
    private String arsNumber;

    @Column(name = "stop_name", nullable = false, length = 200)
    private String stopName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BusStop() {}

    private BusStop(String stopId, String arsNumber, String stopName) {
        this.stopId = stopId;
        this.arsNumber = arsNumber;
        this.stopName = stopName;
    }

    public static BusStop of(String stopId, String arsNumber, String stopName) {
        return new BusStop(stopId, arsNumber, stopName);
    }

    public String getStopId() {
        return stopId;
    }

    public String getArsNumber() {
        return arsNumber;
    }

    public String getStopName() {
        return stopName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

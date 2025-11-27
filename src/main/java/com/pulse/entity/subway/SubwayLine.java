package com.pulse.entity.subway;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "subway_lines")
@EntityListeners(AuditingEntityListener.class)
public class SubwayLine {

    @Id
    @Column(name = "line_name", length = 50)
    private String lineName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SubwayLine() {}

    private SubwayLine(String lineName) {
        this.lineName = lineName;
    }

    public static SubwayLine of(String lineName) {
        return new SubwayLine(lineName);
    }

    public String getLineName() {
        return lineName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

package com.pulse.entity.bookmark;

import com.pulse.entity.user.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
    name = "bookmarks",
    indexes = {
        @Index(name = "idx_user_display_order", columnList = "user_id, display_order"),
        @Index(name = "idx_user_id", columnList = "user_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "departure_station_id", nullable = false)
    private Integer departureStationId;

    @Column(name = "arrival_station_id", nullable = false)
    private Integer arrivalStationId;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bookmark_user"))
    private User user;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Bookmark() {}

    private Bookmark(
            String name,
            Integer departureStationId,
            Integer arrivalStationId,
            LocalTime startTime,
            LocalTime endTime,
            Integer displayOrder,
            User user
    ) {
        this.name = name;
        this.departureStationId = departureStationId;
        this.arrivalStationId = arrivalStationId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.displayOrder = displayOrder;
        this.user = user;
    }

    public static Bookmark of(
            String name,
            Integer departureStationId,
            Integer arrivalStationId,
            LocalTime startTime,
            LocalTime endTime,
            Integer displayOrder,
            User user
    ) {
        return new Bookmark(name, departureStationId, arrivalStationId, startTime, endTime, displayOrder, user);
    }

    public void update(String name, Integer departureStationId, Integer arrivalStationId, LocalTime startTime, LocalTime endTime) {
        if (name != null) {
            this.name = name;
        }
        if (departureStationId != null) {
            this.departureStationId = departureStationId;
        }
        if (arrivalStationId != null) {
            this.arrivalStationId = arrivalStationId;
        }
        if (startTime != null) {
            this.startTime = startTime;
        }
        if (endTime != null) {
            this.endTime = endTime;
        }
    }

    public void updateDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getDepartureStationId() {
        return departureStationId;
    }

    public Integer getArrivalStationId() {
        return arrivalStationId;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

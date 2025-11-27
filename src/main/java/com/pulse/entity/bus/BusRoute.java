package com.pulse.entity.bus;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "bus_routes")
@EntityListeners(AuditingEntityListener.class)
public class BusRoute {

    @Id
    @Column(name = "route_number", length = 50)
    private String routeNumber;

    @Column(name = "route_name", nullable = false, length = 200)
    private String routeName;

    @Column(name = "route_type_code", length = 20)
    private String routeTypeCode;

    @Column(name = "route_type_name", length = 100)
    private String routeTypeName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BusRoute() {}

    private BusRoute(String routeNumber, String routeName, String routeTypeCode, String routeTypeName) {
        this.routeNumber = routeNumber;
        this.routeName = routeName;
        this.routeTypeCode = routeTypeCode;
        this.routeTypeName = routeTypeName;
    }

    public static BusRoute of(String routeNumber, String routeName, String routeTypeCode, String routeTypeName) {
        return new BusRoute(routeNumber, routeName, routeTypeCode, routeTypeName);
    }

    public String getRouteNumber() {
        return routeNumber;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getRouteTypeCode() {
        return routeTypeCode;
    }

    public String getRouteTypeName() {
        return routeTypeName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

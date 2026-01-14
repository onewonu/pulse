package com.pulse.dto.bookmark;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class BookmarkCreateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Departure station ID is required")
    private Integer departureStationId;

    @NotNull(message = "Arrival station ID is required")
    private Integer arrivalStationId;

    @NotNull(message = "Display order is required")
    @Min(value = 0, message = "Display order must be greater than or equal to 0")
    private Integer displayOrder;

    public String getName() {
        return name;
    }

    public Integer getDepartureStationId() {
        return departureStationId;
    }

    public Integer getArrivalStationId() {
        return arrivalStationId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDepartureStationId(Integer departureStationId) {
        this.departureStationId = departureStationId;
    }

    public void setArrivalStationId(Integer arrivalStationId) {
        this.arrivalStationId = arrivalStationId;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}

package com.pulse.dto.bookmark;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Size;

public class BookmarkUpdateRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    @JsonSetter(nulls = Nulls.FAIL)
    private String name;

    @JsonSetter(nulls = Nulls.FAIL)
    private Integer departureStationId;

    @JsonSetter(nulls = Nulls.FAIL)
    private Integer arrivalStationId;

    public String getName() {
        return name;
    }

    public Integer getDepartureStationId() {
        return departureStationId;
    }

    public Integer getArrivalStationId() {
        return arrivalStationId;
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
}

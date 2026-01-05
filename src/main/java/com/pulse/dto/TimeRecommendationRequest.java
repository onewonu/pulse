package com.pulse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public class TimeRecommendationRequest {

    @NotNull
    @Positive
    private Integer departureStationId;

    @NotNull
    @Positive
    private Integer arrivalStationId;

    @NotNull
    private LocalDate searchDate;

    @NotNull
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    public TimeRecommendationRequest(
            Integer departureStationId,
            Integer arrivalStationId,
            LocalDate searchDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        this.departureStationId = departureStationId;
        this.arrivalStationId = arrivalStationId;
        this.searchDate = searchDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Integer getDepartureStationId() {
        return departureStationId;
    }

    public Integer getArrivalStationId() {
        return arrivalStationId;
    }

    public LocalDate getSearchDate() {
        return searchDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setDepartureStationId(Integer departureStationId) {
        this.departureStationId = departureStationId;
    }

    public void setArrivalStationId(Integer arrivalStationId) {
        this.arrivalStationId = arrivalStationId;
    }

    public void setSearchDate(LocalDate searchDate) {
        this.searchDate = searchDate;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}

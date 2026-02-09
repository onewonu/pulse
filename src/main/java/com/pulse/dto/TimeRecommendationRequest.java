package com.pulse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public record TimeRecommendationRequest(
    @NotNull
    @Positive
    Integer departureStationId,

    @NotNull
    @Positive
    Integer arrivalStationId,

    @NotNull
    LocalDate searchDate,

    @NotNull
    @JsonFormat(pattern = "HH:mm")
    LocalTime startTime,

    @NotNull
    @JsonFormat(pattern = "HH:mm")
    LocalTime endTime
) {}

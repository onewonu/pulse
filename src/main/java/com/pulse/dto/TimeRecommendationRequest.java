package com.pulse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalTime;

public record TimeRecommendationRequest(
    @NotNull
    @NotBlank(message = "Departure station ID is required")
    @Pattern(regexp = "^\\d+$", message = "Station ID must be numeric")
    String departureStationId,

    @NotNull
    @NotBlank(message = "Arrival station ID is required")
    @Pattern(regexp = "^\\d+$", message = "Station ID must be numeric")
    String arrivalStationId,

    @NotNull
    LocalDate searchDate,

    @NotNull
    @JsonFormat(pattern = "HH:mm")
    LocalTime startTime,

    @NotNull
    @JsonFormat(pattern = "HH:mm")
    LocalTime endTime
) {}

package com.pulse.dto.bookmark;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record BookmarkCreateRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @NotNull(message = "Departure station ID is required")
    @NotBlank(message = "Departure station ID cannot be blank")
    @Pattern(regexp = "^\\d+$", message = "Station ID must be numeric")
    String departureStationId,

    @NotNull(message = "Arrival station ID is required")
    @NotBlank(message = "Arrival station ID cannot be blank")
    @Pattern(regexp = "^\\d+$", message = "Station ID must be numeric")
    String arrivalStationId,

    @NotNull(message = "Start time is required")
    LocalTime startTime,

    @NotNull(message = "End time is required")
    LocalTime endTime
) {}

package com.pulse.dto.bookmark;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookmarkCreateRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @NotNull(message = "Departure station ID is required")
    Integer departureStationId,

    @NotNull(message = "Arrival station ID is required")
    Integer arrivalStationId
) {}

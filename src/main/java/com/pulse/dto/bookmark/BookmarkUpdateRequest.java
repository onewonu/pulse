package com.pulse.dto.bookmark;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record BookmarkUpdateRequest(
    @Size(max = 100, message = "Name must not exceed 100 characters")
    @JsonSetter(nulls = Nulls.FAIL)
    String name,

    @JsonSetter(nulls = Nulls.FAIL)
    Integer departureStationId,

    @JsonSetter(nulls = Nulls.FAIL)
    Integer arrivalStationId,

    @JsonSetter(nulls = Nulls.FAIL)
    LocalTime startTime,

    @JsonSetter(nulls = Nulls.FAIL)
    LocalTime endTime
) {}

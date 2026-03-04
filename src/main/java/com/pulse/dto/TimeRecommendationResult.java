package com.pulse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TimeRecommendationResult(
    String departureStationId,
    String arrivalStationId,
    String departureStationName,
    String arrivalStationName,
    LocalDate travelDate,
    String dayType,
    List<TimeRecommendation> recommendations,
    String message
) {
    public record TimeRecommendation(
        @JsonFormat(pattern = "HH:mm")
        LocalTime departureTime,
        @JsonFormat(pattern = "HH:mm")
        LocalTime arrivalTime,
        int totalTime,
        int transferCount,
        double congestionScore,
        CongestionLevel congestionLevel,
        List<StationCongestion> stationCongestions
    ) {}

    public record StationCongestion(
        String stationId,
        String stationName,
        String lineName,
        String lineColor,
        @JsonFormat(pattern = "HH:mm")
        LocalTime arrivalTime,
        @JsonFormat(pattern = "HH:mm")
        LocalTime departureTime,
        Integer boardingCount,
        Integer alightingCount,
        Integer totalPassengers
    ) {}
}

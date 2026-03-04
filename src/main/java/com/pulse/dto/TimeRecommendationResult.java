package com.pulse.dto;

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
        LocalTime departureTime,
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
        LocalTime arrivalTime,
        LocalTime departureTime,
        Integer boardingCount,
        Integer alightingCount,
        Integer totalPassengers
    ) {}
}

package com.pulse.controller.web;

import com.pulse.dto.search.StationSearchResponse;
import com.pulse.dto.search.TimeRecommendationRequest;
import com.pulse.dto.search.TimeRecommendationResponse;
import com.pulse.service.search.TimeRecommendationService;
import com.pulse.service.search.StationSearchService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/search")
@Validated
public class StationSearchController {

    private final StationSearchService stationSearchService;
    private final TimeRecommendationService timeRecommendationService;

    public StationSearchController(
            StationSearchService stationSearchService,
            TimeRecommendationService timeRecommendationService
    ) {
        this.stationSearchService = stationSearchService;
        this.timeRecommendationService = timeRecommendationService;
    }

    @GetMapping("/station")
    public ResponseEntity<StationSearchResponse> searchStation(
            @RequestParam
            @NotBlank(message = "stationName cannot be blank")
            @Size(min = 2, message = "stationName must be at least 2 characters")
            String stationName
    ) {
        StationSearchResponse result = stationSearchService.searchStation(stationName);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/route")
    public ResponseEntity<TimeRecommendationResponse> recommendTimes(
            @RequestParam("departureStationId") @NotNull @NotBlank @Pattern(regexp = "^\\d+$", message = "Station ID must be numeric") String departureStationId,
            @RequestParam("arrivalStationId") @NotNull @NotBlank @Pattern(regexp = "^\\d+$", message = "Station ID must be numeric") String arrivalStationId,
            @RequestParam("searchDate") @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchDate,
            @RequestParam("startTime") @NotNull @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam("endTime") @NotNull @DateTimeFormat(pattern = "HH:mm") LocalTime endTime
    ) {
        TimeRecommendationRequest request = new TimeRecommendationRequest(
                departureStationId,
                arrivalStationId,
                searchDate,
                startTime,
                endTime
        );

        TimeRecommendationResponse result = timeRecommendationService.recommendTimes(request);
        return ResponseEntity.ok(result);
    }
}

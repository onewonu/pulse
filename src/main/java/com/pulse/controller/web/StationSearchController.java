package com.pulse.controller.web;

import com.pulse.dto.StationSearchResult;
import com.pulse.dto.TimeRecommendationRequest;
import com.pulse.dto.TimeRecommendationResult;
import com.pulse.service.search.TimeRecommendationService;
import com.pulse.service.search.StationSearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<StationSearchResult> searchStation(
            @RequestParam
            @NotBlank(message = "stationName cannot be blank")
            @Size(min = 2, message = "stationName must be at least 2 characters")
            String stationName
    ) {
        StationSearchResult result = stationSearchService.searchStation(stationName);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/route")
    public ResponseEntity<TimeRecommendationResult> recommendTimes(
            @Valid @RequestBody TimeRecommendationRequest request
    ) {
        TimeRecommendationResult result = timeRecommendationService.recommendTimes(request);
        return ResponseEntity.ok(result);
    }
}

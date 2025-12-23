package com.pulse.controller.web;

import com.pulse.dto.StationSearchResult;
import com.pulse.service.search.StationSearchService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@Validated
public class StationSearchController {

    private final StationSearchService stationSearchService;

    public StationSearchController(StationSearchService stationSearchService) {
        this.stationSearchService = stationSearchService;
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
}

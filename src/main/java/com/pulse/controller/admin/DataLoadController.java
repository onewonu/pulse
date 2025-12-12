package com.pulse.controller.admin;

import com.pulse.service.dataload.bus.BusMasterDataLoadService;
import com.pulse.dto.DataLoadResult;
import com.pulse.service.dataload.bus.BusStatisticsDataLoadService;
import com.pulse.service.dataload.subway.SubwayMasterDataLoadService;
import com.pulse.service.dataload.subway.SubwayStatisticsDataLoadService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/data-load")
@Validated
public class DataLoadController {

    private final BusMasterDataLoadService busMasterDataLoadService;
    private final BusStatisticsDataLoadService busStatisticsDataLoadService;
    private final SubwayMasterDataLoadService subwayMasterDataLoadService;
    private final SubwayStatisticsDataLoadService subwayStatisticsDataLoadService;

    public DataLoadController(
            BusMasterDataLoadService busDataLoadService,
            BusStatisticsDataLoadService busStatisticsDataLoadService,
            SubwayMasterDataLoadService subwayDataLoadService,
            SubwayStatisticsDataLoadService subwayStatisticsData
    ) {
        this.busMasterDataLoadService = busDataLoadService;
        this.busStatisticsDataLoadService = busStatisticsDataLoadService;
        this.subwayMasterDataLoadService = subwayDataLoadService;
        this.subwayStatisticsDataLoadService = subwayStatisticsData;
    }

    @PostMapping("/bus/master")
    public ResponseEntity<DataLoadResult> loadBusMasterData(
            @RequestParam
            @NotBlank(message = "yearMonth cannot be blank")
            @Pattern(regexp = "^\\d{6}$", message = "yearMonth must be 6 digits in yyyyMM format")
            String yearMonth
    ) {
        DataLoadResult result = busMasterDataLoadService.loadBusMasterData(yearMonth);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/bus/statistics")
    public ResponseEntity<DataLoadResult> loadBusStatistics(
            @RequestParam
            @NotBlank(message = "yearMonth cannot be blank")
            @Pattern(regexp = "^\\d{6}$", message = "yearMonth must be 6 digits in yyyyMM format")
            String yearMonth
    ) {
        DataLoadResult result = busStatisticsDataLoadService.loadBusStatisticsData(yearMonth);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/subway/master")
    public ResponseEntity<DataLoadResult> loadSubwayMasterData(
            @RequestParam
            @NotBlank(message = "yearMonth cannot be blank")
            @Pattern(regexp = "^\\d{6}$", message = "yearMonth must be 6 digits in yyyyMM format")
            String yearMonth
    ) {
        DataLoadResult result = subwayMasterDataLoadService.loadSubwayMasterData(yearMonth);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/subway/statistics")
    public ResponseEntity<DataLoadResult> loadSubwayStatistics(
            @RequestParam
            @NotBlank(message = "yearMonth cannot be blank")
            @Pattern(regexp = "^\\d{6}$", message = "yearMonth must be 6 digits in yyyyMM format")
            String yearMonth
    ) {
        DataLoadResult result = subwayStatisticsDataLoadService.loadSubwayStatisticsData(yearMonth);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/all")
    public ResponseEntity<Map<String, DataLoadResult>> loadAllData(
            @RequestParam
            @NotBlank(message = "yearMonth cannot be blank")
            @Pattern(regexp = "^\\d{6}$", message = "yearMonth must be 6 digits in yyyyMM format")
            String yearMonth
    ) {
        Map<String, DataLoadResult> results = new HashMap<>();

        results.put("subwayMaster", subwayMasterDataLoadService.loadSubwayMasterData(yearMonth));
        results.put("subwayStatistics", subwayStatisticsDataLoadService.loadSubwayStatisticsData(yearMonth));

        results.put("busMaster", busMasterDataLoadService.loadBusMasterData(yearMonth));
        results.put("busStatistics", busStatisticsDataLoadService.loadBusStatisticsData(yearMonth));

        return ResponseEntity.ok(results);
    }
}

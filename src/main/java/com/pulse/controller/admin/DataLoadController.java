package com.pulse.controller.admin;

import com.pulse.service.dataload.BusDataLoadService;
import com.pulse.dto.DataLoadResult;
import com.pulse.service.dataload.SubwayDataLoadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/data-load")
public class DataLoadController {

    private final BusDataLoadService busDataLoadService;
    private final SubwayDataLoadService subwayDataLoadService;

    public DataLoadController(BusDataLoadService busDataLoadService, SubwayDataLoadService subwayDataLoadService) {
        this.busDataLoadService = busDataLoadService;
        this.subwayDataLoadService = subwayDataLoadService;
    }

    @PostMapping("/bus/master")
    public ResponseEntity<DataLoadResult> loadBusMasterData(@RequestParam String yearMonth) {
        DataLoadResult result = busDataLoadService.loadBusMasterData(yearMonth);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/bus/statistics")
    public ResponseEntity<DataLoadResult> loadBusStatistics(@RequestParam String yearMonth) {
        DataLoadResult result = busDataLoadService.loadBusStatisticsData(yearMonth);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/subway/master")
    public ResponseEntity<DataLoadResult> loadSubwayMasterData(@RequestParam String yearMonth) {
        DataLoadResult result = subwayDataLoadService.loadSubwayMasterData(yearMonth);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/subway/statistics")
    public ResponseEntity<DataLoadResult> loadSubwayStatistics(@RequestParam String yearMonth) {
        DataLoadResult result = subwayDataLoadService.loadSubwayStatisticsData(yearMonth);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/all")
    public ResponseEntity<Map<String, DataLoadResult>> loadAllData(@RequestParam String yearMonth) {
        Map<String, DataLoadResult> results = new HashMap<>();

        results.put("subwayMaster", subwayDataLoadService.loadSubwayMasterData(yearMonth));
        results.put("subwayStatistics", subwayDataLoadService.loadSubwayStatisticsData(yearMonth));

        results.put("busMaster", busDataLoadService.loadBusMasterData(yearMonth));
        results.put("busStatistics", busDataLoadService.loadBusStatisticsData(yearMonth));

        return ResponseEntity.ok(results);
    }
}

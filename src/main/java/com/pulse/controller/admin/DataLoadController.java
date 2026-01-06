package com.pulse.controller.admin;

import com.pulse.dto.DataLoadResult;
import com.pulse.service.dataload.subway.SubwayMasterDataLoadService;
import com.pulse.service.dataload.subway.SubwayStatisticsDataLoadService;
import com.pulse.service.dataload.subway.TrainScheduleDataLoadService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    private final SubwayMasterDataLoadService subwayMasterDataLoadService;
    private final SubwayStatisticsDataLoadService subwayStatisticsDataLoadService;
    private final TrainScheduleDataLoadService trainScheduleDataLoadService;

    public DataLoadController(
            SubwayMasterDataLoadService subwayMasterDataLoadService,
            SubwayStatisticsDataLoadService subwayStatisticsDataLoadService,
            TrainScheduleDataLoadService trainScheduleDataLoadService
    ) {
        this.subwayMasterDataLoadService = subwayMasterDataLoadService;
        this.subwayStatisticsDataLoadService = subwayStatisticsDataLoadService;
        this.trainScheduleDataLoadService = trainScheduleDataLoadService;
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

        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/subway/statistics")
    public ResponseEntity<DataLoadResult> deleteSubwayStatistics(
            @RequestParam
            @NotBlank(message = "yearMonth cannot be blank")
            @Pattern(regexp = "^\\d{6}$", message = "yearMonth must be 6 digits in yyyyMM format")
            String yearMonth
    ) {
        DataLoadResult result = subwayStatisticsDataLoadService.deleteStatisticsByYearMonth(yearMonth);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/train-schedule/weekday")
    public ResponseEntity<DataLoadResult> loadWeekdayTrainSchedule(
            @RequestParam(required = false)
            String stationName,
            @RequestParam(required = false)
            String lineName
    ) {
        DataLoadResult result = trainScheduleDataLoadService.loadTrainSchedules("평일", stationName, lineName);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/train-schedule/all")
    public ResponseEntity<Map<String, DataLoadResult>> loadAllTrainSchedules() {
        Map<String, DataLoadResult> results = new HashMap<>();
        String[] dayTypes = {"평일", "주말"};

        for (String dayType : dayTypes) {
            results.put(dayType, trainScheduleDataLoadService.loadTrainSchedules(dayType, null, null));
        }

        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/train-schedule/all")
    public ResponseEntity<DataLoadResult> deleteAllTrainSchedules() {
        DataLoadResult result = trainScheduleDataLoadService.deleteAllTrainSchedules();
        return ResponseEntity.ok(result);
    }
}

package com.pulse.service.dataload;

import com.pulse.client.transport.SeoulOpenApiClient;
import com.pulse.client.transport.dto.subway.SubwayApiResponse;
import com.pulse.client.transport.dto.subway.SubwayRidershipData;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.subway.*;
import com.pulse.mapper.SubwayDataMapper;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayLineStationRepository;
import com.pulse.repository.subway.SubwayRidershipHourlyRepository;
import com.pulse.repository.subway.SubwayStationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class SubwayDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(SubwayDataLoadService.class);

    private final SeoulOpenApiClient apiClient;
    private final SubwayDataMapper mapper;
    private final SubwayLineRepository subwayLineRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final SubwayLineStationRepository subwayLineStationRepository;
    private final SubwayRidershipHourlyRepository subwayRidershipRepository;

    @Value("${seoul-api.page-size}")
    private int pageSize;

    public SubwayDataLoadService(
            SeoulOpenApiClient apiClient,
            SubwayDataMapper mapper,
            SubwayLineRepository subwayLineRepository,
            SubwayStationRepository subwayStationRepository,
            SubwayLineStationRepository subwayLineStationRepository,
            SubwayRidershipHourlyRepository subwayRidershipRepository
    ) {
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.subwayLineRepository = subwayLineRepository;
        this.subwayStationRepository = subwayStationRepository;
        this.subwayLineStationRepository = subwayLineStationRepository;
        this.subwayRidershipRepository = subwayRidershipRepository;
    }

    public DataLoadResult loadSubwayMasterData(String yearMonth) {
        log.info("Start loading subway master data: {}", yearMonth);

        try {
            subwayLineStationRepository.deleteAll();
            subwayLineRepository.deleteAll();
            subwayStationRepository.deleteAll();
            log.info("Existing subway master data has been deleted");

            Map<String, SubwayLine> lineMap = new HashMap<>();
            Map<String, SubwayStation> stationMap = new HashMap<>();
            Set<SubwayLineStationId> lineStationSet = new HashSet<>();
            int apiRecordCount = 0;
            int startIndex = 1;

            while (true) {
                int endIndex = startIndex + pageSize - 1;
                SubwayApiResponse response = apiClient.fetchSubwayRidershipData(yearMonth, startIndex, endIndex);

                if (response == null || response.getData() == null || response.getData().isEmpty()) {
                    log.info("Subway master data is empty.");
                    break;
                }

                for (SubwayRidershipData data : response.getData()) {
                    SubwayLine line = mapper.toSubwayLine(data);
                    lineMap.put(line.getLineName(), line);

                    SubwayStation station = mapper.toSubwayStation(data);
                    stationMap.put(station.getStationName(), station);

                    lineStationSet.add(SubwayLineStationId.of(line.getLineName(), station.getStationName()));

                    apiRecordCount++;
                }

                log.info("Subway master data progress: {} ~ {} (API records: {})",
                        startIndex, endIndex, apiRecordCount);
                startIndex = endIndex + 1;
            }

            List<SubwayLine> uniqueLines = new ArrayList<>(lineMap.values());
            List<SubwayStation> uniqueStations = new ArrayList<>(stationMap.values());

            subwayLineRepository.saveAll(uniqueLines);
            subwayStationRepository.saveAll(uniqueStations);
            log.info("Saved {} unique lines and {} unique stations", uniqueLines.size(), uniqueStations.size());

            List<SubwayLineStation> lineStations = new ArrayList<>();
            for (SubwayLineStationId id : lineStationSet) {
                SubwayLine line = lineMap.get(id.getLineName());
                SubwayStation station = stationMap.get(id.getStationName());
                lineStations.add(SubwayLineStation.of(line, station));
            }
            subwayLineStationRepository.saveAll(lineStations);

            int totalCount = apiRecordCount;
            log.info("Subway master data loading completed: {} API records -> {} lines, {} stations, {} line-stations",
                    apiRecordCount, uniqueLines.size(), uniqueStations.size(), lineStations.size());

            return DataLoadResult.success("Subway master data", totalCount);

        } catch (Exception e) {
            log.error("Subway master data load failure", e);

            return DataLoadResult.failure("Subway master data", e.getMessage());
        }
    }

    public DataLoadResult loadSubwayStatisticsData(String yearMonth) {
        log.info("Start loading subway statistics data: {}", yearMonth);

        try {
            LocalDate statDate = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyyMM")).atDay(1);
            subwayRidershipRepository.deleteByStatDate(statDate);
            log.info("Existing subway statistics data has been deleted: {}", yearMonth);

            Map<String, SubwayLine> lineCache = new HashMap<>();
            for (SubwayLine line : subwayLineRepository.findAll()) {
                lineCache.put(line.getLineName(), line);
            }
            log.info("Loaded {} lines into cache", lineCache.size());

            Map<String, SubwayStation> stationCache = new HashMap<>();
            for (SubwayStation station : subwayStationRepository.findAll()) {
                stationCache.put(station.getStationName(), station);
            }
            log.info("Loaded {} stations into cache", stationCache.size());

            Map<String, SubwayRidershipHourly> hourlyDataMap = new HashMap<>();
            int apiRecordCount = 0;
            int startIndex = 1;

            while (true) {
                int endIndex = startIndex + pageSize - 1;
                SubwayApiResponse response = apiClient.fetchSubwayRidershipData(yearMonth, startIndex, endIndex);

                if (response == null || response.getData() == null || response.getData().isEmpty()) {
                    log.info("Subway statistics data is empty.");
                    break;
                }

                for (SubwayRidershipData data : response.getData()) {
                    SubwayLine line = lineCache.get(data.getSbwyRoutLnNm());
                    if (line == null) {
                        throw new IllegalStateException("No line master data: " + data.getSbwyRoutLnNm());
                    }

                    SubwayStation station = stationCache.get(data.getSttn());
                    if (station == null) {
                        throw new IllegalStateException("No station master data: " + data.getSttn());
                    }

                    List<SubwayRidershipHourly> hourlyData = mapper.toSubwayRidershipHourlyList(data, line, station);

                    for (SubwayRidershipHourly hourly : hourlyData) {
                        String key = String.format("%s-%s-%s-%d",
                                hourly.getStatDate(),
                                hourly.getSubwayLine().getLineName(),
                                hourly.getSubwayStation().getStationName(),
                                hourly.getHourSlot());

                        hourlyDataMap.put(key, hourly);
                    }

                    apiRecordCount++;
                }

                log.info("Subway statistics data progress: {} ~ {} (API records: {})",
                        startIndex, endIndex, apiRecordCount);

                startIndex = endIndex + 1;
            }

            List<SubwayRidershipHourly> uniqueHourlyData = new ArrayList<>(hourlyDataMap.values());
            subwayRidershipRepository.saveAll(uniqueHourlyData);

            int totalCount = uniqueHourlyData.size();
            log.info("Subway statistics data loading completed: {} API records -> {} unique hourly records",
                    apiRecordCount, totalCount);

            return DataLoadResult.success("Subway statistics data", totalCount);

        } catch (Exception e) {
            log.error("Failure to load subway statistics data", e);
            return DataLoadResult.failure("Subway statistics data", e.getMessage());
        }
    }
}

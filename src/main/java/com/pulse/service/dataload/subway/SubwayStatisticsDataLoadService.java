package com.pulse.service.dataload.subway;

import com.pulse.api.seoulopendataplaza.SeoulOpenDataPlazaClient;
import com.pulse.api.seoulopendataplaza.dto.subway.SubwayApiResponse;
import com.pulse.api.seoulopendataplaza.dto.subway.SubwayRidershipData;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayRidershipHourly;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.mapper.SubwayDataMapper;
import com.pulse.repository.subway.SubwayLineRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SubwayStatisticsDataLoadService {
    
    private static final Logger log = LoggerFactory.getLogger(SubwayMasterDataLoadService.class);

    private final SeoulOpenDataPlazaClient apiClient;
    private final SubwayDataMapper mapper;
    private final SubwayLineRepository subwayLineRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final SubwayRidershipHourlyRepository subwayRidershipRepository;

    @Value("${seoul-api.page-size}")
    private int pageSize;

    public SubwayStatisticsDataLoadService(
            SeoulOpenDataPlazaClient apiClient,
            SubwayDataMapper mapper,
            SubwayLineRepository subwayLineRepository,
            SubwayStationRepository subwayStationRepository,
            SubwayRidershipHourlyRepository subwayRidershipRepository
    ) {
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.subwayLineRepository = subwayLineRepository;
        this.subwayStationRepository = subwayStationRepository;
        this.subwayRidershipRepository = subwayRidershipRepository;
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

                if (response == null) {
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

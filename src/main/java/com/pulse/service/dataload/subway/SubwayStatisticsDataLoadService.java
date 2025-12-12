package com.pulse.service.dataload.subway;

import com.pulse.api.seoulopendataplaza.SeoulOpenDataPlazaClient;
import com.pulse.api.seoulopendataplaza.dto.subway.SubwayApiResponse;
import com.pulse.api.seoulopendataplaza.dto.subway.SubwayRidershipData;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayRidershipHourly;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.exception.dataload.MasterDataNotFoundException;
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
    
    private static final Logger log = LoggerFactory.getLogger(SubwayStatisticsDataLoadService.class);

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

        deleteSameYearAndMonth(yearMonth);

        MasterDataCaches caches = loadMasterDataCaches();

        List<SubwayRidershipData> apiDataList = fetchAllDataFromApi(yearMonth);

        Map<String, SubwayRidershipHourly> hourlyDataMap = processRidershipData(apiDataList, caches);

        int totalCount = saveRidershipData(hourlyDataMap, apiDataList.size());
        return DataLoadResult.success("Subway statistics data", totalCount);
    }

    private void deleteSameYearAndMonth(String yearMonth) {
        LocalDate statDate = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyyMM")).atDay(1);
        subwayRidershipRepository.deleteByStatDate(statDate);
        log.info("Existing subway statistics data has been deleted: {}", yearMonth);
    }

    private MasterDataCaches loadMasterDataCaches() {
        Map<String, SubwayLine> lineCache = new HashMap<>();
        for (SubwayLine line : subwayLineRepository.findAll()) {
            lineCache.put(line.getLineName(), line);
        }

        Map<String, SubwayStation> stationCache = new HashMap<>();
        for (SubwayStation station : subwayStationRepository.findAll()) {
            stationCache.put(station.getStationName(), station);
        }

        log.info("Loaded master data into cache: {} lines, {} stations",
                lineCache.size(), stationCache.size());

        return new MasterDataCaches(lineCache, stationCache);
    }

    private List<SubwayRidershipData> fetchAllDataFromApi(String yearMonth) {
        log.info("Starting to fetch subway statistics data from API: {}", yearMonth);

        List<SubwayRidershipData> allData = new ArrayList<>();
        int startIndex = 1;
        boolean hasMoreData = true;

        while (hasMoreData) {
            int endIndex = startIndex + pageSize - 1;
            SubwayApiResponse response = apiClient.fetchSubwayRidershipData(yearMonth, startIndex, endIndex);

            List<SubwayRidershipData> pageData = (response != null) ? response.getData() : null;

            if (pageData != null && !pageData.isEmpty()) {
                allData.addAll(pageData);
                log.info("Fetched subway statistics data: {} ~ {} ({} records in this page, {} total)",
                        startIndex, endIndex, pageData.size(), allData.size());
                startIndex = endIndex + 1;
            } else {
                hasMoreData = false;
            }
        }

        log.info("Completed fetching subway statistics data: {} API records", allData.size());
        return allData;
    }

    private Map<String, SubwayRidershipHourly> processRidershipData(
            List<SubwayRidershipData> apiDataList,
            MasterDataCaches caches
    ) {
        log.info("Starting to process {} API records", apiDataList.size());

        Map<String, SubwayRidershipHourly> hourlyDataMap = new HashMap<>();

        for (SubwayRidershipData data : apiDataList) {
            List<SubwayRidershipHourly> hourlyDataList = convertToHourlyRidership(data, caches);

            for (SubwayRidershipHourly hourly : hourlyDataList) {
                String key = generateUniqueKey(hourly);
                hourlyDataMap.put(key, hourly);
            }
        }

        log.info("Completed processing: {} API records -> {} unique hourly records",
                apiDataList.size(), hourlyDataMap.size());

        return hourlyDataMap;
    }

    private List<SubwayRidershipHourly> convertToHourlyRidership(
            SubwayRidershipData data,
            MasterDataCaches caches
    ) {
        SubwayLine line = caches.lineCache().get(data.getSbwyRoutLnNm());
        if (line == null) {
            throw new MasterDataNotFoundException("line", data.getSbwyRoutLnNm());
        }

        SubwayStation station = caches.stationCache().get(data.getSttn());
        if (station == null) {
            throw new MasterDataNotFoundException("station", data.getSttn());
        }

        return mapper.toSubwayRidershipHourlyList(data, line, station);
    }

    private String generateUniqueKey(SubwayRidershipHourly hourly) {
        return String.format("%s-%s-%s-%d",
                hourly.getStatDate(),
                hourly.getSubwayLine().getLineName(),
                hourly.getSubwayStation().getStationName(),
                hourly.getHourSlot());
    }

    private int saveRidershipData(Map<String, SubwayRidershipHourly> hourlyDataMap, int apiRecordCount) {
        List<SubwayRidershipHourly> uniqueHourlyData = new ArrayList<>(hourlyDataMap.values());
        subwayRidershipRepository.saveAll(uniqueHourlyData);

        int totalCount = uniqueHourlyData.size();
        log.info("Subway statistics data loading completed: {} API records -> {} unique hourly records",
                apiRecordCount, totalCount);
        return totalCount;
    }

    private record MasterDataCaches(
            Map<String, SubwayLine> lineCache,
            Map<String, SubwayStation> stationCache
    ) {}
}

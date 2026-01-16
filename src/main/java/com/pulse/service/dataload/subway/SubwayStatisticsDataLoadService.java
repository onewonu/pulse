package com.pulse.service.dataload.subway;

import com.pulse.api.seoulopendata.SeoulOpenDataClient;
import com.pulse.api.seoulopendata.dto.subway.SubwayApiResponse;
import com.pulse.api.seoulopendata.dto.subway.SubwayPassengerData;
import com.pulse.config.SeoulApiProperties;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayPassengerHourly;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.exception.dataload.MasterDataNotFoundException;
import com.pulse.mapper.SubwayDataMapper;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayPassengerHourlyRepository;
import com.pulse.repository.subway.SubwayStationRepository;
import com.pulse.util.LineNameNormalizer;
import com.pulse.util.StationNameNormalizer;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class SubwayStatisticsDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(SubwayStatisticsDataLoadService.class);

    private final EntityManager entityManager;
    private final SeoulOpenDataClient apiClient;
    private final SubwayDataMapper mapper;
    private final SubwayLineRepository subwayLineRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final SubwayPassengerHourlyRepository subwayPassengerRepository;
    private final SeoulApiProperties properties;

    public SubwayStatisticsDataLoadService(
            EntityManager entityManager,
            SeoulOpenDataClient apiClient,
            SubwayDataMapper mapper,
            SubwayLineRepository subwayLineRepository,
            SubwayStationRepository subwayStationRepository,
            SubwayPassengerHourlyRepository subwayPassengerRepository,
            SeoulApiProperties properties
    ) {
        this.entityManager = entityManager;
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.subwayLineRepository = subwayLineRepository;
        this.subwayStationRepository = subwayStationRepository;
        this.subwayPassengerRepository = subwayPassengerRepository;
        this.properties = properties;
    }

    public DataLoadResult deleteStatisticsByYearMonth(String yearMonth) {
        String operationId = UUID.randomUUID().toString().substring(0, 8);

        log.info("[{}] Start deleting subway statistics data: {}", operationId, yearMonth);

        int deletedCount = subwayPassengerRepository.deleteByYearMonth(yearMonth);

        log.info("[{}] Deleted {} records for {}", operationId, deletedCount, yearMonth);

        return DataLoadResult.success("Subway statistics deleted", deletedCount);
    }

    public DataLoadResult loadSubwayStatisticsData(String yearMonth) {
        String operationId = UUID.randomUUID().toString().substring(0, 8);

        log.info("[{}] Start loading subway statistics data: {}", operationId, yearMonth);

        deleteSameYearAndMonth(yearMonth, operationId);

        MasterDataCaches caches = loadMasterDataCaches(operationId);

        List<SubwayPassengerData> apiDataList = fetchAllDataFromApi(yearMonth, operationId);

        Map<String, SubwayPassengerHourly> hourlyDataMap = processPassengerData(apiDataList, caches, operationId);

        int totalCount = savePassengerData(hourlyDataMap, apiDataList.size(), operationId);
        return DataLoadResult.success("Subway statistics data", totalCount);
    }

    private void deleteSameYearAndMonth(String yearMonth, String operationId) {
        LocalDate statDate = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyyMM")).atDay(1);
        subwayPassengerRepository.deleteByStatDate(statDate);

        log.info("[{}] Existing subway statistics data has been deleted: {}", operationId, yearMonth);
    }

    private MasterDataCaches loadMasterDataCaches(String operationId) {
        Map<String, SubwayLine> lineCache = new HashMap<>();
        for (SubwayLine line : subwayLineRepository.findAll()) {
            lineCache.put(line.getLineName(), line);
        }

        Map<String, SubwayStation> stationCache = new HashMap<>();
        for (SubwayStation station : subwayStationRepository.findAll()) {
            stationCache.put(station.getStationName(), station);
        }

        log.info(
                "[{}] Loaded master data into cache: {} lines, {} stations",
                operationId,
                lineCache.size(),
                stationCache.size()
        );

        return new MasterDataCaches(lineCache, stationCache);
    }

    private List<SubwayPassengerData> fetchAllDataFromApi(String yearMonth, String operationId) {
        log.info("[{}] Starting to fetch subway statistics data from API: {}", operationId, yearMonth);

        List<SubwayPassengerData> allData = new ArrayList<>();
        int startIndex = 1;
        int pageNumber = 0;
        boolean hasMoreData = true;

        while (hasMoreData) {
            pageNumber++;
            int endIndex = startIndex + properties.getPageSize() - 1;
            SubwayApiResponse response = apiClient.fetchSubwayPassengerData(yearMonth, startIndex, endIndex);

            List<SubwayPassengerData> pageData = (response != null) ? response.getData() : null;

            if (pageData != null && !pageData.isEmpty()) {
                allData.addAll(pageData);

                log.info(
                        "[{}] Fetched page {} ({} records in this page, {} total)",
                        operationId,
                        pageNumber,
                        pageData.size(),
                        allData.size()
                );

                startIndex = endIndex + 1;
            } else {
                hasMoreData = false;
            }
        }

        log.info(
                "[{}] Completed fetching subway statistics data: {} API records from {} pages",
                operationId,
                allData.size(),
                pageNumber
        );

        return allData;
    }

    private Map<String, SubwayPassengerHourly> processPassengerData(
            List<SubwayPassengerData> apiDataList,
            MasterDataCaches caches,
            String operationId
    ) {
        log.info("[{}] Starting to process {} API records", operationId, apiDataList.size());

        Map<String, SubwayPassengerHourly> hourlyDataMap = new HashMap<>();

        for (SubwayPassengerData data : apiDataList) {
            List<SubwayPassengerHourly> hourlyDataList = convertToHourlyPassenger(data, caches);

            for (SubwayPassengerHourly hourly : hourlyDataList) {
                String key = generateUniqueKey(hourly);
                hourlyDataMap.put(key, hourly);
            }
        }

        log.info(
                "[{}] Completed processing: {} API records -> {} unique hourly records",
                operationId,
                apiDataList.size(),
                hourlyDataMap.size()
        );

        return hourlyDataMap;
    }

    private List<SubwayPassengerHourly> convertToHourlyPassenger(
            SubwayPassengerData data,
            MasterDataCaches caches
    ) {
        String normalizedLineName = LineNameNormalizer.normalize(data.getSbwyRoutLnNm());
        SubwayLine line = caches.lineCache().get(normalizedLineName);
        if (line == null) {
            throw new MasterDataNotFoundException("line", data.getSbwyRoutLnNm());
        }

        String normalizedStationName = StationNameNormalizer.normalize(data.getSttn());
        SubwayStation station = caches.stationCache().get(normalizedStationName);
        if (station == null) {
            throw new MasterDataNotFoundException("station", data.getSttn());
        }

        return mapper.toSubwayPassengerHourlyList(data, line, station);
    }

    private String generateUniqueKey(SubwayPassengerHourly hourly) {
        return String.format("%s-%s-%s-%d",
                hourly.getStatDate(),
                hourly.getSubwayLine().getLineName(),
                hourly.getSubwayStation().getStationName(),
                hourly.getHourSlot());
    }

    private int savePassengerData(Map<String, SubwayPassengerHourly> hourlyDataMap, int apiRecordCount, String operationId) {
        List<SubwayPassengerHourly> uniqueHourlyData = new ArrayList<>(hourlyDataMap.values());
        subwayPassengerRepository.saveAll(uniqueHourlyData);
        entityManager.flush();

        int totalCount = uniqueHourlyData.size();

        log.info(
                "[{}] Subway statistics data loading completed: {} API records -> {} unique hourly records (saved and flushed)",
                operationId,
                apiRecordCount,
                totalCount
        );

        return totalCount;
    }

    private record MasterDataCaches(
            Map<String, SubwayLine> lineCache,
            Map<String, SubwayStation> stationCache
    ) {}
}

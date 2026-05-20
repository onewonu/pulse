package com.pulse.service.dataload.subway;

import com.pulse.api.seoulopendata.SeoulOpenDataClient;
import com.pulse.api.seoulopendata.dto.subway.SubwayApiResponse;
import com.pulse.api.seoulopendata.dto.subway.SubwayPassengerData;
import com.pulse.config.SeoulApiProperties;
import com.pulse.dto.dataload.DataLoadResponse;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayPassengerHourly;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.mapper.SubwayDataMapper;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayPassengerHourlyRepository;
import com.pulse.repository.subway.SubwayStationRepository;
import com.pulse.util.LineNameNormalizer;
import com.pulse.util.StationNameNormalizer;
import com.pulse.annotation.DataLoadOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SubwayStatisticsDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(SubwayStatisticsDataLoadService.class);

    private final SeoulOpenDataClient apiClient;
    private final SubwayDataMapper mapper;
    private final SubwayLineRepository subwayLineRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final SubwayPassengerHourlyRepository subwayPassengerRepository;
    private final SeoulApiProperties properties;

    public SubwayStatisticsDataLoadService(
            SeoulOpenDataClient apiClient,
            SubwayDataMapper mapper,
            SubwayLineRepository subwayLineRepository,
            SubwayStationRepository subwayStationRepository,
            SubwayPassengerHourlyRepository subwayPassengerRepository,
            SeoulApiProperties properties
    ) {
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.subwayLineRepository = subwayLineRepository;
        this.subwayStationRepository = subwayStationRepository;
        this.subwayPassengerRepository = subwayPassengerRepository;
        this.properties = properties;
    }

    @Transactional
    public DataLoadResponse deleteStatisticsByYearMonth(String yearMonth) {
        int deletedCount = subwayPassengerRepository.deleteByYearMonth(yearMonth);
        return DataLoadResponse.success("Subway statistics deleted", deletedCount);
    }

    @Transactional
    @DataLoadOperation
    public DataLoadResponse loadSubwayStatisticsData(String yearMonth) {
        MasterDataCaches caches = loadMasterDataCaches();
        List<SubwayPassengerData> apiDataList = fetchAllDataFromApi(yearMonth);
        Map<String, SubwayPassengerHourly> hourlyDataMap = processPassengerData(apiDataList, caches);
        int totalCount = savePassengerData(hourlyDataMap);

        return DataLoadResponse.success("Subway statistics data (" + yearMonth + ")", totalCount);
    }

    private MasterDataCaches loadMasterDataCaches() {
        Map<String, SubwayLine> lineCache = subwayLineRepository.findAll().stream()
                .collect(Collectors.toMap(SubwayLine::getLineName, Function.identity()));

        Map<String, SubwayStation> stationCache = subwayStationRepository.findAllWithLine().stream()
                .collect(Collectors.toMap(
                        station -> station.getStationName() + "|" + station.getSubwayLine().getLineName(),
                        Function.identity()
                ));

        log.info("Loaded master data into cache: {} lines, {} stations",
                lineCache.size(), stationCache.size());

        return new MasterDataCaches(lineCache, stationCache);
    }

    private List<SubwayPassengerData> fetchAllDataFromApi(String yearMonth) {
        List<SubwayPassengerData> allData = new ArrayList<>();
        int startIndex = 1;
        int pageNumber = 0;
        List<SubwayPassengerData> pageData = fetchPage(yearMonth, startIndex);

        while (!pageData.isEmpty()) {
            pageNumber++;
            allData.addAll(pageData);

            log.info("Fetched page {} ({} records, {} total)", pageNumber, pageData.size(), allData.size());

            startIndex += properties.getPageSize();
            pageData = fetchPage(yearMonth, startIndex);
        }

        log.info("Completed fetching: {} records from {} pages", allData.size(), pageNumber);
        return allData;
    }

    private List<SubwayPassengerData> fetchPage(String yearMonth, int startIndex) {
        int endIndex = startIndex + properties.getPageSize() - 1;
        SubwayApiResponse response = apiClient.fetchSubwayPassengerData(yearMonth, startIndex, endIndex);

        if (response == null) {
            log.warn("API returned null response at startIndex {}", startIndex);
            return List.of();
        }

        List<SubwayPassengerData> data = response.getData();
        return (data != null) ? data : List.of();
    }

    private Map<String, SubwayPassengerHourly> processPassengerData(
            List<SubwayPassengerData> apiDataList,
            MasterDataCaches caches
    ) {
        Map<String, SubwayPassengerHourly> hourlyDataMap = new HashMap<>();
        int lineSkippedCount = 0;
        int stationSkippedCount = 0;

        for (SubwayPassengerData data : apiDataList) {
            String normalizedLineName = LineNameNormalizer.normalize(data.getSbwyRoutLnNm());
            SubwayLine line = caches.lineCache().get(normalizedLineName);

            if (line == null) {
                log.debug("Skipping data for line not in master data: {}", data.getSbwyRoutLnNm());
                lineSkippedCount++;
            } else {
                String normalizedStationName = StationNameNormalizer.normalize(data.getSttn());
                SubwayStation station = caches.stationCache().get(normalizedStationName + "|" + normalizedLineName);
                if (station == null) {
                    log.warn("Skipping data for station not in master data: {} on {}",
                            data.getSttn(), data.getSbwyRoutLnNm());
                    stationSkippedCount++;
                } else {
                    for (SubwayPassengerHourly hourly : mapper.toSubwayPassengerHourlyList(data, station)) {
                        hourlyDataMap.put(generateUniqueKey(hourly), hourly);
                    }
                }
            }
        }

        log.info("Completed processing: {} API records -> {} unique hourly records (line skipped: {}, station skipped: {})",
                apiDataList.size(), hourlyDataMap.size(), lineSkippedCount, stationSkippedCount);

        return hourlyDataMap;
    }

    private String generateUniqueKey(SubwayPassengerHourly hourly) {
        return String.format("%s-%s-%s-%d",
                hourly.getStatDate(),
                hourly.getSubwayLine().getLineName(),
                hourly.getSubwayStation().getStationName(),
                hourly.getHourSlot());
    }

    private int savePassengerData(Map<String, SubwayPassengerHourly> hourlyDataMap) {
        subwayPassengerRepository.saveAll(hourlyDataMap.values());
        return hourlyDataMap.size();
    }

    private record MasterDataCaches(
            Map<String, SubwayLine> lineCache,
            Map<String, SubwayStation> stationCache
    ) {}
}

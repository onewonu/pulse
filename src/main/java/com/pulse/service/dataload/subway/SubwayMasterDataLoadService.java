package com.pulse.service.dataload.subway;

import com.pulse.api.seoulopendata.SeoulOpenDataClient;
import com.pulse.api.seoulopendata.dto.subway.SubwayApiResponse;
import com.pulse.api.seoulopendata.dto.subway.SubwayPassengerData;
import com.pulse.config.SeoulApiProperties;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.mapper.SubwayDataMapper;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayStationRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.UUID;

@Deprecated
@Service
@Transactional
public class SubwayMasterDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(SubwayMasterDataLoadService.class);

    private final EntityManager entityManager;
    private final SeoulOpenDataClient apiClient;
    private final SubwayDataMapper mapper;
    private final SubwayLineRepository subwayLineRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final SeoulApiProperties properties;

    public SubwayMasterDataLoadService(
            EntityManager entityManager,
            SeoulOpenDataClient apiClient,
            SubwayDataMapper mapper,
            SubwayLineRepository subwayLineRepository,
            SubwayStationRepository subwayStationRepository,
            SeoulApiProperties properties
    ) {
        this.entityManager = entityManager;
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.subwayLineRepository = subwayLineRepository;
        this.subwayStationRepository = subwayStationRepository;
        this.properties = properties;
    }

    public DataLoadResult loadSubwayMasterData(String yearMonth) {
        String operationId = UUID.randomUUID().toString().substring(0, 8);

        log.info("[{}] Start loading subway master data: {}", operationId, yearMonth);

        deleteAllExistingMasterData(operationId);

        List<SubwayPassengerData> apiDataList = fetchAllDataFromApi(yearMonth, operationId);

        MasterDataCollections collections = extractAndDeduplicateMasterData(apiDataList, operationId);

        saveLinesAndStations(collections, operationId);

        // saveLineStationAssociations(collections, operationId);

        int totalCount = apiDataList.size();

        log.info(
                "[{}] Subway master data loading completed: {} API records -> {} lines, {} stations",
                operationId,
                totalCount,
                collections.lines().size(),
                collections.stations().size()
        );

        return DataLoadResult.success("Subway master data", totalCount);
    }

    private void deleteAllExistingMasterData(String operationId) {
        // subwayLineStationRepository.deleteAll();
        subwayStationRepository.deleteAll();
        subwayLineRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        log.info("[{}] Existing subway master data has been deleted", operationId);
    }

    private List<SubwayPassengerData> fetchAllDataFromApi(String yearMonth, String operationId) {
        log.info("[{}] Starting to fetch subway master data from API: {}", operationId, yearMonth);

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

        log.info("[{}] Completed fetching subway master data: {} API records from {} pages",
                operationId,
                allData.size(),
                pageNumber
        );

        return allData;
    }

    private MasterDataCollections extractAndDeduplicateMasterData(List<SubwayPassengerData> apiDataList, String operationId) {
        log.info("[{}] Starting to extract and deduplicate master data from {} API records",
                operationId, apiDataList.size());

        Map<String, SubwayLine> lineMap = new HashMap<>();
        Map<String, SubwayStation> stationMap = new HashMap<>();

        for (SubwayPassengerData data : apiDataList) {
            SubwayLine line = mapper.toSubwayLine(data);
            lineMap.put(line.getLineName(), line);

            SubwayStation station = mapper.toSubwayStation(data);
            // stationMap.put(station.getStationName(), station);
        }

        List<SubwayLine> uniqueLines = new ArrayList<>(lineMap.values());
        List<SubwayStation> uniqueStations = new ArrayList<>(stationMap.values());

        log.info(
                "[{}] Extracted and deduplicated: {} unique lines, {} unique stations",
                operationId,
                uniqueLines.size(),
                uniqueStations.size()
        );

        return new MasterDataCollections(lineMap, stationMap, uniqueLines, uniqueStations);
    }

    private void saveLinesAndStations(MasterDataCollections collections, String operationId) {
        subwayLineRepository.saveAll(collections.lines());
        subwayStationRepository.saveAll(collections.stations());
        entityManager.flush();

        log.info(
                "[{}] Saved and flushed {} unique lines and {} unique stations",
                operationId,
                collections.lines().size(),
                collections.stations().size()
        );
    }

    // private void saveLineStationAssociations(MasterDataCollections collections, String operationId) { ... }

    private record MasterDataCollections(
            Map<String, SubwayLine> lineMap,
            Map<String, SubwayStation> stationMap,
            List<SubwayLine> lines,
            List<SubwayStation> stations
    ) {}
}

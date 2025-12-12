package com.pulse.service.dataload.subway;

import com.pulse.api.seoulopendataplaza.SeoulOpenDataPlazaClient;
import com.pulse.api.seoulopendataplaza.dto.subway.SubwayApiResponse;
import com.pulse.api.seoulopendataplaza.dto.subway.SubwayRidershipData;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayLineStation;
import com.pulse.entity.subway.SubwayLineStationId;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.mapper.SubwayDataMapper;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayLineStationRepository;
import com.pulse.repository.subway.SubwayStationRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class SubwayMasterDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(SubwayMasterDataLoadService.class);

    private final EntityManager entityManager;
    private final SeoulOpenDataPlazaClient apiClient;
    private final SubwayDataMapper mapper;
    private final SubwayLineRepository subwayLineRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final SubwayLineStationRepository subwayLineStationRepository;

    @Value("${seoul-api.page-size}")
    private int pageSize;

    public SubwayMasterDataLoadService(
            EntityManager entityManager,
            SeoulOpenDataPlazaClient apiClient,
            SubwayDataMapper mapper,
            SubwayLineRepository subwayLineRepository,
            SubwayStationRepository subwayStationRepository,
            SubwayLineStationRepository subwayLineStationRepository
    ) {
        this.entityManager = entityManager;
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.subwayLineRepository = subwayLineRepository;
        this.subwayStationRepository = subwayStationRepository;
        this.subwayLineStationRepository = subwayLineStationRepository;
    }

    public DataLoadResult loadSubwayMasterData(String yearMonth) {
        log.info("Start loading subway master data: {}", yearMonth);

        deleteAllExistingMasterData();

        List<SubwayRidershipData> apiDataList = fetchAllDataFromApi(yearMonth);

        MasterDataCollections collections = extractAndDeduplicateMasterData(apiDataList);

        saveLinesAndStations(collections);

        saveLineStationAssociations(collections);

        int totalCount = apiDataList.size();
        log.info("Subway master data loading completed: {} API records -> {} lines, {} stations, {} line-stations",
                totalCount, collections.lines().size(), collections.stations().size(),
                collections.lineStationIds().size());

        return DataLoadResult.success("Subway master data", totalCount);
    }

    private void deleteAllExistingMasterData() {
        subwayLineStationRepository.deleteAll();
        subwayLineRepository.deleteAll();
        subwayStationRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
        log.info("Existing subway master data has been deleted");
    }

    private List<SubwayRidershipData> fetchAllDataFromApi(String yearMonth) {
        log.info("Starting to fetch subway master data from API: {}", yearMonth);

        List<SubwayRidershipData> allData = new ArrayList<>();
        int startIndex = 1;

        while (true) {
            int endIndex = startIndex + pageSize - 1;
            SubwayApiResponse response = apiClient.fetchSubwayRidershipData(yearMonth, startIndex, endIndex);

            if (response == null) {
                break;
            }

            List<SubwayRidershipData> pageData = response.getData();
            if (pageData == null || pageData.isEmpty()) {
                break;
            }

            allData.addAll(pageData);

            log.info("Fetched subway master data: {} ~ {} ({} records in this page, {} total)",
                    startIndex, endIndex, pageData.size(), allData.size());

            startIndex = endIndex + 1;
        }

        log.info("Completed fetching subway master data: {} API records", allData.size());
        return allData;
    }

    private MasterDataCollections extractAndDeduplicateMasterData(List<SubwayRidershipData> apiDataList) {
        log.info("Starting to extract and deduplicate master data from {} API records", apiDataList.size());

        Map<String, SubwayLine> lineMap = new HashMap<>();
        Map<String, SubwayStation> stationMap = new HashMap<>();
        Set<SubwayLineStationId> lineStationSet = new HashSet<>();

        for (SubwayRidershipData data : apiDataList) {
            SubwayLine line = mapper.toSubwayLine(data);
            lineMap.put(line.getLineName(), line);

            SubwayStation station = mapper.toSubwayStation(data);
            stationMap.put(station.getStationName(), station);

            lineStationSet.add(SubwayLineStationId.of(line.getLineName(), station.getStationName()));
        }

        List<SubwayLine> uniqueLines = new ArrayList<>(lineMap.values());
        List<SubwayStation> uniqueStations = new ArrayList<>(stationMap.values());

        log.info("Extracted and deduplicated: {} unique lines, {} unique stations, {} line-station associations",
                uniqueLines.size(), uniqueStations.size(), lineStationSet.size());

        return new MasterDataCollections(lineMap, stationMap, uniqueLines, uniqueStations, lineStationSet);
    }

    private void saveLinesAndStations(MasterDataCollections collections) {
        subwayLineRepository.saveAll(collections.lines());
        subwayStationRepository.saveAll(collections.stations());
        entityManager.flush();
        log.info("Saved {} unique lines and {} unique stations",
                collections.lines().size(), collections.stations().size());
    }

    private void saveLineStationAssociations(MasterDataCollections collections) {
        List<SubwayLineStation> lineStations = new ArrayList<>();

        for (SubwayLineStationId id : collections.lineStationIds()) {
            SubwayLine line = collections.lineMap().get(id.getLineName());
            SubwayStation station = collections.stationMap().get(id.getStationName());
            lineStations.add(SubwayLineStation.of(line, station));
        }

        subwayLineStationRepository.saveAll(lineStations);
        log.info("Saved {} line-station associations", lineStations.size());
    }

    private record MasterDataCollections(
            Map<String, SubwayLine> lineMap,
            Map<String, SubwayStation> stationMap,
            List<SubwayLine> lines,
            List<SubwayStation> stations,
            Set<SubwayLineStationId> lineStationIds
    ) {}
}

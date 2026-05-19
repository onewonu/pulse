package com.pulse.service.dataload.subway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.config.MasterDataProperties;
import com.pulse.dto.dataload.DataLoadResponse;
import com.pulse.dto.masterdata.LinesData;
import com.pulse.dto.masterdata.StationMasterData;
import com.pulse.dto.masterdata.StationExportData;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.exception.dataload.MasterDataLoadException;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayStationRepository;
import com.pulse.util.StationNameNormalizer;
import jakarta.persistence.EntityManager;
import com.pulse.annotation.DataLoadOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubwayMasterDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(SubwayMasterDataLoadService.class);

    private final EntityManager entityManager;
    private final SubwayLineRepository subwayLineRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final MasterDataProperties masterDataProperties;

    public SubwayMasterDataLoadService(
            EntityManager entityManager,
            SubwayLineRepository subwayLineRepository,
            SubwayStationRepository subwayStationRepository,
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            MasterDataProperties masterDataProperties
    ) {
        this.entityManager = entityManager;
        this.subwayLineRepository = subwayLineRepository;
        this.subwayStationRepository = subwayStationRepository;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.masterDataProperties = masterDataProperties;
    }

    @DataLoadOperation
    public DataLoadResponse loadMasterDataFromJson() {
        log.info("Start loading subway master data: lines={}, stations={}",
                masterDataProperties.getLinesPath(),
                masterDataProperties.getStationsPath());

        deleteAllExistingMasterData();

        List<SubwayLine> lines = loadLinesFromJson();
        subwayLineRepository.saveAll(lines);
        entityManager.flush();

        Map<String, SubwayLine> lineCache = lines.stream()
                .collect(Collectors.toMap(SubwayLine::getLineName, Function.identity()));

        List<SubwayStation> stations = loadStationsFromJson(lineCache);
        subwayStationRepository.saveAll(stations);

        log.info("Subway master data loading completed: {} lines, {} stations (total: {})",
                lines.size(), stations.size(), lines.size() + stations.size());

        return DataLoadResponse.success("Subway master data", lines.size() + stations.size());
    }

    private void deleteAllExistingMasterData() {
        subwayStationRepository.deleteAllInBatch();
        subwayLineRepository.deleteAllInBatch();
        entityManager.clear();

        log.info("Existing subway master data has been deleted");
    }

    private List<SubwayLine> loadLinesFromJson() {
        try {
            Resource resource = resourceLoader.getResource(masterDataProperties.getLinesPath());
            InputStream inputStream = resource.getInputStream();
            LinesData linesData = objectMapper.readValue(inputStream, LinesData.class);

            List<SubwayLine> lines = linesData.lines().stream()
                    .map(lineInfo -> SubwayLine.of(lineInfo.lineName(), lineInfo.color()))
                    .toList();

            log.info("Loaded {} lines from JSON", lines.size());

            return lines;

        } catch (IOException e) {
            throw new MasterDataLoadException("Failed to load lines.json", e);
        }
    }

    private List<SubwayStation> loadStationsFromJson(Map<String, SubwayLine> lineCache) {
        try {
            Resource resource = resourceLoader.getResource(masterDataProperties.getStationsPath());
            InputStream inputStream = resource.getInputStream();
            StationExportData exportData = objectMapper.readValue(inputStream, StationExportData.class);

            List<SubwayStation> stations = exportData.stationSearchResults().stream()
                    .flatMap(searchResult -> searchResult.results().stream())
                    .map(stationData -> processStationData(stationData, lineCache))
                    .flatMap(Optional::stream)
                    .toList();

            log.info("Loaded {} stations from JSON", stations.size());

            return stations;

        } catch (IOException e) {
            throw new MasterDataLoadException("Failed to load stations.json", e);
        }
    }

    private Optional<SubwayStation> processStationData(StationMasterData stationData, Map<String, SubwayLine> lineCache) {
        String lineName = stationData.laneName();

        if (!lineCache.containsKey(lineName)) {
            log.warn("Line not found for station: {} (line: {})", stationData.stationName(), lineName);
            return Optional.empty();
        }

        SubwayLine line = lineCache.get(lineName);

        Double latitude = stationData.getLatitude();
        Double longitude = stationData.getLongitude();

        if (latitude == null || longitude == null) {
            log.warn("Invalid coordinates for station: {} (lat: {}, lng: {})",
                    stationData.stationName(), stationData.y(), stationData.x());

            return Optional.empty();
        }

        String normalizedStationName = StationNameNormalizer.normalize(stationData.stationName());

        SubwayStation station = SubwayStation.of(
                stationData.stationID(),
                normalizedStationName,
                line,
                latitude,
                longitude
        );

        return Optional.of(station);
    }
}

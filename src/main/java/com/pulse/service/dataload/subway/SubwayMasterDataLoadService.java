package com.pulse.service.dataload.subway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.dto.DataLoadResult;
import com.pulse.dto.masterdata.LinesData;
import com.pulse.dto.masterdata.StationMasterData;
import com.pulse.dto.masterdata.StationExportData;
import com.pulse.dto.masterdata.StationSearchResult;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.exception.dataload.MasterDataLoadException;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayStationRepository;
import com.pulse.util.StationNameNormalizer;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@Transactional
public class SubwayMasterDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(SubwayMasterDataLoadService.class);

    private final EntityManager entityManager;
    private final SubwayLineRepository subwayLineRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final ObjectMapper objectMapper;

    private static final String LINES_JSON_PATH = "data/lines.json";
    private static final String STATIONS_JSON_PATH = "data/stations.json";

    public SubwayMasterDataLoadService(
            EntityManager entityManager,
            SubwayLineRepository subwayLineRepository,
            SubwayStationRepository subwayStationRepository,
            ObjectMapper objectMapper
    ) {
        this.entityManager = entityManager;
        this.subwayLineRepository = subwayLineRepository;
        this.subwayStationRepository = subwayStationRepository;
        this.objectMapper = objectMapper;
    }

    public DataLoadResult loadMasterDataFromJson() {
        String operationId = UUID.randomUUID().toString().substring(0, 8);

        log.info("[{}] Start loading subway master data from JSON files", operationId);

        deleteAllExistingMasterData(operationId);

        List<SubwayLine> lines = loadLinesFromJson(operationId);
        subwayLineRepository.saveAll(lines);
        entityManager.flush();

        Set<String> validLineNames = new HashSet<>();
        for (SubwayLine line : lines) {
            validLineNames.add(line.getLineName());
        }

        log.info("[{}] Loaded and saved {} subway lines", operationId, lines.size());

        List<SubwayStation> stations = loadStationsFromJson(validLineNames, operationId);
        subwayStationRepository.saveAll(stations);
        entityManager.flush();

        log.info("[{}] Loaded and saved {} subway stations", operationId, stations.size());

        int totalCount = lines.size() + stations.size();

        log.info("[{}] Subway master data loading completed: {} lines, {} stations (total: {})",
                operationId, lines.size(), stations.size(), totalCount);

        return DataLoadResult.success("Subway master data", totalCount);
    }

    private void deleteAllExistingMasterData(String operationId) {
        subwayStationRepository.deleteAll();
        subwayLineRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        log.info("[{}] Existing subway master data has been deleted", operationId);
    }

    private List<SubwayLine> loadLinesFromJson(String operationId) {
        log.info("[{}] Loading lines from classpath: {}", operationId, LINES_JSON_PATH);

        try {
            ClassPathResource resource = new ClassPathResource(LINES_JSON_PATH);
            InputStream inputStream = resource.getInputStream();
            LinesData linesData = objectMapper.readValue(inputStream, LinesData.class);

            List<SubwayLine> lines = new ArrayList<>();

            for (LinesData.LineInfo lineInfo : linesData.getLines()) {
                SubwayLine line = SubwayLine.of(lineInfo.getLineName());
                lines.add(line);
            }

            log.info("[{}] Loaded {} lines from JSON", operationId, lines.size());
            return lines;

        } catch (IOException e) {
            throw new MasterDataLoadException("Failed to load lines.json", e);
        }
    }

    private List<SubwayStation> loadStationsFromJson(Set<String> validLineNames, String operationId) {
        log.info("[{}] Loading stations from classpath: {}", operationId, STATIONS_JSON_PATH);

        try {
            ClassPathResource resource = new ClassPathResource(STATIONS_JSON_PATH);
            InputStream inputStream = resource.getInputStream();
            StationExportData exportData = objectMapper.readValue(inputStream, StationExportData.class);

            List<SubwayStation> stations = new ArrayList<>();
            int filteredOutCount = 0;

            for (StationSearchResult searchResult : exportData.getStationSearchResults()) {
                for (StationMasterData stationData : searchResult.getResults()) {
                    Optional<SubwayStation> stationOptional = processStationData(stationData, validLineNames, operationId);

                    if (stationOptional.isPresent()) {
                        stations.add(stationOptional.get());
                    } else {
                        filteredOutCount++;
                    }
                }
            }

            log.info("[{}] Loaded {} stations from JSON (filtered out {} stations not in valid lines)",
                    operationId, stations.size(), filteredOutCount);

            return stations;

        } catch (IOException e) {
            throw new MasterDataLoadException("Failed to load stations.json", e);
        }
    }

    private Optional<SubwayStation> processStationData(
            StationMasterData stationData,
            Set<String> validLineNames,
            String operationId
    ) {
        String lineName = stationData.getLaneName();

        if (!validLineNames.contains(lineName)) {
            return Optional.empty();
        }

        SubwayLine line = subwayLineRepository.findById(lineName).orElse(null);
        if (line == null) {
            log.warn("[{}] Line not found for station: {} (line: {})",
                    operationId, stationData.getStationName(), lineName);
            return Optional.empty();
        }

        Double latitude = stationData.getLatitude();
        Double longitude = stationData.getLongitude();

        if (latitude == null || longitude == null) {
            log.warn("[{}] Invalid coordinates for station: {} (lat: {}, lng: {})",
                    operationId, stationData.getStationName(),
                    stationData.getY(), stationData.getX());
            return Optional.empty();
        }

        String normalizedStationName = StationNameNormalizer.normalize(stationData.getStationName());

        SubwayStation station = SubwayStation.of(
                stationData.getStationID(),
                normalizedStationName,
                line,
                latitude,
                longitude
        );

        return Optional.of(station);
    }
}

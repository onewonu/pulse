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
import com.pulse.repository.subway.SubwayRidershipHourlyRepository;
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
            SubwayLineStationRepository subwayLineStationRepository,
            SubwayRidershipHourlyRepository subwayRidershipRepository
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

        try {
            subwayLineStationRepository.deleteAll();
            subwayLineRepository.deleteAll();
            subwayStationRepository.deleteAll();
            entityManager.flush();
            entityManager.clear();
            log.info("Existing subway master data has been deleted");

            Map<String, SubwayLine> lineMap = new HashMap<>();
            Map<String, SubwayStation> stationMap = new HashMap<>();
            Set<SubwayLineStationId> lineStationSet = new HashSet<>();
            int apiRecordCount = 0;
            int startIndex = 1;

            while (true) {
                int endIndex = startIndex + pageSize - 1;
                SubwayApiResponse response = apiClient.fetchSubwayRidershipData(yearMonth, startIndex, endIndex);

                if (response == null) {
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
            entityManager.flush();
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
}

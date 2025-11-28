package com.pulse.service.dataload;

import com.pulse.client.transport.SeoulOpenApiClient;
import com.pulse.client.transport.dto.subway.SubwayApiResponse;
import com.pulse.client.transport.dto.subway.SubwayRidershipData;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.subway.*;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayLineStationRepository;
import com.pulse.repository.subway.SubwayRidershipHourlyRepository;
import com.pulse.repository.subway.SubwayStationRepository;
import com.pulse.mapper.SubwayDataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
            int totalCount = 0;
            int startIndex = 1;

            while (true) {
                int endIndex = startIndex + pageSize - 1;
                SubwayApiResponse response = apiClient.fetchSubwayRidershipData(yearMonth, startIndex, endIndex);

                if (response == null || response.getData() == null || response.getData().isEmpty()) {
                    log.info("SubWay master data is empty.");
                    break;
                }

                for (SubwayRidershipData data : response.getData()) {
                    SubwayLine line = subwayLineRepository
                            .findByLineName(data.getSbwyRoutLnNm())
                            .orElseGet(() -> subwayLineRepository.save(mapper.toSubwayLine(data)));

                    SubwayStation station = subwayStationRepository
                            .findByStationName(data.getSttn())
                            .orElseGet(() -> subwayStationRepository.save(mapper.toSubwayStation(data)));

                    saveSubwayLineStationIfNotExists(line, station);

                    totalCount++;
                }

                log.info("Subway master data progress: {} ~ {} (total - {})", startIndex, endIndex, totalCount);

                startIndex = endIndex + 1;
            }

            log.info("Subway master data loading completed: total - {}", totalCount);

            return DataLoadResult.success("Subway master data", totalCount);

        } catch (Exception e) {
            log.error("Subway master data load failure", e);

            return DataLoadResult.failure("Subway master data", e.getMessage());
        }
    }

    private void saveSubwayLineStationIfNotExists(SubwayLine line, SubwayStation station) {
        SubwayLineStationId id = SubwayLineStationId.of(line.getLineName(), station.getStationName());

        if (!subwayLineStationRepository.existsById(id)) {
            subwayLineStationRepository.save(SubwayLineStation.of(line, station));
        }
    }

    public DataLoadResult loadSubwayStatisticsData(String yearMonth) {
        log.info("Start loading subway statistical Data: {}", yearMonth);

        try {
            LocalDate statDate = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyyMM")).atDay(1);
            subwayRidershipRepository.deleteByStatDate(statDate);
            log.info("Existing subway statistical data has been deleted: {}", yearMonth);

            int totalCount = 0;
            int startIndex = 1;

            while (true) {
                int endIndex = startIndex + pageSize - 1;
                SubwayApiResponse response = apiClient.fetchSubwayRidershipData(yearMonth, startIndex, endIndex);

                if (response == null || response.getData() == null || response.getData().isEmpty()) {
                    log.info("Subway statistical data is empty.");
                    break;
                }

                for (SubwayRidershipData data : response.getData()) {
                    SubwayLine line = subwayLineRepository
                            .findByLineName(data.getSbwyRoutLnNm())
                            .orElseThrow(() -> new IllegalStateException(
                                    "No route master data: " + data.getSbwyRoutLnNm()));

                    SubwayStation station = subwayStationRepository
                            .findByStationName(data.getSttn())
                            .orElseThrow(() -> new IllegalStateException(
                                    "No station master data: " + data.getSttn()));

                    List<SubwayRidershipHourly> hourlyData = mapper.toSubwayRidershipHourlyList(data, line, station);
                    subwayRidershipRepository.saveAll(hourlyData);

                    totalCount += 24;
                }

                log.info("Subway Statistical data progress: {} ~ {} (total - {})", startIndex, endIndex, totalCount);
                startIndex = endIndex + 1;
            }

            log.info("Subway statistical data loaded: total - {}", totalCount);
            return DataLoadResult.success("Subway statistical data", totalCount);

        } catch (Exception e) {
            log.error("Failure to load subway statistical data", e);
            return DataLoadResult.failure("Subway statistical data", e.getMessage());
        }
    }
}

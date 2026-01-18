package com.pulse.service.dataload.subway;

import com.pulse.api.seoulmetro.SeoulMetroClient;
import com.pulse.api.seoulmetro.dto.SeoulMetroTrainScheduleResponse;
import com.pulse.api.seoulmetro.dto.TrainScheduleItem;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.entity.subway.SubwayTrainSchedule;
import com.pulse.exception.dataload.ApiCommunicationException;
import com.pulse.exception.dataload.ApiResponseInvalidException;
import com.pulse.repository.subway.SubwayStationRepository;
import com.pulse.repository.subway.SubwayTrainScheduleRepository;
import com.pulse.util.StationNameNormalizer;
import com.pulse.mapper.TrainScheduleMapper;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.UUID;

@Service
@Transactional
public class TrainScheduleDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(TrainScheduleDataLoadService.class);
    private static final String REGULAR_SCHEDULE = "N";
    private static final String[] UPDOWN_TYPES = {"상행", "하행", "내선", "외선"};

    private final EntityManager entityManager;
    private final SeoulMetroClient apiClient;
    private final SubwayStationRepository stationRepository;
    private final SubwayTrainScheduleRepository scheduleRepository;
    private final TrainScheduleMapper mapper;

    public TrainScheduleDataLoadService(
            EntityManager entityManager,
            SeoulMetroClient apiClient,
            SubwayStationRepository stationRepository,
            SubwayTrainScheduleRepository scheduleRepository,
            TrainScheduleMapper mapper
    ) {
        this.entityManager = entityManager;
        this.apiClient = apiClient;
        this.stationRepository = stationRepository;
        this.scheduleRepository = scheduleRepository;
        this.mapper = mapper;
    }

    public DataLoadResult deleteAllTrainSchedules() {
        String operationId = UUID.randomUUID().toString().substring(0, 8);

        log.info("[{}] Starting to delete all train schedules", operationId);

        long count = scheduleRepository.count();
        scheduleRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        log.info("[{}] Deleted and flushed all train schedules: {} records", operationId, count);

        return DataLoadResult.success("All train schedules deleted", (int) count);
    }

    public DataLoadResult loadTrainSchedules(String dayType, String stationName, String lineName) {
        String operationId = UUID.randomUUID().toString().substring(0, 8);
        log.info(
                "[{}] Start loading train schedules for dayType: {}, station: {}, line: {}",
                operationId,
                dayType,
                stationName != null ? stationName : "ALL",
                lineName != null ? lineName : "ALL"
        );

        deleteExistingSchedules(dayType, operationId);

        Map<String, SubwayStation> stationCache = new ConcurrentHashMap<>();

        List<StationDirection> stationDirections = generateStationDirections(stationName, lineName, operationId);

        if (stationDirections.isEmpty()) {

            log.warn(
                    "[{}] No station-direction combinations found for station: {}, line: {}",
                    operationId,
                    stationName,
                    lineName
            );

            return DataLoadResult.failure(
                    "Train schedules",
                    "Station not found: " + stationName + " on line " + lineName
            );
        }

        log.info(
                "[{}] Found {} station-direction combinations, starting parallel API fetching",
                operationId,
                stationDirections.size()
        );

        List<SubwayTrainSchedule> allSchedules = fetchSchedulesFromApi(stationDirections, dayType, stationCache, operationId);

        Map<String, SubwayTrainSchedule> uniqueSchedulesMap = deduplicateSchedules(allSchedules, operationId);

        int totalCount = saveSchedulesToDatabase(uniqueSchedulesMap, operationId);

        String description = dayType;
        if (stationName != null) description += ", " + stationName;
        if (lineName != null) description += ", " + lineName;

        log.info("[{}] Train schedule loading completed: {} unique schedules saved", operationId, totalCount);

        return DataLoadResult.success("Train schedules (" + description + ")", totalCount);
    }

    private void deleteExistingSchedules(String dayType, String operationId) {
        scheduleRepository.deleteByDayType(dayType);
        entityManager.flush();
        entityManager.clear();

        log.info("[{}] Deleted existing schedules for dayType: {}", operationId, dayType);
    }

    private List<StationDirection> generateStationDirections(String targetStationName, String targetLineName, String operationId) {
        List<SubwayStation> stations = stationRepository.findAll();
        List<StationDirection> stationDirections = new ArrayList<>();

        for (SubwayStation station : stations) {
            String lineName = station.getSubwayLine().getLineName();
            String stationName = station.getStationName();

            if (
                    (targetStationName == null || stationName.startsWith(targetStationName)) &&
                            (targetLineName == null || lineName.equals(targetLineName))
            ) {
                String normalizedStationName = StationNameNormalizer.normalize(stationName);

                for (String updownType : UPDOWN_TYPES) {
                    stationDirections.add(new StationDirection(lineName, normalizedStationName, updownType));
                }
            }
        }

        log.info(
                "[{}] Generated {} station-direction combinations from {} stations",
                operationId,
                stationDirections.size(),
                stations.size()
        );

        return stationDirections;
    }

    private List<SubwayTrainSchedule> fetchSchedulesFromApi(
            List<StationDirection> stationDirections,
            String dayType,
            Map<String, SubwayStation> stationCache,
            String operationId
    ) {
        return stationDirections.parallelStream()
                .flatMap(sd -> fetchSchedulesForDirection(sd, dayType, stationCache, operationId))
                .toList();
    }

    private Stream<SubwayTrainSchedule> fetchSchedulesForDirection(
            StationDirection direction,
            String dayType,
            Map<String, SubwayStation> stationCache,
            String operationId
    ) {
        try {
            SeoulMetroTrainScheduleResponse response = apiClient.getTrainSchedule(
                    direction.lineName(),
                    direction.stationName(),
                    direction.updownType(),
                    dayType,
                    REGULAR_SCHEDULE
            );

            List<TrainScheduleItem> items = extractItemsFromResponse(response);
            return convertToScheduleEntities(items, direction, stationCache).stream();
        } catch (ApiCommunicationException | ApiResponseInvalidException e) {

            log.warn(
                    "[{}][Thread-{}] Failed to fetch schedule for line={}, station={}, direction={}: {}",
                    operationId,
                    Thread.currentThread().threadId(),
                    direction.lineName(), direction.stationName(), direction.updownType(),
                    e.getMessage()
            );

            return Stream.empty();
        }
    }

    private List<TrainScheduleItem> extractItemsFromResponse(SeoulMetroTrainScheduleResponse response) {
        SeoulMetroTrainScheduleResponse.Body body = response.getResponse().getBody();
        if (body == null || body.getItems() == null || body.getItems().getItem() == null) {
            return Collections.emptyList();
        }
        return body.getItems().getItem();
    }

    private List<SubwayTrainSchedule> convertToScheduleEntities(
            List<TrainScheduleItem> items,
            StationDirection direction,
            Map<String, SubwayStation> stationCache
    ) {
        List<SubwayTrainSchedule> schedules = new ArrayList<>();

        for (TrainScheduleItem item : items) {
            SubwayTrainSchedule schedule = mapper.toSubwayTrainSchedule(item, direction.lineName(), stationCache);
            if (schedule != null) {
                schedules.add(schedule);
            }
        }

        return schedules;
    }

    private Map<String, SubwayTrainSchedule> deduplicateSchedules(List<SubwayTrainSchedule> schedules, String operationId) {
        Map<String, SubwayTrainSchedule> uniqueMap = new LinkedHashMap<>();

        for (SubwayTrainSchedule schedule : schedules) {
            String key = generateScheduleKey(schedule);
            uniqueMap.putIfAbsent(key, schedule);
        }

        log.info(
                "[{}] Deduplicated schedules: {} fetched -> {} unique",
                operationId,
                schedules.size(),
                uniqueMap.size()
        );

        return uniqueMap;
    }

    private String generateScheduleKey(SubwayTrainSchedule schedule) {
        return String.format("%s|%s|%s|%s",
                schedule.getTrainNo(),
                schedule.getDepartureStation().getStationName(),
                schedule.getDepartureTime(),
                schedule.getDayType());
    }

    private int saveSchedulesToDatabase(Map<String, SubwayTrainSchedule> uniqueSchedulesMap, String operationId) {
        scheduleRepository.saveAll(uniqueSchedulesMap.values());
        entityManager.flush();

        log.info("[{}] Saved and flushed {} schedules to database", operationId, uniqueSchedulesMap.size());

        return uniqueSchedulesMap.size();
    }

    private record StationDirection(
            String lineName,
            String stationName,
            String updownType
    ) {}
}

package com.pulse.service.dataload.subway;

import com.pulse.api.seoulmetro.SeoulMetroClient;
import com.pulse.api.seoulmetro.dto.SeoulMetroTrainScheduleResponse;
import com.pulse.api.seoulmetro.dto.TrainScheduleItem;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.subway.SubwayLineStation;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.entity.subway.SubwayTrainSchedule;
import com.pulse.exception.dataload.ApiCommunicationException;
import com.pulse.exception.dataload.ApiResponseInvalidException;
import com.pulse.repository.subway.SubwayLineStationRepository;
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

@Service
@Transactional
public class TrainScheduleDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(TrainScheduleDataLoadService.class);
    private static final String REGULAR_SCHEDULE = "N";
    private static final String[] UPDOWN_TYPES = {"상행", "하행", "내선", "외선"};

    private final EntityManager entityManager;
    private final SeoulMetroClient apiClient;
    private final SubwayLineStationRepository lineStationRepository;
    private final SubwayTrainScheduleRepository scheduleRepository;
    private final TrainScheduleMapper mapper;

    public TrainScheduleDataLoadService(
            EntityManager entityManager,
            SeoulMetroClient apiClient,
            SubwayLineStationRepository lineStationRepository,
            SubwayTrainScheduleRepository scheduleRepository,
            TrainScheduleMapper mapper
    ) {
        this.entityManager = entityManager;
        this.apiClient = apiClient;
        this.lineStationRepository = lineStationRepository;
        this.scheduleRepository = scheduleRepository;
        this.mapper = mapper;
    }

    public DataLoadResult deleteAllTrainSchedules() {
        log.info("Starting to delete all train schedules");

        long count = scheduleRepository.count();
        scheduleRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        log.info("Deleted all train schedules: {} records", count);
        return DataLoadResult.success("All train schedules deleted", (int) count);
    }

    public DataLoadResult loadTrainSchedules(String dayType, String stationName, String lineName) {
        log.info(
                "Start loading train schedules for dayType: {}, station: {}, line: {}",
                dayType, stationName != null ? stationName : "ALL", lineName != null ? lineName : "ALL"
        );

        deleteExistingSchedules(dayType);

        Map<String, SubwayStation> stationCache = new ConcurrentHashMap<>();

        List<StationDirection> stationDirections = generateStationDirections(stationName, lineName);

        if (stationDirections.isEmpty()) {
            log.warn("No station-direction combinations found for station: {}, line: {}", stationName, lineName);
            return DataLoadResult.failure(
                    "Train schedules",
                    "Station not found: " + stationName + " on line " + lineName
            );
        }

        List<SubwayTrainSchedule> allSchedules = fetchSchedulesFromApi(stationDirections, dayType, stationCache);

        Map<String, SubwayTrainSchedule> uniqueSchedulesMap = deduplicateSchedules(allSchedules);

        int totalCount = saveSchedulesToDatabase(uniqueSchedulesMap);

        String description = dayType;
        if (stationName != null) description += ", " + stationName;
        if (lineName != null) description += ", " + lineName;

        return DataLoadResult.success("Train schedules (" + description + ")", totalCount);
    }

    private void deleteExistingSchedules(String dayType) {
        scheduleRepository.deleteByDayType(dayType);
        entityManager.flush();
        entityManager.clear();
    }

    private List<StationDirection> generateStationDirections(String targetStationName, String targetLineName) {
        List<SubwayLineStation> lineStations = lineStationRepository.findAll();
        List<StationDirection> stationDirections = new ArrayList<>();

        for (SubwayLineStation ls : lineStations) {
            String lineName = ls.getSubwayLine().getLineName();
            String stationName = ls.getSubwayStation().getStationName();

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

        return stationDirections;
    }

    private List<SubwayTrainSchedule> fetchSchedulesFromApi(
            List<StationDirection> stationDirections,
            String dayType,
            Map<String, SubwayStation> stationCache
    ) {
        return stationDirections.parallelStream()
                .flatMap(sd -> fetchSchedulesForDirection(sd, dayType, stationCache))
                .toList();
    }

    private Stream<SubwayTrainSchedule> fetchSchedulesForDirection(
            StationDirection direction,
            String dayType,
            Map<String, SubwayStation> stationCache
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

    private Map<String, SubwayTrainSchedule> deduplicateSchedules(List<SubwayTrainSchedule> schedules) {
        Map<String, SubwayTrainSchedule> uniqueMap = new LinkedHashMap<>();

        for (SubwayTrainSchedule schedule : schedules) {
            String key = generateScheduleKey(schedule);
            uniqueMap.putIfAbsent(key, schedule);
        }

        return uniqueMap;
    }

    private String generateScheduleKey(SubwayTrainSchedule schedule) {
        return String.format("%s|%s|%s|%s",
                schedule.getTrainNo(),
                schedule.getDepartureStation().getStationName(),
                schedule.getDepartureTime(),
                schedule.getDayType());
    }

    private int saveSchedulesToDatabase(Map<String, SubwayTrainSchedule> uniqueSchedulesMap) {
        scheduleRepository.saveAll(uniqueSchedulesMap.values());
        entityManager.flush();

        return uniqueSchedulesMap.size();
    }

    private record StationDirection(String lineName, String stationName, String updownType) {}
}

package com.pulse.service.dataload.subway;

import com.pulse.annotation.DataLoadOperation;
import com.pulse.api.seoulmetro.SeoulMetroClient;
import com.pulse.api.seoulmetro.dto.SeoulMetroTrainScheduleResponse;
import com.pulse.api.seoulmetro.dto.TrainScheduleItem;
import com.pulse.dto.dataload.DataLoadResponse;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.entity.subway.SubwayTrainSchedule;
import com.pulse.exception.dataload.ApiCommunicationException;
import com.pulse.exception.dataload.ApiResponseInvalidException;
import com.pulse.mapper.TrainScheduleMapper;
import com.pulse.repository.subway.SubwayStationRepository;
import com.pulse.repository.subway.SubwayTrainScheduleRepository;
import com.pulse.util.LineDirectionResolver;
import com.pulse.util.LineNameNormalizer;
import com.pulse.util.StationNameNormalizer;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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

    public DataLoadResponse deleteAllTrainSchedules() {
        long count = scheduleRepository.count();
        scheduleRepository.deleteAll();
        return DataLoadResponse.success("All train schedules deleted", (int) count);
    }

    @DataLoadOperation
    public DataLoadResponse loadTrainSchedules(String dayType) {
        deleteExistingSchedules(dayType);

        List<StationDirection> stationDirections = generateStationDirections();

        List<SubwayTrainSchedule> allSchedules = fetchSchedulesFromApi(
                stationDirections,
                dayType,
                new ConcurrentHashMap<>()
        );

        Map<String, SubwayTrainSchedule> uniqueSchedulesMap = deduplicateSchedules(allSchedules);
        int totalCount = saveSchedulesToDatabase(uniqueSchedulesMap);

        return DataLoadResponse.success("Train schedules (" + dayType + ")", totalCount);
    }

    private void deleteExistingSchedules(String dayType) {
        scheduleRepository.deleteByDayType(dayType);
        entityManager.flush();
        entityManager.clear();

        log.info("Deleted existing schedules for dayType: {}", dayType);
    }

    private List<StationDirection> generateStationDirections() {
        List<SubwayStation> stations = stationRepository.findAll();
        List<StationDirection> stationDirections = stations.stream()
                .flatMap(station -> {
                    String lineName = station.getSubwayLine().getLineName();
                    String stationName = station.getStationName();
                    String denormalizedLineName = LineNameNormalizer.denormalize(lineName);
                    String normalizedStationName = StationNameNormalizer.normalize(stationName);
                    String[] validDirections = LineDirectionResolver.getValidDirections(denormalizedLineName);

                    return Arrays.stream(validDirections)
                            .map(direction -> new StationDirection(
                                    denormalizedLineName,
                                    normalizedStationName,
                                    direction
                            ));
                })
                .toList();

        log.info("Generated {} station-direction combinations from {} stations",
                stationDirections.size(), stations.size());

        return stationDirections;
    }

    private List<SubwayTrainSchedule> fetchSchedulesFromApi(
            List<StationDirection> stationDirections,
            String dayType,
            Map<String, SubwayStation> stationCache
    ) {
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        return stationDirections.parallelStream()
                .flatMap(sd -> {
                    if (mdcContext != null) MDC.setContextMap(mdcContext);
                    return fetchSchedulesForDirection(sd, dayType, stationCache);
                })
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

            log.warn("Failed to fetch schedule for line={}, station={}, direction={}: {}",
                    direction.lineName(),
                    direction.stationName(),
                    direction.updownType(),
                    e.getMessage());

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
        return items.stream()
                .map(item -> mapper.toSubwayTrainSchedule(item, direction.lineName(), stationCache))
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<String, SubwayTrainSchedule> deduplicateSchedules(List<SubwayTrainSchedule> schedules) {
        Map<String, SubwayTrainSchedule> uniqueMap = new LinkedHashMap<>();

        for (SubwayTrainSchedule schedule : schedules) {
            String key = generateScheduleKey(schedule);
            uniqueMap.putIfAbsent(key, schedule);
        }

        log.info("Deduplicated schedules: {} fetched -> {} unique", schedules.size(), uniqueMap.size());

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

        log.info("Saved {} schedules to database", uniqueSchedulesMap.size());

        return uniqueSchedulesMap.size();
    }

    private record StationDirection(
            String lineName,
            String stationName,
            String updownType
    ) {}
}

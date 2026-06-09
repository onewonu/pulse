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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TrainScheduleDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(TrainScheduleDataLoadService.class);
    private static final String REGULAR_SCHEDULE = "N";

    private static final int BATCH_SIZE = 500;

    private final SeoulMetroClient apiClient;
    private final SubwayStationRepository stationRepository;
    private final SubwayTrainScheduleRepository scheduleRepository;
    private final SubwayTrainScheduleSaveService saveService;
    private final TrainScheduleMapper mapper;

    public TrainScheduleDataLoadService(
            SeoulMetroClient apiClient,
            SubwayStationRepository stationRepository,
            SubwayTrainScheduleRepository scheduleRepository,
            SubwayTrainScheduleSaveService saveService,
            TrainScheduleMapper mapper
    ) {
        this.apiClient = apiClient;
        this.stationRepository = stationRepository;
        this.scheduleRepository = scheduleRepository;
        this.saveService = saveService;
        this.mapper = mapper;
    }

    @Transactional
    public DataLoadResponse deleteAllTrainSchedules() {
        long count = scheduleRepository.count();
        scheduleRepository.deleteAll();
        return DataLoadResponse.success("All train schedules deleted", (int) count);
    }

    @DataLoadOperation
    public DataLoadResponse loadTrainSchedules(String dayType) {
        saveService.deleteByDayType(dayType);

        List<SubwayStation> stations = stationRepository.findAll();
        List<StationDirection> stationDirections = generateStationDirections(stations);
        Map<String, SubwayStation> stationCache = buildStationCache(stations);

        List<SubwayTrainSchedule> allSchedules = fetchSchedulesFromApi(
                stationDirections,
                dayType,
                stationCache
        );

        Map<String, SubwayTrainSchedule> uniqueSchedulesMap = deduplicateSchedules(allSchedules);
        int totalCount = saveSchedulesToDatabase(uniqueSchedulesMap);

        return DataLoadResponse.success("Train schedules (" + dayType + ")", totalCount);
    }

    private List<StationDirection> generateStationDirections(List<SubwayStation> stations) {
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

    private Map<String, SubwayStation> buildStationCache(List<SubwayStation> stations) {
        return stations.stream()
                .collect(Collectors.toMap(
                        station -> station.getStationName() + "|" + LineNameNormalizer.denormalize(
                                station.getSubwayLine().getLineName()
                        ),
                        Function.identity(),
                        (a, b) -> a,
                        ConcurrentHashMap::new
                ));
    }

    private List<SubwayTrainSchedule> fetchSchedulesFromApi(
            List<StationDirection> stationDirections,
            String dayType,
            Map<String, SubwayStation> stationCache
    ) {
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<SubwayTrainSchedule>>> futures = stationDirections.stream()
                    .map(sd -> executor.submit(() -> {
                        if (mdcContext != null) MDC.setContextMap(mdcContext);
                        return fetchSchedulesForDirection(sd, dayType, stationCache).toList();
                    }))
                    .toList();

            return futures.stream()
                    .flatMap(future -> {
                        try {
                            return future.get().stream();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return Stream.empty();
                        } catch (ExecutionException e) {
                            log.warn("Unexpected error fetching schedules: {}", e.getMessage());
                            return Stream.empty();
                        }
                    })
                    .toList();
        }
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
        List<SubwayTrainSchedule> schedules = new ArrayList<>(uniqueSchedulesMap.values());
        int savedCount = 0;

        for (int i = 0; i < schedules.size(); i += BATCH_SIZE) {
            List<SubwayTrainSchedule> batch = schedules.subList(i, Math.min(i + BATCH_SIZE, schedules.size()));

            try {
                savedCount += saveService.saveBatch(batch);
            } catch (Exception e) {
                log.warn("Failed to save batch [{}-{}]: {}", i, i + batch.size() - 1, e.getMessage());
            }
        }

        log.info("Saved {} / {} schedules to database", savedCount, uniqueSchedulesMap.size());
        return savedCount;
    }

    private record StationDirection(
            String lineName,
            String stationName,
            String updownType
    ) {}
}

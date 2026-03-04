package com.pulse.service.search;

import com.pulse.api.odsay.OdsayClient;
import com.pulse.api.odsay.dto.OdsaySubwayScheduleResponse;
import com.pulse.dto.CongestionLevel;
import com.pulse.dto.TimeRecommendationRequest;
import com.pulse.dto.TimeRecommendationResult;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayPassengerHourly;
import com.pulse.exception.search.IncompleteCongestionDataException;
import com.pulse.exception.search.NoSchedulesAvailableException;
import com.pulse.exception.search.OdsayApiException;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayPassengerHourlyRepository;
import com.pulse.repository.subway.SubwayTrainScheduleRepository;
import com.pulse.util.DayCodeConverter;
import com.pulse.util.LineNameNormalizer;
import com.pulse.util.StationNameNormalizer;
import com.pulse.util.TimeParser;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TimeRecommendationService {

    private static final int SHORTEST_TIME = 1;
    private static final int SUBWAY = 1;
    private static final int MAX_COUNT = 3;
    private static final DateTimeFormatter TIME_FORMAT_HHMM = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter TIME_FORMAT_HHMMSS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final SubwayTrainScheduleRepository subwayTrainScheduleRepository;
    private final SubwayPassengerHourlyRepository subwayPassengerHourlyRepository;
    private final SubwayLineRepository subwayLineRepository;
    private final OdsayClient odsayClient;

    public TimeRecommendationService(
            SubwayTrainScheduleRepository subwayTrainScheduleRepository,
            SubwayPassengerHourlyRepository subwayPassengerHourlyRepository,
            SubwayLineRepository subwayLineRepository,
            OdsayClient odsayClient
    ) {
        this.subwayTrainScheduleRepository = subwayTrainScheduleRepository;
        this.subwayPassengerHourlyRepository = subwayPassengerHourlyRepository;
        this.subwayLineRepository = subwayLineRepository;
        this.odsayClient = odsayClient;
    }

    public TimeRecommendationResult recommendTimes(TimeRecommendationRequest request) {
        DayInfo dayInfo = convertToDayInfo(request);

        RouteTemplate template;
        try {
            template = fetchRouteTemplate(request, dayInfo, request.startTime());
            if (template == null) {
                throw new IncompleteCongestionDataException("No route found between stations");
            }
        } catch (OdsayApiException e) {
            throw new IncompleteCongestionDataException("Failed to fetch route from Odsay API");
        }

        String actualDepartureStationId = extractActualDepartureStationId(template);

        List<LocalTime> departureTimes = getAvailableDepartureTimes(
                actualDepartureStationId,
                dayInfo,
                request.startTime(),
                request.endTime()
        );

        if (departureTimes.isEmpty()) {
            throw new NoSchedulesAvailableException("No train schedules found for the actual departure station");
        }

        List<DepartureTimeRecommendation> recommendations = generateRecommendationsFromTemplate(template, departureTimes);

        return buildResponse(request, dayInfo, recommendations, template);
    }

    private DayInfo convertToDayInfo(TimeRecommendationRequest request) {
        int dayCode = DayCodeConverter.convert(request.searchDate());
        String dayType = DayCodeConverter.toDayType(dayCode);
        return new DayInfo(dayCode, dayType);
    }

    private RouteTemplate fetchRouteTemplate(
            TimeRecommendationRequest request,
            DayInfo dayInfo,
            LocalTime representativeTime
    ) {
        OdsaySubwayScheduleResponse response = odsayClient.searchSubwaySchedule(
                request.departureStationId(),
                request.arrivalStationId(),
                dayInfo.dayCode(),
                representativeTime.format(TIME_FORMAT_HHMM)
        );

        OdsaySubwayScheduleResponse.PathData path = extractFastestPath(response);
        if (path == null) {
            return null;
        }

        OdsaySubwayScheduleResponse.InfoData info = path.getInfo();
        LocalTime referenceDepartureTime = TimeParser.parseHHmmss(info.getDepartureTime());
        LocalTime referenceArrivalTime = TimeParser.parseHHmmss(info.getArrivalTime());

        List<StationTemplate> stationTemplates = extractStationTemplates(
                path,
                referenceDepartureTime
        );

        return new RouteTemplate(
                stationTemplates,
                info.getTotalTime(),
                info.getTransferCount(),
                referenceDepartureTime,
                referenceArrivalTime
        );
    }

    private OdsaySubwayScheduleResponse.PathData extractFastestPath(OdsaySubwayScheduleResponse response) {
        List<OdsaySubwayScheduleResponse.PathData> paths = response.getResult().getPath();
        if (paths.isEmpty()) {
            return null;
        }

        return paths.stream()
                .filter(path -> path.getPathType() != null && path.getPathType() == SHORTEST_TIME)
                .findFirst()
                .orElse(paths.getFirst());
    }

    private List<StationTemplate> extractStationTemplates(
            OdsaySubwayScheduleResponse.PathData path,
            LocalTime referenceDepartureTime
    ) {
        if (path.getSubPath() == null) {
            return Collections.emptyList();
        }

        return path.getSubPath().stream()
                .filter(subPath -> subPath.getMovingType() == SUBWAY)
                .flatMap(subPath -> extractStationsFromSubPath(subPath, referenceDepartureTime))
                .toList();
    }

    private Stream<StationTemplate> extractStationsFromSubPath(
            OdsaySubwayScheduleResponse.SubPathData subPath,
            LocalTime referenceDepartureTime) {
        OdsaySubwayScheduleResponse.PassStopListData passStopList = subPath.getPassStopList();
        if (passStopList == null || passStopList.getStations() == null) {
            return Stream.empty();
        }

        String lineName = subPath.getLaneName();
        String lineColor = getLineColor(lineName);

        return passStopList.getStations().stream()
                .map(station -> createStationTemplate(station, lineName, lineColor, referenceDepartureTime));
    }

    private String getLineColor(String lineName) {
        if (lineName == null) {
            return null;
        }

        return Optional.of(lineName)
                .map(LineNameNormalizer::normalize)
                .flatMap(subwayLineRepository::findById)
                .map(SubwayLine::getColor)
                .orElse(null);
    }

    private StationTemplate createStationTemplate(
            OdsaySubwayScheduleResponse.StationInfoData station,
            String lineName,
            String lineColor,
            LocalTime referenceDepartureTime) {
        String stationId = station.getStationID() != null
                ? station.getStationID()
                : null;
        String normalizedName = StationNameNormalizer.normalize(station.getStationName());
        LocalTime arrivalTime = TimeParser.parseHHmmss(station.getArrivalTime());
        LocalTime departureTime = TimeParser.parseHHmmss(station.getDepartureTime());

        int minutesFromDepartureToArrival = 0;
        int minutesFromDepartureToDeparture = 0;

        if (arrivalTime != null && referenceDepartureTime != null) {
            minutesFromDepartureToArrival = (int) Duration
                    .between(referenceDepartureTime, arrivalTime)
                    .toMinutes();
        }

        if (departureTime != null && referenceDepartureTime != null) {
            minutesFromDepartureToDeparture = (int) Duration
                    .between(referenceDepartureTime, departureTime)
                    .toMinutes();
        }

        return new StationTemplate(
                stationId,
                normalizedName,
                lineName,
                lineColor,
                minutesFromDepartureToArrival,
                minutesFromDepartureToDeparture
        );
    }

    private String extractActualDepartureStationId(RouteTemplate template) {
        if (template.stations().isEmpty()) {
            throw new IncompleteCongestionDataException("Route template has no stations");
        }

        String stationId = template.stations().getFirst().stationId();
        if (stationId == null) {
            throw new IncompleteCongestionDataException("First station in route has no ID");
        }

        return stationId;
    }

    private List<LocalTime> getAvailableDepartureTimes(
            String departureStationId,
            DayInfo dayInfo,
            LocalTime startTime,
            LocalTime endTime
    ) {
        return subwayTrainScheduleRepository.findDistinctDepartureTimesByStationIdAndDayAndTimeRange(
                departureStationId,
                dayInfo.dayType(),
                startTime,
                endTime
        );
    }

    private List<DepartureTimeRecommendation> generateRecommendationsFromTemplate(
            RouteTemplate template,
            List<LocalTime> departureTimes
    ) {
        return departureTimes.stream()
                .map(departureTime -> generateRecommendationForDeparture(template, departureTime))
                .filter(Objects::nonNull)
                .toList();
    }

    private DepartureTimeRecommendation generateRecommendationForDeparture(
            RouteTemplate template,
            LocalTime departureTime
    ) {
        try {
            List<StationWithTime> stationsWithTime = calculateStationTimesForDeparture(
                    template,
                    departureTime
            );

            CongestionData congestionData = calculateCongestion(stationsWithTime);

            OdsaySubwayScheduleResponse.InfoData info = createRouteInfo(template, departureTime);

            OdsaySubwayScheduleResponse.PathData pathData = new OdsaySubwayScheduleResponse.PathData();
            pathData.setInfo(info);
            pathData.setPathType(SHORTEST_TIME);

            return new DepartureTimeRecommendation(pathData, stationsWithTime, congestionData);
        } catch (DataAccessException e) {
            return null;
        }
    }

    private List<StationWithTime> calculateStationTimesForDeparture(
            RouteTemplate template,
            LocalTime departureTime
    ) {
        return template.stations().stream()
                .map(stationTemplate -> {
                    LocalTime arrivalTime = departureTime.plusMinutes(stationTemplate.minutesFromDepartureToArrival());
                    LocalTime stationDepartureTime = departureTime.plusMinutes(stationTemplate.minutesFromDepartureToDeparture());

                    return new StationWithTime(
                            stationTemplate.stationId(),
                            stationTemplate.stationName(),
                            arrivalTime,
                            stationDepartureTime,
                            stationTemplate.lineName(),
                            stationTemplate.lineColor()
                    );
                })
                .toList();
    }

    private CongestionData calculateCongestion(List<StationWithTime> stations) {
        Map<String, StationWithTime> uniqueStations = deduplicateStations(stations);
        Map<Byte, List<String>> stationsByHour = groupStationIdsByHour(uniqueStations.values());
        List<SubwayPassengerHourly> passengers = fetchPassengerData(stationsByHour);

        Map<String, SubwayPassengerHourly> passengerMap = passengers.stream()
                .collect(Collectors.toMap(
                        passenger -> passenger.getSubwayStation().getStationId(),
                        passenger -> passenger,
                        (existing, replacement) -> existing
                ));

        int averageScore = calculateAverageScore(passengers);

        return new CongestionData(passengerMap, averageScore);
    }

    private Map<String, StationWithTime> deduplicateStations(List<StationWithTime> stations) {
        return stations.stream()
                .filter(station -> station.stationId != null)
                .collect(Collectors.toMap(
                        station -> station.stationId,
                        station -> station,
                        (existing, replacement) -> existing
                ));
    }

    private Map<Byte, List<String>> groupStationIdsByHour(Collection<StationWithTime> stations) {
        return stations.stream()
                .map(station -> {
                    if (station.arrivalTime == null || station.stationId == null) {
                        return null;
                    }
                    return Map.entry((byte) station.arrivalTime.getHour(), station.stationId);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }

    private List<SubwayPassengerHourly> fetchPassengerData(Map<Byte, List<String>> stationsByHour) {
        return stationsByHour.entrySet().stream()
                .flatMap(entry -> subwayPassengerHourlyRepository
                        .findByStationIdsAndHourSlot(entry.getValue(), entry.getKey())
                        .stream())
                .toList();
    }

    private int calculateAverageScore(List<SubwayPassengerHourly> passengers) {
        if (passengers.isEmpty()) {
            return 0;
        }

        int total = passengers.stream()
                .mapToInt(passenger -> passenger.getBoardingCount() + passenger.getAlightingCount())
                .sum();

        return total / passengers.size();
    }

    private OdsaySubwayScheduleResponse.InfoData createRouteInfo(
            RouteTemplate template,
            LocalTime departureTime
    ) {
        LocalTime arrivalTime = departureTime.plusMinutes(template.totalTime());

        OdsaySubwayScheduleResponse.InfoData info = new OdsaySubwayScheduleResponse.InfoData();
        info.setDepartureTime(departureTime.format(TIME_FORMAT_HHMMSS));
        info.setArrivalTime(arrivalTime.format(TIME_FORMAT_HHMMSS));
        info.setTotalTime(template.totalTime());
        info.setTransferCount(template.transferCount());

        return info;
    }

    private TimeRecommendationResult buildResponse(
            TimeRecommendationRequest request,
            DayInfo dayInfo,
            List<DepartureTimeRecommendation> recommendations,
            RouteTemplate template
    ) {
        if (recommendations.isEmpty()) {
            return buildEmptyResult(request, dayInfo, template);
        }

        List<DepartureTimeRecommendation> selected = selectDiverseRecommendations(recommendations);

        List<TimeRecommendationResult.TimeRecommendation> results = selected.stream()
                .map(this::toResultRecommendation)
                .toList();

        return new TimeRecommendationResult(
                request.departureStationId(),
                request.arrivalStationId(),
                getFirstStationName(template),
                getLastStationName(template),
                request.searchDate(),
                dayInfo.dayType(),
                results,
                null
        );
    }

    private TimeRecommendationResult buildEmptyResult(
            TimeRecommendationRequest request,
            DayInfo dayInfo,
            RouteTemplate template) {
        String departureStationName = getFirstStationName(template);
        String arrivalStationName = getLastStationName(template);

        return new TimeRecommendationResult(
                request.departureStationId(),
                request.arrivalStationId(),
                departureStationName,
                arrivalStationName,
                request.searchDate(),
                dayInfo.dayType(),
                Collections.emptyList(),
                "No congestion data found. Only route information is provided."
        );
    }

    private String getFirstStationName(RouteTemplate template) {
        return template.stations().isEmpty() ? null : template.stations().getFirst().stationName();
    }

    private String getLastStationName(RouteTemplate template) {
        return template.stations().isEmpty() ? null : template.stations().getLast().stationName();
    }

    private List<DepartureTimeRecommendation> selectDiverseRecommendations(List<DepartureTimeRecommendation> recommendations) {
        if (recommendations.isEmpty() || MAX_COUNT <= 0) {
            return Collections.emptyList();
        }

        List<DepartureTimeRecommendation> sorted = new ArrayList<>(recommendations);
        sorted.sort(Comparator.comparingInt(r -> r.congestion.averageScore));

        Map<CongestionLevel, List<DepartureTimeRecommendation>> levelGroups = groupByLevel(sorted);
        List<DepartureTimeRecommendation> firstFromEachLevel = selectFirstFromEachLevel(levelGroups);

        if (firstFromEachLevel.size() >= MAX_COUNT) {
            return firstFromEachLevel;
        }

        return fillRemainingSlots(firstFromEachLevel, sorted);
    }

    private Map<CongestionLevel, List<DepartureTimeRecommendation>> groupByLevel(
            List<DepartureTimeRecommendation> sortedRecommendations) {
        return sortedRecommendations.stream()
                .collect(Collectors.groupingBy(
                        rec -> CongestionLevel.fromScore(rec.congestion.averageScore),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private List<DepartureTimeRecommendation> selectFirstFromEachLevel(
            Map<CongestionLevel, List<DepartureTimeRecommendation>> levelGroups) {
        return Stream.of(CongestionLevel.values())
                .map(levelGroups::get)
                .filter(list -> list != null && !list.isEmpty())
                .map(List::getFirst)
                .limit(MAX_COUNT)
                .toList();
    }

    private List<DepartureTimeRecommendation> fillRemainingSlots(
            List<DepartureTimeRecommendation> selected,
            List<DepartureTimeRecommendation> sortedRecommendations) {
        Set<DepartureTimeRecommendation> selectedSet = new HashSet<>(selected);

        List<DepartureTimeRecommendation> remaining = sortedRecommendations.stream()
                .filter(rec -> !selectedSet.contains(rec))
                .limit((long) MAX_COUNT - selected.size())
                .toList();

        return Stream.concat(selected.stream(), remaining.stream()).toList();
    }

    private TimeRecommendationResult.TimeRecommendation toResultRecommendation(DepartureTimeRecommendation recommendation) {
        OdsaySubwayScheduleResponse.InfoData info = recommendation.pathInfo.getInfo();

        LocalTime departureTime = TimeParser.parseHHmmss(info.getDepartureTime());
        LocalTime arrivalTime = TimeParser.parseHHmmss(info.getArrivalTime());

        CongestionLevel congestionLevel = CongestionLevel.fromScore(recommendation.congestion.averageScore);

        List<TimeRecommendationResult.StationCongestion> stationCongestions = recommendation.stations.stream()
                .map(station -> {
                    SubwayPassengerHourly passenger = recommendation.congestion.passengerMap.get(station.stationId);
                    return new TimeRecommendationResult.StationCongestion(
                            station.stationId,
                            station.stationName,
                            station.lineName,
                            station.lineColor,
                            station.arrivalTime,
                            station.departureTime,
                            getBoardingCount(passenger),
                            getAlightingCount(passenger),
                            getTotalPassengers(passenger)
                    );
                })
                .toList();

        return new TimeRecommendationResult.TimeRecommendation(
                departureTime,
                arrivalTime,
                info.getTotalTime(),
                info.getTransferCount(),
                recommendation.congestion.averageScore,
                congestionLevel,
                stationCongestions
        );
    }

    private Integer getBoardingCount(SubwayPassengerHourly passenger) {
        return passenger != null ? passenger.getBoardingCount() : null;
    }

    private Integer getAlightingCount(SubwayPassengerHourly passenger) {
        return passenger != null ? passenger.getAlightingCount() : null;
    }

    private Integer getTotalPassengers(SubwayPassengerHourly passenger) {
        return passenger != null ? (passenger.getBoardingCount() + passenger.getAlightingCount()) : null;
    }

    private record RouteTemplate(
            List<StationTemplate> stations,
            int totalTime,
            int transferCount,
            LocalTime referenceDepartureTime,
            LocalTime referenceArrivalTime
    ) {}

    private record StationTemplate(
            String stationId,
            String stationName,
            String lineName,
            String lineColor,
            int minutesFromDepartureToArrival,
            int minutesFromDepartureToDeparture
    ) {}

    private record DayInfo(
            int dayCode,
            String dayType
    ) {}

    private record StationWithTime(
            String stationId,
            String stationName,
            LocalTime arrivalTime,
            LocalTime departureTime,
            String lineName,
            String lineColor
    ) {}

    private record DepartureTimeRecommendation(
            OdsaySubwayScheduleResponse.PathData pathInfo,
            List<StationWithTime> stations,
            CongestionData congestion
    ) {}

    private record CongestionData(
            Map<String, SubwayPassengerHourly> passengerMap,
            int averageScore
    ) {}
}

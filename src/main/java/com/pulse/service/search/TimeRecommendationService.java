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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TimeRecommendationService {

    private static final int SHORTEST_TIME = 1;
    private static final int SUBWAY = 1;

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

        List<LocalTime> departureTimes = getAvailableDepartureTimes(request, dayInfo);

        if (departureTimes.isEmpty()) {
            throw new NoSchedulesAvailableException("No train schedules found");
        }

        List<RouteWithCongestion> routes = processAllDepartureTimes(request, dayInfo, departureTimes);

        if (routes.isEmpty()) {
            throw new IncompleteCongestionDataException("Congestion data incomplete for all routes");
        }

        return buildResponse(request, dayInfo, routes);
    }

    private DayInfo convertToDayInfo(TimeRecommendationRequest request) {
        int dayCode = DayCodeConverter.convert(request.searchDate());
        String dayType = DayCodeConverter.toDayType(dayCode);
        return new DayInfo(dayCode, dayType);
    }

    private List<LocalTime> getAvailableDepartureTimes(TimeRecommendationRequest request, DayInfo dayInfo) {
        return subwayTrainScheduleRepository.findDistinctDepartureTimesByStationIdAndDayAndTimeRange(
                request.departureStationId().toString(),
                dayInfo.dayType(),
                request.startTime(),
                request.endTime()
        );
    }

    private List<RouteWithCongestion> processAllDepartureTimes(
            TimeRecommendationRequest request,
            DayInfo dayInfo,
            List<LocalTime> departureTimes
    ) {
        return departureTimes.stream()
                .map(departureTime -> processSingleDepartureTime(request, dayInfo, departureTime))
                .filter(Objects::nonNull)
                .toList();
    }

    private RouteWithCongestion processSingleDepartureTime(
            TimeRecommendationRequest request,
            DayInfo dayInfo,
            LocalTime departureTime
    ) {
        try {
            OdsaySubwayScheduleResponse response = odsayClient.searchSubwaySchedule(
                    request.departureStationId(),
                    request.arrivalStationId(),
                    dayInfo.dayCode(),
                    departureTime.format(DateTimeFormatter.ofPattern("HHmm"))
            );

            OdsaySubwayScheduleResponse.PathData path = extractFastestPath(response);
            if (path == null) {
                return null;
            }

            List<StationWithTime> stationsWithTime = extractStationsWithTime(path);
            CongestionData congestionData = calculateCongestion(stationsWithTime);

            return new RouteWithCongestion(path, stationsWithTime, congestionData);
        } catch (OdsayApiException | DataAccessException e) {
            return null;
        }
    }

    private TimeRecommendationResult buildResponse(
            TimeRecommendationRequest request,
            DayInfo dayInfo,
            List<RouteWithCongestion> routes
    ) {
        Map<String, List<RouteWithCongestion>> routesByPath = routes.stream()
                .collect(Collectors.groupingBy(this::getRoutePathKey));

        List<RouteWithCongestion> mostCommonPathRoutes = routesByPath.values().stream()
                .max(Comparator
                        .comparingInt(List<RouteWithCongestion>::size)
                        .thenComparing(Comparator.comparingDouble(
                                (List<RouteWithCongestion> routeList) -> routeList.stream()
                                        .mapToDouble(r -> r.congestionData.averageScore)
                                        .average()
                                        .orElse(Double.MAX_VALUE)
                        ).reversed()))
                .orElse(Collections.emptyList());

        List<RouteWithCongestion> sortedRoutes = new ArrayList<>(mostCommonPathRoutes);
        sortedRoutes.sort(Comparator.comparingDouble(r -> r.congestionData.averageScore));

        List<TimeRecommendationResult.TimeRecommendation> recommendations = sortedRoutes.stream()
                .limit(3)
                .map(this::mapToRecommendation)
                .toList();

        String departureStationName = extractStationName(mostCommonPathRoutes, true);
        String arrivalStationName = extractStationName(mostCommonPathRoutes, false);

        return new TimeRecommendationResult(
                request.departureStationId(),
                request.arrivalStationId(),
                departureStationName,
                arrivalStationName,
                request.searchDate(),
                dayInfo.dayType(),
                recommendations,
                null
        );
    }

    private String getRoutePathKey(RouteWithCongestion route) {
        if (route.stationsWithTime == null) {
            return "";
        }
        return route.stationsWithTime.stream()
                .map(station -> station.stationId)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("-"));
    }

    private String extractStationName(List<RouteWithCongestion> routes, boolean isDeparture) {
        if (routes.isEmpty()) {
            return null;
        }

        List<StationWithTime> stations = routes.getFirst().stationsWithTime;
        if (stations.isEmpty()) {
            return null;
        }

        return isDeparture ? stations.getFirst().stationName : stations.getLast().stationName;
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

    private List<StationWithTime> extractStationsWithTime(OdsaySubwayScheduleResponse.PathData path) {
        if (path.getSubPath() == null) {
            return Collections.emptyList();
        }

        return path.getSubPath().stream()
                .filter(subPath -> subPath.getMovingType() == SUBWAY)
                .flatMap(subPath -> {
                    OdsaySubwayScheduleResponse.PassStopListData passStopList = subPath.getPassStopList();
                    String lineName = subPath.getLaneName();

                    String lineColor = Optional.ofNullable(lineName)
                            .map(LineNameNormalizer::normalize)
                            .flatMap(subwayLineRepository::findById)
                            .map(SubwayLine::getColor)
                            .orElse(null);

                    if (passStopList == null || passStopList.getStations() == null) {
                        return Stream.empty();
                    }

                    return passStopList.getStations().stream()
                            .map(station -> {
                                String stationId = station.getStationID() != null ? station.getStationID().toString() : null;
                                String normalizedName = StationNameNormalizer.normalize(station.getStationName());
                                LocalTime arrivalTime = TimeParser.parseHHmmss(station.getArrivalTime());
                                LocalTime departureTime = TimeParser.parseHHmmss(station.getDepartureTime());

                                return new StationWithTime(stationId, normalizedName, arrivalTime, departureTime, lineName, lineColor);
                            });
                })
                .toList();
    }

    private CongestionData calculateCongestion(List<StationWithTime> stations) {
        Map<String, StationWithTime> uniqueStations = stations.stream()
                .filter(station -> station.stationId != null)
                .collect(Collectors.toMap(
                        station -> station.stationId,
                        station -> station,
                        (existing, replacement) -> existing
                ));

        Map<Byte, List<String>> stationsByHour = uniqueStations.values().stream()
                .map(station -> {
                    LocalTime stationTime = station.arrivalTime != null ? station.arrivalTime : station.departureTime;
                    if (stationTime == null || station.stationId == null) {
                        return null;
                    }
                    return Map.entry((byte) stationTime.getHour(), station.stationId);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));

        List<SubwayPassengerHourly> allPassengers = stationsByHour.entrySet().stream()
                .flatMap(entry -> subwayPassengerHourlyRepository
                        .findByStationIdsAndHourSlot(entry.getValue(), entry.getKey())
                        .stream())
                .toList();

        Map<String, SubwayPassengerHourly> passengerMap = new HashMap<>();
        double totalScore = 0.0;

        for (SubwayPassengerHourly passenger : allPassengers) {
            passengerMap.put(passenger.getSubwayStation().getStationId(), passenger);
            totalScore += passenger.getBoardingCount() + passenger.getAlightingCount();
        }

        int validStationCount = allPassengers.size();
        double averageScore = validStationCount > 0 ? totalScore / validStationCount : 0.0;

        return new CongestionData(
                passengerMap,
                averageScore
        );
    }

    private TimeRecommendationResult.TimeRecommendation mapToRecommendation(RouteWithCongestion route) {
        OdsaySubwayScheduleResponse.InfoData info = route.path.getInfo();

        LocalTime departureTime = TimeParser.parseHHmmss(info.getDepartureTime());
        LocalTime arrivalTime = TimeParser.parseHHmmss(info.getArrivalTime());

        CongestionLevel congestionLevel = CongestionLevel.fromScore(route.congestionData.averageScore);

        List<TimeRecommendationResult.StationCongestion> stationCongestions = route.stationsWithTime.stream()
                .map(station -> {
                    SubwayPassengerHourly passenger = route.congestionData.passengerMap.get(station.stationId);
                    return new TimeRecommendationResult.StationCongestion(
                            station.stationId,
                            station.stationName,
                            station.lineName,
                            station.lineColor,
                            station.arrivalTime,
                            station.departureTime,
                            passenger != null ? passenger.getBoardingCount() : null,
                            passenger != null ? passenger.getAlightingCount() : null,
                            passenger != null ? (passenger.getBoardingCount() + passenger.getAlightingCount()) : null
                    );
                })
                .toList();

        return new TimeRecommendationResult.TimeRecommendation(
                departureTime,
                arrivalTime,
                info.getTotalTime(),
                info.getTransferCount(),
                route.congestionData.averageScore,
                congestionLevel,
                stationCongestions
        );
    }

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

    private record RouteWithCongestion(
            OdsaySubwayScheduleResponse.PathData path,
            List<StationWithTime> stationsWithTime,
            CongestionData congestionData
    ) {}

    private record CongestionData(
            Map<String, SubwayPassengerHourly> passengerMap,
            double averageScore
    ) {}
}

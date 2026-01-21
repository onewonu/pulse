package com.pulse.service.search;

import com.pulse.api.odsay.OdsayClient;
import com.pulse.api.odsay.dto.OdsaySubwayScheduleResponse;
import com.pulse.dto.CongestionLevel;
import com.pulse.dto.TimeRecommendationRequest;
import com.pulse.dto.TimeRecommendationResult;
import com.pulse.entity.subway.SubwayPassengerHourly;
import com.pulse.exception.search.IncompleteCongestionDataException;
import com.pulse.exception.search.NoSchedulesAvailableException;
import com.pulse.exception.search.OdsayApiException;
import com.pulse.repository.subway.SubwayPassengerHourlyRepository;
import com.pulse.repository.subway.SubwayTrainScheduleRepository;
import com.pulse.util.DayCodeConverter;
import com.pulse.util.StationNameNormalizer;
import com.pulse.util.TimeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.UUID;

@Service
public class TimeRecommendationService {

    private static final int SHORTEST_TIME = 1;
    private static final int SUBWAY = 1;

    private static final Logger log = LoggerFactory.getLogger(TimeRecommendationService.class);

    private final SubwayTrainScheduleRepository subwayTrainScheduleRepository;
    private final SubwayPassengerHourlyRepository subwayPassengerHourlyRepository;
    private final OdsayClient odsayClient;

    public TimeRecommendationService(
            SubwayTrainScheduleRepository subwayTrainScheduleRepository,
            SubwayPassengerHourlyRepository subwayPassengerHourlyRepository,
            OdsayClient odsayClient
    ) {
        this.subwayTrainScheduleRepository = subwayTrainScheduleRepository;
        this.subwayPassengerHourlyRepository = subwayPassengerHourlyRepository;
        this.odsayClient = odsayClient;
    }

    public TimeRecommendationResult recommendTimes(TimeRecommendationRequest request) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        DayInfo dayInfo = convertToDayInfo(request);

        List<LocalTime> departureTimes = getAvailableDepartureTimes(request, dayInfo);

        if (departureTimes.isEmpty()) {
            throw new NoSchedulesAvailableException(
                    String.format(
                            "No train schedules found from %s to %s between %s and %s on %s",
                            request.getDepartureStationId(),
                            request.getArrivalStationId(),
                            request.getStartTime(),
                            request.getEndTime(),
                            request.getSearchDate()
                    )
            );
        }

        List<RouteWithCongestion> routes = processAllDepartureTimes(request, dayInfo, departureTimes, requestId);

        if (routes.isEmpty()) {
            throw new IncompleteCongestionDataException(
                    String.format(
                            "Congestion data incomplete for all routes from %s to %s on %s. " +
                            "Found %d departure times but none had sufficient congestion data",
                            request.getDepartureStationId(),
                            request.getArrivalStationId(),
                            request.getSearchDate(),
                            departureTimes.size()
                    )
            );
        }

        return buildResponse(request, dayInfo, routes);
    }

    private DayInfo convertToDayInfo(TimeRecommendationRequest request) {
        int dayCode = DayCodeConverter.convert(request.getSearchDate());
        String dayType = DayCodeConverter.toDayType(dayCode);
        return new DayInfo(dayCode, dayType);
    }

    private List<LocalTime> getAvailableDepartureTimes(TimeRecommendationRequest request, DayInfo dayInfo) {
        return subwayTrainScheduleRepository.findDistinctDepartureTimesByStationIdAndDayAndTimeRange(
                request.getDepartureStationId().toString(),
                dayInfo.dayType(),
                request.getStartTime(),
                request.getEndTime()
        );
    }

    private List<RouteWithCongestion> processAllDepartureTimes(
            TimeRecommendationRequest request,
            DayInfo dayInfo,
            List<LocalTime> departureTimes,
            String requestId
    ) {
        List<RouteWithCongestion> routes = new ArrayList<>();

        for (LocalTime departureTime : departureTimes) {
            RouteWithCongestion route = processSingleDepartureTime(request, dayInfo, departureTime, requestId);
            if (route != null) {
                routes.add(route);
            }
        }

        return routes;
    }

    private RouteWithCongestion processSingleDepartureTime(
            TimeRecommendationRequest request,
            DayInfo dayInfo,
            LocalTime departureTime,
            String requestId
    ) {
        try {
            Thread.sleep(200);

            OdsaySubwayScheduleResponse response = odsayClient.searchSubwaySchedule(
                    request.getDepartureStationId(),
                    request.getArrivalStationId(),
                    dayInfo.dayCode(),
                    departureTime.format(DateTimeFormatter.ofPattern("HHmm"))
            );

            OdsaySubwayScheduleResponse.PathData path = extractFastestPath(response);
            if (path == null) {
                return null;
            }

            List<StationWithTime> stationsWithTime = extractStationsWithTime(path);
            CongestionData congestionData = calculateCongestion(stationsWithTime);

            if (congestionData.completenessPercentage < 50.0) {
                return null;
            }

            return new RouteWithCongestion(path, stationsWithTime, congestionData);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            log.warn("[{}] Interrupted while processing departure time: {}", requestId, departureTime);

            return null;
        } catch (OdsayApiException | DataAccessException e) {
            return null;
        }
    }

    private TimeRecommendationResult buildResponse(
            TimeRecommendationRequest request,
            DayInfo dayInfo,
            List<RouteWithCongestion> routes
    ) {
        List<RouteWithCongestion> sortedRoutes = new ArrayList<>(routes);
        sortedRoutes.sort(Comparator.comparingDouble(r -> r.congestionData.totalScore));

        List<TimeRecommendationResult.TimeRecommendation> recommendations = new ArrayList<>();
        int limit = Math.min(3, sortedRoutes.size());

        for (int i = 0; i < limit; i++) {
            recommendations.add(mapToRecommendation(sortedRoutes.get(i)));
        }

        String departureStationName = extractStationName(routes, true);
        String arrivalStationName = extractStationName(routes, false);

        return new TimeRecommendationResult(
                request.getDepartureStationId(),
                request.getArrivalStationId(),
                departureStationName,
                arrivalStationName,
                request.getSearchDate(),
                dayInfo.dayType(),
                recommendations,
                null
        );
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

        for (OdsaySubwayScheduleResponse.PathData path : paths) {
            if (path.getPathType() != null && path.getPathType() == SHORTEST_TIME) {
                return path;
            }
        }

        return paths.getFirst();
    }

    private List<StationWithTime> extractStationsWithTime(OdsaySubwayScheduleResponse.PathData path) {
        List<StationWithTime> result = new ArrayList<>();

        if (path.getSubPath() == null) {
            return result;
        }

        for (OdsaySubwayScheduleResponse.SubPathData subPath : path.getSubPath()) {
            if (subPath.getMovingType() == SUBWAY) {
                OdsaySubwayScheduleResponse.PassStopListData passStopList = subPath.getPassStopList();

                if (passStopList != null && passStopList.getStations() != null) {
                    for (OdsaySubwayScheduleResponse.StationInfoData station : passStopList.getStations()) {
                        String stationId = station.getStationID() != null ? station.getStationID().toString() : null;
                        String normalizedName = StationNameNormalizer.normalize(station.getStationName());

                        LocalTime arrivalTime = TimeParser.parseHHmmss(station.getArrivalTime());
                        LocalTime departureTime = TimeParser.parseHHmmss(station.getDepartureTime());

                        result.add(new StationWithTime(stationId, normalizedName, arrivalTime, departureTime));
                    }
                }
            }
        }

        return result;
    }

    private CongestionData calculateCongestion(List<StationWithTime> stations) {
        Map<String, StationWithTime> uniqueStations = new HashMap<>();
        for (StationWithTime station : stations) {
            if (station.stationId != null) {
                uniqueStations.putIfAbsent(station.stationId, station);
            }
        }

        Map<Byte, List<String>> stationsByHour = new HashMap<>();
        for (StationWithTime station : uniqueStations.values()) {
            LocalTime stationTime = station.arrivalTime != null ? station.arrivalTime : station.departureTime;

            if (stationTime == null || station.stationId == null) continue;

            byte hour = (byte) stationTime.getHour();
            List<String> stationIds = stationsByHour.get(hour);
            if (stationIds == null) {
                stationIds = new ArrayList<>();
                stationsByHour.put(hour, stationIds);
            }
            stationIds.add(station.stationId);
        }

        Map<String, SubwayPassengerHourly> passengerMap = new HashMap<>();
        double totalScore = 0.0;
        int validStationCount = 0;

        for (Map.Entry<Byte, List<String>> entry : stationsByHour.entrySet()) {
            byte hour = entry.getKey();
            List<String> stationIds = entry.getValue();

            List<SubwayPassengerHourly> passengers = subwayPassengerHourlyRepository
                    .findByStationIdsAndHourSlot(stationIds, hour);

            for (SubwayPassengerHourly passenger : passengers) {
                String stationId = passenger.getSubwayStation().getStationId();
                passengerMap.put(stationId, passenger);

                int congestionScore = passenger.getBoardingCount() + passenger.getAlightingCount();
                totalScore += congestionScore;
                validStationCount++;
            }
        }

        int totalStationCount = uniqueStations.size();
        double completeness = totalStationCount > 0 ? (validStationCount * 100.0 / totalStationCount) : 0.0;

        double averageScore = validStationCount > 0 ? totalScore / validStationCount : 0.0;

        return new CongestionData(
                uniqueStations,
                passengerMap,
                averageScore,
                validStationCount,
                totalStationCount,
                completeness
        );
    }

    private TimeRecommendationResult.TimeRecommendation mapToRecommendation(RouteWithCongestion route) {
        OdsaySubwayScheduleResponse.InfoData info = route.path.getInfo();

        LocalTime departureTime = TimeParser.parseHHmmss(info.getDepartureTime());
        LocalTime arrivalTime = TimeParser.parseHHmmss(info.getArrivalTime());

        CongestionLevel congestionLevel = CongestionLevel.fromScore(route.congestionData.totalScore);

        List<TimeRecommendationResult.StationCongestion> stationCongestions = new ArrayList<>();
        for (StationWithTime station : route.stationsWithTime) {
            SubwayPassengerHourly passenger = route.congestionData.passengerMap.get(station.stationId);

            stationCongestions.add(new TimeRecommendationResult.StationCongestion(
                    station.stationId,
                    station.stationName,
                    station.arrivalTime,
                    station.departureTime,
                    passenger != null ? passenger.getBoardingCount() : null,
                    passenger != null ? passenger.getAlightingCount() : null,
                    passenger != null ? (passenger.getBoardingCount() + passenger.getAlightingCount()) : null
            ));
        }

        return new TimeRecommendationResult.TimeRecommendation(
                departureTime,
                arrivalTime,
                info.getTotalTime(),
                info.getTransferCount(),
                route.congestionData.totalScore,
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
            LocalTime departureTime
    ) {}

    private record RouteWithCongestion(
            OdsaySubwayScheduleResponse.PathData path,
            List<StationWithTime> stationsWithTime,
            CongestionData congestionData
    ) {}

    private record CongestionData(
            Map<String, StationWithTime> stationMap,
            Map<String, SubwayPassengerHourly> passengerMap,
            double totalScore,
            int validStationCount,
            int totalStationCount,
            double completenessPercentage
    ) {}
}

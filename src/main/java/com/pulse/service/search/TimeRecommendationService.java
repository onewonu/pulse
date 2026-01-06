package com.pulse.service.search;

import com.pulse.api.odsay.OdsayClient;
import com.pulse.api.odsay.dto.OdsaySubwayScheduleResponse;
import com.pulse.dto.CongestionLevel;
import com.pulse.dto.TimeRecommendationRequest;
import com.pulse.dto.TimeRecommendationResult;
import com.pulse.entity.subway.SubwayRidershipHourly;
import com.pulse.exception.search.IncompleteCongestionDataException;
import com.pulse.exception.search.NoSchedulesAvailableException;
import com.pulse.exception.search.OdsayApiException;
import com.pulse.repository.subway.SubwayRidershipHourlyRepository;
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
    private final SubwayRidershipHourlyRepository subwayRidershipHourlyRepository;
    private final OdsayClient odsayClient;

    public TimeRecommendationService(
            SubwayTrainScheduleRepository subwayTrainScheduleRepository,
            SubwayRidershipHourlyRepository subwayRidershipHourlyRepository,
            OdsayClient odsayClient
    ) {
        this.subwayTrainScheduleRepository = subwayTrainScheduleRepository;
        this.subwayRidershipHourlyRepository = subwayRidershipHourlyRepository;
        this.odsayClient = odsayClient;
    }

    public TimeRecommendationResult recommendTimes(TimeRecommendationRequest request) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        DayInfo dayInfo = convertToDayInfo(request);

        String departureStationName = fetchDepartureStationName(request, dayInfo);

        List<LocalTime> departureTimes = getAvailableDepartureTimes(request, departureStationName, dayInfo);

        if (departureTimes.isEmpty()) {
            log.warn(
                    "[{}] No train schedules found: sid={}, eid={}, date={}, time={}-{}",
                    requestId,
                    request.getDepartureStationId(),
                    request.getArrivalStationId(),
                    request.getSearchDate(),
                    request.getStartTime(),
                    request.getEndTime()
            );

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

            log.warn(
                    "[{}] Routes have incomplete congestion data: sid={}, eid={}, date={}, found {} departure times",
                    requestId,
                    request.getDepartureStationId(),
                    request.getArrivalStationId(),
                    request.getSearchDate(),
                    departureTimes.size()
            );

            throw new IncompleteCongestionDataException(
                    String.format(
                            "Congestion data incomplete for all routes from %s to %s on %s. " +
                            "Found %d departure times but none had sufficient congestion data (>=50%% coverage required)",
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

    private List<LocalTime> getAvailableDepartureTimes(TimeRecommendationRequest request, String departureStationName, DayInfo dayInfo) {
        return subwayTrainScheduleRepository.findDistinctDepartureTimesByStationAndDayAndTimeRange(
                departureStationName,
                dayInfo.dayType(),
                request.getStartTime(),
                request.getEndTime()
        );
    }

    private String fetchDepartureStationName(TimeRecommendationRequest request, DayInfo dayInfo) {
        OdsaySubwayScheduleResponse response = odsayClient.searchSubwaySchedule(
                request.getDepartureStationId(),
                request.getArrivalStationId(),
                dayInfo.dayCode(),
                request.getStartTime().format(DateTimeFormatter.ofPattern("HHmm"))
        );

        OdsaySubwayScheduleResponse.PathData path = extractFastestPath(response);
        if (path == null || path.getInfo() == null) {
            throw new IllegalStateException("No path found for station name extraction");
        }

        String stationName = path.getInfo().getFirstStartStationName();
        return StationNameNormalizer.normalize(stationName);
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
        } catch (RuntimeException e) {
            log.error("[{}] Unexpected error while processing departure time: {}", requestId, departureTime, e);
            throw e;
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
                dayInfo.dayCode(),
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
                        String normalizedName = StationNameNormalizer.normalize(station.getStationName());

                        LocalTime arrivalTime = TimeParser.parseHHmmss(station.getArrivalTime());
                        LocalTime departureTime = TimeParser.parseHHmmss(station.getDepartureTime());

                        result.add(new StationWithTime(normalizedName, arrivalTime, departureTime));
                    }
                }
            }
        }

        return result;
    }

    private CongestionData calculateCongestion(List<StationWithTime> stations) {
        Map<String, StationWithTime> uniqueStations = new HashMap<>();
        for (StationWithTime station : stations) {
            uniqueStations.putIfAbsent(station.stationName, station);
        }

        Map<Byte, List<String>> stationsByHour = new HashMap<>();
        for (StationWithTime station : uniqueStations.values()) {
            LocalTime stationTime = station.arrivalTime != null ? station.arrivalTime : station.departureTime;

            if (stationTime == null) continue;

            byte hour = (byte) stationTime.getHour();
            List<String> stationNames = stationsByHour.get(hour);
            if (stationNames == null) {
                stationNames = new ArrayList<>();
                stationsByHour.put(hour, stationNames);
            }
            stationNames.add(station.stationName);
        }

        // 3. 시간대별 일괄 조회 및 혼잡도 계산
        Map<String, SubwayRidershipHourly> ridershipMap = new HashMap<>();
        double totalScore = 0.0;
        int validStationCount = 0;

        for (Map.Entry<Byte, List<String>> entry : stationsByHour.entrySet()) {
            byte hour = entry.getKey();
            List<String> stationNames = entry.getValue();

            List<SubwayRidershipHourly> riderships = subwayRidershipHourlyRepository
                    .findByStationNamesAndHourSlot(stationNames, hour);

            for (SubwayRidershipHourly ridership : riderships) {
                String stationName = ridership.getSubwayStation().getStationName();
                ridershipMap.put(stationName, ridership);

                int congestionScore = ridership.getBoardingCount() + ridership.getAlightingCount();
                totalScore += congestionScore;
                validStationCount++;
            }
        }

        int totalStationCount = uniqueStations.size();
        double completeness = totalStationCount > 0 ? (validStationCount * 100.0 / totalStationCount) : 0.0;

        return new CongestionData(
                uniqueStations,
                ridershipMap,
                totalScore,
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
            SubwayRidershipHourly ridership = route.congestionData.ridershipMap.get(station.stationName);

            stationCongestions.add(new TimeRecommendationResult.StationCongestion(
                    station.stationName,
                    station.arrivalTime,
                    station.departureTime,
                    ridership != null ? ridership.getBoardingCount() : null,
                    ridership != null ? ridership.getAlightingCount() : null,
                    ridership != null ? (ridership.getBoardingCount() + ridership.getAlightingCount()) : null
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
            Map<String, SubwayRidershipHourly> ridershipMap,
            double totalScore,
            int validStationCount,
            int totalStationCount,
            double completenessPercentage
    ) {}
}

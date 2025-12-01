package com.pulse.service.dataload.bus;

import com.pulse.api.seoulopendataplaza.SeoulOpenDataPlazaClient;
import com.pulse.api.seoulopendataplaza.dto.bus.BusApiResponse;
import com.pulse.api.seoulopendataplaza.dto.bus.BusRidershipData;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.bus.BusRidershipHourly;
import com.pulse.entity.bus.BusRoute;
import com.pulse.entity.bus.BusStop;
import com.pulse.mapper.BusDataMapper;
import com.pulse.repository.bus.BusRidershipHourlyRepository;
import com.pulse.repository.bus.BusRouteRepository;
import com.pulse.repository.bus.BusStopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class BusStatisticsDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(BusMasterDataLoadService.class);

    private final SeoulOpenDataPlazaClient apiClient;
    private final BusDataMapper mapper;
    private final BusRouteRepository busRouteRepository;
    private final BusStopRepository busStopRepository;
    private final BusRidershipHourlyRepository busRidershipRepository;

    @Value("${seoul-api.page-size}")
    private int pageSize;

    public BusStatisticsDataLoadService(
            SeoulOpenDataPlazaClient apiClient,
            BusDataMapper mapper,
            BusRouteRepository busRouteRepository,
            BusStopRepository busStopRepository,
            BusRidershipHourlyRepository busRidershipRepository
    ) {
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.busRouteRepository = busRouteRepository;
        this.busStopRepository = busStopRepository;
        this.busRidershipRepository = busRidershipRepository;
    }

    public DataLoadResult loadBusStatisticsData(String yearMonth) {
        log.info("Start loading bus statistics data: {}", yearMonth);

        try {
            LocalDate statDate = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyyMM")).atDay(1);
            busRidershipRepository.deleteByStatDate(statDate);
            log.info("Existing bus statistics data has been deleted: {}", yearMonth);

            Map<String, BusRoute> routeCache = new HashMap<>();
            for (BusRoute route : busRouteRepository.findAll()) {
                routeCache.put(route.getRouteNumber(), route);
            }
            log.info("Loaded {} routes into cache", routeCache.size());

            Map<String, BusStop> stopCache = new HashMap<>();
            for (BusStop stop : busStopRepository.findAll()) {
                stopCache.put(stop.getStopId(), stop);
            }
            log.info("Loaded {} stops into cache", stopCache.size());

            Map<String, BusRidershipHourly> hourlyDataMap = new HashMap<>();
            int apiRecordCount = 0;
            int startIndex = 1;

            while (true) {
                int endIndex = startIndex + pageSize - 1;
                BusApiResponse response = apiClient.fetchBusRidershipData(yearMonth, startIndex, endIndex);

                if (response == null) {
                    break;
                }

                for (BusRidershipData data : response.getData()) {
                    BusRoute route = routeCache.get(data.getRteNo());
                    if (route == null) {
                        throw new IllegalStateException("No route master data: " + data.getRteNo());
                    }

                    BusStop stop = stopCache.get(data.getStopsId());
                    if (stop == null) {
                        throw new IllegalStateException("No stop master data: " + data.getStopsId());
                    }

                    List<BusRidershipHourly> hourlyData = mapper.toBusRidershipHourlyList(data, route, stop);

                    for (BusRidershipHourly hourly : hourlyData) {
                        String key = String.format("%s-%s-%s-%d",
                                hourly.getStatDate(),
                                hourly.getBusRoute().getRouteNumber(),
                                hourly.getBusStop().getStopId(),
                                hourly.getHourSlot());

                        hourlyDataMap.put(key, hourly);
                    }

                    apiRecordCount++;
                }

                log.info("Bus statistics data progress: {} ~ {} (API records: {})",
                        startIndex, endIndex, apiRecordCount);

                startIndex = endIndex + 1;
            }

            List<BusRidershipHourly> uniqueHourlyData = new ArrayList<>(hourlyDataMap.values());
            busRidershipRepository.saveAll(uniqueHourlyData);

            int totalCount = uniqueHourlyData.size();
            log.info("Bus statistics data loading completed: {} API records -> {} unique hourly records",
                    apiRecordCount, totalCount);

            return DataLoadResult.success("Bus statistics data", totalCount);

        } catch (Exception e) {
            log.error("Failure to load bus statistics data", e);
            return DataLoadResult.failure("Bus statistics data", e.getMessage());
        }
    }
}

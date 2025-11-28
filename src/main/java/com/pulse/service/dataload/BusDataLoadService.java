package com.pulse.service.dataload;

import com.pulse.client.transport.SeoulOpenApiClient;
import com.pulse.client.transport.dto.bus.BusApiResponse;
import com.pulse.client.transport.dto.bus.BusRidershipData;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.bus.*;
import com.pulse.mapper.BusDataMapper;
import com.pulse.repository.bus.BusRidershipHourlyRepository;
import com.pulse.repository.bus.BusRouteRepository;
import com.pulse.repository.bus.BusRouteStopRepository;
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
public class BusDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(BusDataLoadService.class);

    private final SeoulOpenApiClient apiClient;
    private final BusDataMapper mapper;
    private final BusRouteRepository busRouteRepository;
    private final BusStopRepository busStopRepository;
    private final BusRouteStopRepository busRouteStopRepository;
    private final BusRidershipHourlyRepository busRidershipRepository;

    @Value("${seoul-api.page-size}")
    private int pageSize;

    public BusDataLoadService(
            SeoulOpenApiClient apiClient,
            BusDataMapper mapper,
            BusRouteRepository busRouteRepository,
            BusStopRepository busStopRepository,
            BusRouteStopRepository busRouteStopRepository,
            BusRidershipHourlyRepository busRidershipRepository
    ) {
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.busRouteRepository = busRouteRepository;
        this.busStopRepository = busStopRepository;
        this.busRouteStopRepository = busRouteStopRepository;
        this.busRidershipRepository = busRidershipRepository;
    }

    public DataLoadResult loadBusMasterData(String yearMonth) {
        log.info("Start loading bus master data: {}", yearMonth);

        try {
            int totalCount = 0;
            int startIndex = 1;

            while (true) {
                int endIndex = startIndex + pageSize - 1;
                BusApiResponse response = apiClient.fetchBusRidershipData(yearMonth, startIndex, endIndex);

                if (response == null || response.getData() == null || response.getData().isEmpty()) {
                    log.info("Bus master data is empty.");
                    break;
                }

                for (BusRidershipData data : response.getData()) {
                    BusRoute route = busRouteRepository
                            .findByRouteNumber(data.getRteNo())
                            .orElseGet(() -> busRouteRepository.save(mapper.toBusRoute(data)));

                    BusStop stop = busStopRepository
                            .findById(data.getStopsId())
                            .orElseGet(() -> busStopRepository.save(mapper.toBusStop(data)));

                    saveBusRouteStopIfNotExists(route, stop);

                    totalCount++;
                }

                log.info("Bus master data progress: {} ~ {} (total - {})", startIndex, endIndex, totalCount);

                startIndex = endIndex + 1;
            }

            log.info("Bus master data loading completed: total - {}", totalCount);

            return DataLoadResult.success("Bus master data", totalCount);

        } catch (Exception e) {
            log.error("Bus master data load failure", e);

            return DataLoadResult.failure("Bus master data", e.getMessage());
        }
    }

    private void saveBusRouteStopIfNotExists(BusRoute route, BusStop stop) {
        BusRouteStopId id = BusRouteStopId.of(route.getRouteNumber(), stop.getStopId());

        if (!busRouteStopRepository.existsById(id)) {
            busRouteStopRepository.save(BusRouteStop.of(route, stop));
        }
    }

    public DataLoadResult loadBusStatisticsData(String yearMonth) {
        log.info("Start loading bus statistics data: {}", yearMonth);

        try {
            LocalDate statDate = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyyMM")).atDay(1);
            busRidershipRepository.deleteByStatDate(statDate);
            log.info("Existing bus statistical data has been deleted: {}", yearMonth);

            Map<String, BusRidershipHourly> hourlyDataMap = new HashMap<>();
            int apiRecordCount = 0;
            int startIndex = 1;

            while (true) {
                int endIndex = startIndex + pageSize - 1;
                BusApiResponse response = apiClient.fetchBusRidershipData(yearMonth, startIndex, endIndex);

                if (response == null || response.getData() == null || response.getData().isEmpty()) {
                    log.info("Bus statistics data is empty.");
                    break;
                }

                for (BusRidershipData data : response.getData()) {
                    BusRoute route = busRouteRepository
                            .findByRouteNumber(data.getRteNo())
                            .orElseThrow(() -> new IllegalStateException(
                                    "No route master data: " + data.getRteNo()));

                    BusStop stop = busStopRepository
                            .findById(data.getStopsId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "No stop master data: " + data.getStopsId()));

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

            return DataLoadResult.success("Bus statistical data", totalCount);

        } catch (Exception e) {
            log.error("failure to load bus statistics data", e);
            return DataLoadResult.failure("Bus statistical data", e.getMessage());
        }
    }
}

package com.pulse.service.dataload.bus;

import com.pulse.api.seoulopendataplaza.SeoulOpenDataPlazaClient;
import com.pulse.api.seoulopendataplaza.dto.bus.BusApiResponse;
import com.pulse.api.seoulopendataplaza.dto.bus.BusRidershipData;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.bus.BusRoute;
import com.pulse.entity.bus.BusRouteStop;
import com.pulse.entity.bus.BusRouteStopId;
import com.pulse.entity.bus.BusStop;
import com.pulse.mapper.BusDataMapper;
import com.pulse.repository.bus.BusRidershipHourlyRepository;
import com.pulse.repository.bus.BusRouteRepository;
import com.pulse.repository.bus.BusRouteStopRepository;
import com.pulse.repository.bus.BusStopRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class BusMasterDataLoadService {

    private static final Logger log = LoggerFactory.getLogger(BusMasterDataLoadService.class);

    private final EntityManager entityManager;
    private final SeoulOpenDataPlazaClient apiClient;
    private final BusDataMapper mapper;
    private final BusRouteRepository busRouteRepository;
    private final BusStopRepository busStopRepository;
    private final BusRouteStopRepository busRouteStopRepository;

    @Value("${seoul-api.page-size}")
    private int pageSize;

    public BusMasterDataLoadService(
            EntityManager entityManager,
            SeoulOpenDataPlazaClient apiClient,
            BusDataMapper mapper,
            BusRouteRepository busRouteRepository,
            BusStopRepository busStopRepository,
            BusRouteStopRepository busRouteStopRepository,
            BusRidershipHourlyRepository busRidershipRepository
    ) {
        this.entityManager = entityManager;
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.busRouteRepository = busRouteRepository;
        this.busStopRepository = busStopRepository;
        this.busRouteStopRepository = busRouteStopRepository;
    }

    public DataLoadResult loadBusMasterData(String yearMonth) {
        log.info("Start loading bus master data: {}", yearMonth);

        try {
            busRouteStopRepository.deleteAll();
            busRouteRepository.deleteAll();
            busStopRepository.deleteAll();
            entityManager.flush();
            entityManager.clear();
            log.info("Existing bus master data has been deleted");

            Map<String, BusRoute> routeMap = new HashMap<>();
            Map<String, BusStop> stopMap = new HashMap<>();
            Set<BusRouteStopId> routeStopSet = new HashSet<>();
            int apiRecordCount = 0;
            int startIndex = 1;

            while (true) {
                int endIndex = startIndex + pageSize - 1;
                BusApiResponse response = apiClient.fetchBusRidershipData(yearMonth, startIndex, endIndex);

                if (response == null) {
                    break;
                }

                for (BusRidershipData data : response.getData()) {
                    BusRoute route = mapper.toBusRoute(data);
                    routeMap.put(route.getRouteNumber(), route);

                    BusStop stop = mapper.toBusStop(data);
                    stopMap.put(stop.getStopId(), stop);

                    routeStopSet.add(BusRouteStopId.of(route.getRouteNumber(), stop.getStopId()));

                    apiRecordCount++;
                }

                log.info("Bus master data progress: {} ~ {} (API records: {})",
                        startIndex, endIndex, apiRecordCount);
                startIndex = endIndex + 1;
            }

            List<BusRoute> uniqueRoutes = new ArrayList<>(routeMap.values());
            List<BusStop> uniqueStops = new ArrayList<>(stopMap.values());

            busRouteRepository.saveAll(uniqueRoutes);
            busStopRepository.saveAll(uniqueStops);
            entityManager.flush();
            log.info("Saved {} unique routes and {} unique stops", uniqueRoutes.size(), uniqueStops.size());

            List<BusRouteStop> routeStops = new ArrayList<>();
            for (BusRouteStopId id : routeStopSet) {
                BusRoute route = routeMap.get(id.getRouteNumber());
                BusStop stop = stopMap.get(id.getStopId());
                routeStops.add(BusRouteStop.of(route, stop));
            }
            busRouteStopRepository.saveAll(routeStops);

            int totalCount = apiRecordCount;
            log.info("Bus master data loading completed: {} API records -> {} routes, {} stops, {} route-stops",
                    apiRecordCount, uniqueRoutes.size(), uniqueStops.size(), routeStops.size());

            return DataLoadResult.success("Bus master data", totalCount);

        } catch (Exception e) {
            log.error("Bus master data load failure", e);

            return DataLoadResult.failure("Bus master data", e.getMessage());
        }
    }
}

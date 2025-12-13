package com.pulse.service.dataload.bus;

import com.pulse.api.seoulopendataplaza.SeoulOpenDataPlazaClient;
import com.pulse.api.seoulopendataplaza.dto.bus.BusApiResponse;
import com.pulse.api.seoulopendataplaza.dto.bus.BusRidershipData;
import com.pulse.config.SeoulApiProperties;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.bus.BusRoute;
import com.pulse.entity.bus.BusRouteStop;
import com.pulse.entity.bus.BusRouteStopId;
import com.pulse.entity.bus.BusStop;
import com.pulse.mapper.BusDataMapper;
import com.pulse.repository.bus.BusRouteRepository;
import com.pulse.repository.bus.BusRouteStopRepository;
import com.pulse.repository.bus.BusStopRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final SeoulApiProperties properties;

    public BusMasterDataLoadService(
            EntityManager entityManager,
            SeoulOpenDataPlazaClient apiClient,
            BusDataMapper mapper,
            BusRouteRepository busRouteRepository,
            BusStopRepository busStopRepository,
            BusRouteStopRepository busRouteStopRepository,
            SeoulApiProperties properties
    ) {
        this.entityManager = entityManager;
        this.apiClient = apiClient;
        this.mapper = mapper;
        this.busRouteRepository = busRouteRepository;
        this.busStopRepository = busStopRepository;
        this.busRouteStopRepository = busRouteStopRepository;
        this.properties = properties;
    }

    public DataLoadResult loadBusMasterData(String yearMonth) {
        log.info("Start loading bus master data: {}", yearMonth);

        deleteAllExistingMasterData();

        List<BusRidershipData> apiDataList = fetchAllDataFromApi(yearMonth);

        MasterDataCollections collections = extractAndDeduplicateMasterData(apiDataList);

        saveRoutesAndStops(collections);

        saveRouteStopAssociations(collections);

        int totalCount = apiDataList.size();
        log.info("Bus master data loading completed: {} API records -> {} routes, {} stops, {} route-stops",
                totalCount, collections.routes().size(), collections.stops().size(),
                collections.routeStopIds().size());

        return DataLoadResult.success("Bus master data", totalCount);
    }

    private void deleteAllExistingMasterData() {
        busRouteStopRepository.deleteAll();
        busRouteRepository.deleteAll();
        busStopRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
        log.info("Existing bus master data has been deleted");
    }

    private List<BusRidershipData> fetchAllDataFromApi(String yearMonth) {
        log.info("Starting to fetch bus master data from API: {}", yearMonth);

        List<BusRidershipData> allData = new ArrayList<>();
        int startIndex = 1;
        boolean hasMoreData = true;

        while (hasMoreData) {
            int endIndex = startIndex + properties.getPageSize() - 1;
            BusApiResponse response = apiClient.fetchBusRidershipData(yearMonth, startIndex, endIndex);

            List<BusRidershipData> pageData = (response != null) ? response.getData() : null;

            if (pageData != null && !pageData.isEmpty()) {
                allData.addAll(pageData);
                log.info("Fetched bus master data: {} ~ {} ({} records in this page, {} total)",
                        startIndex, endIndex, pageData.size(), allData.size());
                startIndex = endIndex + 1;
            } else {
                hasMoreData = false;
            }
        }

        log.info("Completed fetching bus master data: {} API records", allData.size());
        return allData;
    }

    private MasterDataCollections extractAndDeduplicateMasterData(List<BusRidershipData> apiDataList) {
        log.info("Starting to extract and deduplicate master data from {} API records", apiDataList.size());

        Map<String, BusRoute> routeMap = new HashMap<>();
        Map<String, BusStop> stopMap = new HashMap<>();
        Set<BusRouteStopId> routeStopSet = new HashSet<>();

        for (BusRidershipData data : apiDataList) {
            BusRoute route = mapper.toBusRoute(data);
            routeMap.put(route.getRouteNumber(), route);

            BusStop stop = mapper.toBusStop(data);
            stopMap.put(stop.getStopId(), stop);

            routeStopSet.add(BusRouteStopId.of(route.getRouteNumber(), stop.getStopId()));
        }

        List<BusRoute> uniqueRoutes = new ArrayList<>(routeMap.values());
        List<BusStop> uniqueStops = new ArrayList<>(stopMap.values());

        log.info("Extracted and deduplicated: {} unique routes, {} unique stops, {} route-stop associations",
                uniqueRoutes.size(), uniqueStops.size(), routeStopSet.size());

        return new MasterDataCollections(routeMap, stopMap, uniqueRoutes, uniqueStops, routeStopSet);
    }

    private void saveRoutesAndStops(MasterDataCollections collections) {
        busRouteRepository.saveAll(collections.routes());
        busStopRepository.saveAll(collections.stops());
        entityManager.flush();
        log.info("Saved {} unique routes and {} unique stops",
                collections.routes().size(), collections.stops().size());
    }

    private void saveRouteStopAssociations(MasterDataCollections collections) {
        List<BusRouteStop> routeStops = new ArrayList<>();

        for (BusRouteStopId id : collections.routeStopIds()) {
            BusRoute route = collections.routeMap().get(id.getRouteNumber());
            BusStop stop = collections.stopMap().get(id.getStopId());
            routeStops.add(BusRouteStop.of(route, stop));
        }

        busRouteStopRepository.saveAll(routeStops);
        log.info("Saved {} route-stop associations", routeStops.size());
    }

    private record MasterDataCollections(
            Map<String, BusRoute> routeMap,
            Map<String, BusStop> stopMap,
            List<BusRoute> routes,
            List<BusStop> stops,
            Set<BusRouteStopId> routeStopIds
    ) {}
}

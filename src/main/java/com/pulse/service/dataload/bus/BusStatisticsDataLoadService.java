package com.pulse.service.dataload.bus;

import com.pulse.api.seoulopendataplaza.SeoulOpenDataPlazaClient;
import com.pulse.api.seoulopendataplaza.dto.bus.BusApiResponse;
import com.pulse.api.seoulopendataplaza.dto.bus.BusRidershipData;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.bus.BusRidershipHourly;
import com.pulse.entity.bus.BusRoute;
import com.pulse.entity.bus.BusStop;
import com.pulse.exception.dataload.MasterDataNotFoundException;
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

    private static final Logger log = LoggerFactory.getLogger(BusStatisticsDataLoadService.class);

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

        deleteSameYearAndMonth(yearMonth);

        MasterDataCaches caches = loadMasterDataCaches();

        List<BusRidershipData> apiDataList = fetchAllDataFromApi(yearMonth);

        Map<String, BusRidershipHourly> hourlyDataMap = processRidershipData(apiDataList, caches);

        int totalCount = saveRidershipData(hourlyDataMap, apiDataList.size());
        return DataLoadResult.success("Bus statistics data", totalCount);
    }

    private void deleteSameYearAndMonth(String yearMonth) {
        LocalDate statDate = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyyMM")).atDay(1);
        busRidershipRepository.deleteByStatDate(statDate);
        log.info("Existing bus statistics data has been deleted: {}", yearMonth);
    }

    private MasterDataCaches loadMasterDataCaches() {
        Map<String, BusRoute> routeCache = new HashMap<>();
        for (BusRoute route : busRouteRepository.findAll()) {
            routeCache.put(route.getRouteNumber(), route);
        }

        Map<String, BusStop> stopCache = new HashMap<>();
        for (BusStop stop : busStopRepository.findAll()) {
            stopCache.put(stop.getStopId(), stop);
        }

        log.info("Loaded master data into cache: {} routes, {} stops",
                routeCache.size(), stopCache.size());

        return new MasterDataCaches(routeCache, stopCache);
    }

    private List<BusRidershipData> fetchAllDataFromApi(String yearMonth) {
        log.info("Starting to fetch bus statistics data from API: {}", yearMonth);

        List<BusRidershipData> allData = new ArrayList<>();
        int startIndex = 1;
        boolean hasMoreData = true;

        while (hasMoreData) {
            int endIndex = startIndex + pageSize - 1;
            BusApiResponse response = apiClient.fetchBusRidershipData(yearMonth, startIndex, endIndex);

            List<BusRidershipData> pageData = (response != null) ? response.getData() : null;

            if (pageData != null && !pageData.isEmpty()) {
                allData.addAll(pageData);
                log.info("Fetched bus statistics data: {} ~ {} ({} records in this page, {} total)",
                        startIndex, endIndex, pageData.size(), allData.size());
                startIndex = endIndex + 1;
            } else {
                hasMoreData = false;
            }
        }

        log.info("Completed fetching bus statistics data: {} API records", allData.size());
        return allData;
    }

    private Map<String, BusRidershipHourly> processRidershipData(
            List<BusRidershipData> apiDataList,
            MasterDataCaches caches
    ) {
        log.info("Starting to process {} API records", apiDataList.size());

        Map<String, BusRidershipHourly> hourlyDataMap = new HashMap<>();

        for (BusRidershipData data : apiDataList) {
            List<BusRidershipHourly> hourlyDataList = convertToHourlyRidership(data, caches);

            for (BusRidershipHourly hourly : hourlyDataList) {
                String key = generateUniqueKey(hourly);
                hourlyDataMap.put(key, hourly);
            }
        }

        log.info("Completed processing: {} API records -> {} unique hourly records",
                apiDataList.size(), hourlyDataMap.size());

        return hourlyDataMap;
    }

    private List<BusRidershipHourly> convertToHourlyRidership(
            BusRidershipData data,
            MasterDataCaches caches
    ) {
        BusRoute route = caches.routeCache().get(data.getRteNo());
        if (route == null) {
            throw new MasterDataNotFoundException("route", data.getRteNo());
        }

        BusStop stop = caches.stopCache().get(data.getStopsId());
        if (stop == null) {
            throw new MasterDataNotFoundException("stop", data.getStopsId());
        }

        return mapper.toBusRidershipHourlyList(data, route, stop);
    }

    private String generateUniqueKey(BusRidershipHourly hourly) {
        return String.format("%s-%s-%s-%d",
                hourly.getStatDate(),
                hourly.getBusRoute().getRouteNumber(),
                hourly.getBusStop().getStopId(),
                hourly.getHourSlot());
    }

    private int saveRidershipData(Map<String, BusRidershipHourly> hourlyDataMap, int apiRecordCount) {
        List<BusRidershipHourly> uniqueHourlyData = new ArrayList<>(hourlyDataMap.values());
        busRidershipRepository.saveAll(uniqueHourlyData);

        int totalCount = uniqueHourlyData.size();
        log.info("Bus statistics data loading completed: {} API records -> {} unique hourly records",
                apiRecordCount, totalCount);
        return totalCount;
    }

    private record MasterDataCaches(
            Map<String, BusRoute> routeCache,
            Map<String, BusStop> stopCache
    ) {}
}

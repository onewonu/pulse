package com.pulse.service.dataload.bus;

import com.pulse.api.seoulopendataplaza.SeoulOpenDataPlazaClient;
import com.pulse.api.seoulopendataplaza.dto.bus.BusApiResponse;
import com.pulse.api.seoulopendataplaza.dto.bus.BusRidershipData;
import com.pulse.config.SeoulApiProperties;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.bus.BusRoute;
import com.pulse.entity.bus.BusStop;
import com.pulse.mapper.BusDataMapper;
import com.pulse.repository.bus.BusRouteRepository;
import com.pulse.repository.bus.BusRouteStopRepository;
import com.pulse.repository.bus.BusStopRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusMasterDataLoadServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private SeoulOpenDataPlazaClient apiClient;

    @Mock
    private BusDataMapper mapper;

    @Mock
    private BusRouteRepository busRouteRepository;

    @Mock
    private BusStopRepository busStopRepository;

    @Mock
    private BusRouteStopRepository busRouteStopRepository;

    @Mock
    private SeoulApiProperties properties;

    @InjectMocks
    private BusMasterDataLoadService service;

    private static final int PAGE_SIZE = 1000;
    private static final String TEST_YEAR_MONTH = "202510";

    @BeforeEach
    void setUp() {
        when(properties.getPageSize()).thenReturn(PAGE_SIZE);
    }

    @Test
    void shouldLoadBusMasterDataSuccessfully_WhenSinglePageApiResponse() {
        // Given
        BusApiResponse response = createBusApiResponse(Arrays.asList(
                createBusRidershipData("100", "강남역"),
                createBusRidershipData("100", "역삼역")
        ));

        // API 클라이언트 Mock
        when(apiClient.fetchBusRidershipData(TEST_YEAR_MONTH, 1, PAGE_SIZE)).thenReturn(response);

        when(apiClient.fetchBusRidershipData(TEST_YEAR_MONTH, PAGE_SIZE + 1, PAGE_SIZE * 2))
                .thenReturn(null);

        // Mapper Mock
        when(mapper.toBusRoute(any())).thenAnswer(inv ->
                BusRoute.of(inv.<BusRidershipData>getArgument(0).getRteNo(),
                        inv.<BusRidershipData>getArgument(0).getRteNm(),
                        inv.<BusRidershipData>getArgument(0).getTrfcMnsTypeCd(),
                        inv.<BusRidershipData>getArgument(0).getTrfcMnsTypeNm()));

        when(mapper.toBusStop(any())).thenAnswer(inv ->
                BusStop.of(inv.<BusRidershipData>getArgument(0).getStopsId(),
                        inv.<BusRidershipData>getArgument(0).getStopsArsNo(),
                        inv.<BusRidershipData>getArgument(0).getSbwyStnsNm()));

        // Repository Mock
        when(busRouteRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(busStopRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(busRouteStopRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        DataLoadResult result = service.loadBusMasterData(TEST_YEAR_MONTH);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalCount());
    }

    @Test
    void shouldDeduplicateCorrectly_WhenSameRouteAndStopAppearsMultipleTimes() {
        // Given
        BusApiResponse response = createBusApiResponse(Arrays.asList(
                createBusRidershipData("100", "강남역"),
                createBusRidershipData("100", "역삼역"),
                createBusRidershipData("100", "강남역"),
                createBusRidershipData("200", "강남역")
        ));

        // API 클라이언트 Mock
        when(apiClient.fetchBusRidershipData(TEST_YEAR_MONTH, 1, PAGE_SIZE)).thenReturn(response);

        when(apiClient.fetchBusRidershipData(TEST_YEAR_MONTH, PAGE_SIZE + 1, PAGE_SIZE * 2))
                .thenReturn(null);

        // Mapper Mock
        when(mapper.toBusRoute(any())).thenAnswer(inv ->
                BusRoute.of(inv.<BusRidershipData>getArgument(0).getRteNo(),
                        inv.<BusRidershipData>getArgument(0).getRteNm(),
                        inv.<BusRidershipData>getArgument(0).getTrfcMnsTypeCd(),
                        inv.<BusRidershipData>getArgument(0).getTrfcMnsTypeNm()));

        when(mapper.toBusStop(any())).thenAnswer(inv ->
                BusStop.of(inv.<BusRidershipData>getArgument(0).getStopsId(),
                        inv.<BusRidershipData>getArgument(0).getStopsArsNo(),
                        inv.<BusRidershipData>getArgument(0).getSbwyStnsNm()));

        // Repository Mock: 2개의 노선, 2개의 정류장, 3개의 조합
        when(busRouteRepository.saveAll(any())).thenAnswer(inv -> {
            List<BusRoute> routes = inv.getArgument(0);
            assertEquals(2, routes.size());
            return routes;
        });

        when(busStopRepository.saveAll(any())).thenAnswer(inv -> {
            List<BusStop> stops = inv.getArgument(0);
            assertEquals(2, stops.size());
            return stops;
        });

        when(busRouteStopRepository.saveAll(any())).thenAnswer(inv -> {
            List<?> routeStops = inv.getArgument(0);
            assertEquals(3, routeStops.size());
            return routeStops;
        });

        // When
        DataLoadResult result = service.loadBusMasterData(TEST_YEAR_MONTH);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(4, result.getTotalCount());
    }


    private BusApiResponse createBusApiResponse(List<BusRidershipData> records) {
        BusApiResponse response = new BusApiResponse();
        BusApiResponse.BusApiData apiData = new BusApiResponse.BusApiData();
        apiData.setListTotalCount(records.size());
        apiData.setRow(records);
        response.setCardBusTimeNew(apiData);
        return response;
    }

    private BusRidershipData createBusRidershipData(String routeNo, String stopName) {
        BusRidershipData data = new BusRidershipData();
        data.setUseYm(TEST_YEAR_MONTH);
        data.setRteNo(routeNo);
        data.setRteNm(routeNo + "번");
        data.setStopsId("STOP_" + stopName);
        data.setStopsArsNo("ARS_" + stopName);
        data.setSbwyStnsNm(stopName);
        data.setTrfcMnsTypeCd("01");
        data.setTrfcMnsTypeNm("간선");
        data.setRegYmd("20251001");

        data.setHr0GetOnTnope(100.0);
        data.setHr1GetOnNope(100.0);
        data.setHr2GetOnTnope(100.0);
        data.setHr3GetOnTnope(100.0);
        data.setHr4GetOnTnope(100.0);
        data.setHr5GetOnTnope(100.0);
        data.setHr6GetOnTnope(100.0);
        data.setHr7GetOnTnope(100.0);
        data.setHr8GetOnTnope(100.0);
        data.setHr9GetOnTnope(100.0);
        data.setHr10GetOnTnope(100.0);
        data.setHr11GetOnTnope(100.0);
        data.setHr12GetOnTnope(100.0);
        data.setHr13GetOnTnope(100.0);
        data.setHr14GetOnTnope(100.0);
        data.setHr15GetOnTnope(100.0);
        data.setHr16GetOnTnope(100.0);
        data.setHr17GetOnTnope(100.0);
        data.setHr18GetOnTnope(100.0);
        data.setHr19GetOnTnope(100.0);
        data.setHr20GetOnTnope(100.0);
        data.setHr21GetOnTnope(100.0);
        data.setHr22GetOnTnope(100.0);
        data.setHr23GetOnTnope(100.0);

        data.setHr0GetOffTnope(100.0);
        data.setHr1GetOffNope(100.0);
        data.setHr2GetOffTnope(100.0);
        data.setHr3GetOffTnope(100.0);
        data.setHr4GetOffTnope(100.0);
        data.setHr5GetOffTnope(100.0);
        data.setHr6GetOffTnope(100.0);
        data.setHr7GetOffTnope(100.0);
        data.setHr8GetOffTnope(100.0);
        data.setHr9GetOffTnope(100.0);
        data.setHr10GetOffTnope(100.0);
        data.setHr11GetOffTnope(100.0);
        data.setHr12GetOffTnope(100.0);
        data.setHr13GetOffTnope(100.0);
        data.setHr14GetOffTnope(100.0);
        data.setHr15GetOffTnope(100.0);
        data.setHr16GetOffTnope(100.0);
        data.setHr17GetOffTnope(100.0);
        data.setHr18GetOffTnope(100.0);
        data.setHr19GetOffTnope(100.0);
        data.setHr20GetOffTnope(100.0);
        data.setHr21GetOffTnope(100.0);
        data.setHr22GetOffTnope(100.0);
        data.setHr23GetOffTnope(100.0);

        return data;
    }
}

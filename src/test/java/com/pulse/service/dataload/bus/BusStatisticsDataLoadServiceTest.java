package com.pulse.service.dataload.bus;

import com.pulse.api.seoulopendataplaza.SeoulOpenDataPlazaClient;
import com.pulse.api.seoulopendataplaza.dto.bus.BusApiResponse;
import com.pulse.api.seoulopendataplaza.dto.bus.BusRidershipData;
import com.pulse.config.SeoulApiProperties;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.bus.BusRidershipHourly;
import com.pulse.entity.bus.BusRoute;
import com.pulse.entity.bus.BusStop;
import com.pulse.mapper.BusDataMapper;
import com.pulse.repository.bus.BusRidershipHourlyRepository;
import com.pulse.repository.bus.BusRouteRepository;
import com.pulse.repository.bus.BusStopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusStatisticsDataLoadServiceTest {

    @Mock
    private SeoulOpenDataPlazaClient apiClient;

    @Mock
    private BusDataMapper mapper;

    @Mock
    private BusRouteRepository busRouteRepository;

    @Mock
    private BusStopRepository busStopRepository;

    @Mock
    private BusRidershipHourlyRepository busRidershipRepository;

    @Mock
    private SeoulApiProperties properties;

    @InjectMocks
    private BusStatisticsDataLoadService service;

    private static final int PAGE_SIZE = 1000;
    private static final String TEST_YEAR_MONTH = "202510";

    @BeforeEach
    void setUp() {
        when(properties.getPageSize()).thenReturn(PAGE_SIZE);
    }

    @Test
    void shouldLoadBusStatisticsDataSuccessfully_WhenSinglePageApiResponse() {
        // Given
        BusRoute route1 = BusRoute.of("100", "100번", "01", "간선");
        BusStop stop1 = BusStop.of("STOP_001", "ARS_001", "강남역");
        BusStop stop2 = BusStop.of("STOP_002", "ARS_002", "역삼역");

        BusApiResponse response = createBusApiResponse(Arrays.asList(
                createBusRidershipData("100", "STOP_001", "강남역"),
                createBusRidershipData("100", "STOP_002", "역삼역")
        ));

        // API 클라이언트 Mock
        when(apiClient.fetchBusRidershipData(TEST_YEAR_MONTH, 1, PAGE_SIZE)).thenReturn(response);
        when(apiClient.fetchBusRidershipData(TEST_YEAR_MONTH, PAGE_SIZE + 1, PAGE_SIZE * 2))
                .thenReturn(null);

        // 마스터 데이터 캐시 Mock
        when(busRouteRepository.findAll()).thenReturn(List.of(route1));
        when(busStopRepository.findAll()).thenReturn(Arrays.asList(stop1, stop2));

        // Mapper Mock
        when(mapper.toBusRidershipHourlyList(any(), any(), any())).thenAnswer(inv -> {
            BusRoute route = inv.getArgument(1);
            BusStop stop = inv.getArgument(2);
            return createHourlyRidershipList(route, stop);
        });

        // Repository Mock
        when(busRidershipRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        DataLoadResult result = service.loadBusStatisticsData(TEST_YEAR_MONTH);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(48, result.getTotalCount());
    }

    @Test
    void shouldConvertApiRecordsToHourlyRecords_WhenProcessingStatistics() {
        // Given
        BusRoute route1 = BusRoute.of("100", "100번", "01", "간선");
        BusStop stop1 = BusStop.of("STOP_001", "ARS_001", "강남역");

        BusApiResponse response = createBusApiResponse(List.of(
                createBusRidershipData("100", "STOP_001", "강남역")
        ));

        // API 클라이언트 Mock
        when(apiClient.fetchBusRidershipData(TEST_YEAR_MONTH, 1, PAGE_SIZE)).thenReturn(response);
        when(apiClient.fetchBusRidershipData(TEST_YEAR_MONTH, PAGE_SIZE + 1, PAGE_SIZE * 2))
                .thenReturn(null);

        // 마스터 데이터 캐시 Mock
        when(busRouteRepository.findAll()).thenReturn(List.of(route1));
        when(busStopRepository.findAll()).thenReturn(List.of(stop1));

        // Mapper Mock - 1개 API 레코드
        when(mapper.toBusRidershipHourlyList(any(), any(), any())).thenAnswer(inv -> {
            BusRoute route = inv.getArgument(1);
            BusStop stop = inv.getArgument(2);
            return createHourlyRidershipList(route, stop);
        });

        // Repository Mock
        when(busRidershipRepository.saveAll(any())).thenAnswer(inv -> {
            List<?> hourlyRecords = inv.getArgument(0);
            assertEquals(24, hourlyRecords.size());
            return hourlyRecords;
        });

        // When
        DataLoadResult result = service.loadBusStatisticsData(TEST_YEAR_MONTH);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(24, result.getTotalCount());
    }

    private BusApiResponse createBusApiResponse(List<BusRidershipData> records) {
        BusApiResponse response = new BusApiResponse();
        BusApiResponse.BusApiData apiData = new BusApiResponse.BusApiData();
        apiData.setListTotalCount(records.size());
        apiData.setRow(records);
        response.setCardBusTimeNew(apiData);
        return response;
    }

    private BusRidershipData createBusRidershipData(String routeNo, String stopId, String stopName) {
        BusRidershipData data = new BusRidershipData();
        data.setUseYm(TEST_YEAR_MONTH);
        data.setRteNo(routeNo);
        data.setRteNm(routeNo + "번");
        data.setStopsId(stopId);
        data.setStopsArsNo("ARS_" + stopId);
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

        data.setHr0GetOffTnope(50.0);
        data.setHr1GetOffNope(50.0);
        data.setHr2GetOffTnope(50.0);
        data.setHr3GetOffTnope(50.0);
        data.setHr4GetOffTnope(50.0);
        data.setHr5GetOffTnope(50.0);
        data.setHr6GetOffTnope(50.0);
        data.setHr7GetOffTnope(50.0);
        data.setHr8GetOffTnope(50.0);
        data.setHr9GetOffTnope(50.0);
        data.setHr10GetOffTnope(50.0);
        data.setHr11GetOffTnope(50.0);
        data.setHr12GetOffTnope(50.0);
        data.setHr13GetOffTnope(50.0);
        data.setHr14GetOffTnope(50.0);
        data.setHr15GetOffTnope(50.0);
        data.setHr16GetOffTnope(50.0);
        data.setHr17GetOffTnope(50.0);
        data.setHr18GetOffTnope(50.0);
        data.setHr19GetOffTnope(50.0);
        data.setHr20GetOffTnope(50.0);
        data.setHr21GetOffTnope(50.0);
        data.setHr22GetOffTnope(50.0);
        data.setHr23GetOffTnope(50.0);

        return data;
    }

    private List<BusRidershipHourly> createHourlyRidershipList(
            BusRoute route,
            BusStop stop
    ) {
        List<BusRidershipHourly> result = new java.util.ArrayList<>();
        LocalDate statDate = LocalDate.of(2025, 10, 1);

        for (byte hour = 0; hour < 24; hour++) {
            result.add(BusRidershipHourly.of(statDate, route, stop, hour, 100, 50));
        }

        return result;
    }
}

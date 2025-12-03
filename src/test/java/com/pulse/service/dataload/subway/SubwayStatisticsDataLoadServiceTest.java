package com.pulse.service.dataload.subway;

import com.pulse.api.seoulopendataplaza.SeoulOpenDataPlazaClient;
import com.pulse.api.seoulopendataplaza.dto.subway.SubwayApiResponse;
import com.pulse.api.seoulopendataplaza.dto.subway.SubwayRidershipData;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayRidershipHourly;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.mapper.SubwayDataMapper;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayRidershipHourlyRepository;
import com.pulse.repository.subway.SubwayStationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubwayStatisticsDataLoadServiceTest {

    @Mock
    private SeoulOpenDataPlazaClient apiClient;

    @Mock
    private SubwayDataMapper mapper;

    @Mock
    private SubwayLineRepository subwayLineRepository;

    @Mock
    private SubwayStationRepository subwayStationRepository;

    @Mock
    private SubwayRidershipHourlyRepository subwayRidershipRepository;

    @InjectMocks
    private SubwayStatisticsDataLoadService service;

    private static final int PAGE_SIZE = 1000;
    private static final String TEST_YEAR_MONTH = "202510";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", PAGE_SIZE);
    }

    @Test
    void shouldLoadSubwayStatisticsDataSuccessfully_WhenSinglePageApiResponse() {
        // Given
        SubwayLine line1 = SubwayLine.of("2호선");
        SubwayStation station1 = SubwayStation.of("강남역");
        SubwayStation station2 = SubwayStation.of("역삼역");

        SubwayApiResponse response = createSubwayApiResponse(Arrays.asList(
                createSubwayRidershipData("2호선", "강남역"),
                createSubwayRidershipData("2호선", "역삼역")
        ));

        // API 클라이언트 Mock
        when(apiClient.fetchSubwayRidershipData(TEST_YEAR_MONTH, 1, PAGE_SIZE)).thenReturn(response);
        when(apiClient.fetchSubwayRidershipData(TEST_YEAR_MONTH, PAGE_SIZE + 1, PAGE_SIZE * 2))
                .thenReturn(null);

        // 마스터 데이터 캐시 Mock
        when(subwayLineRepository.findAll()).thenReturn(List.of(line1));
        when(subwayStationRepository.findAll()).thenReturn(Arrays.asList(station1, station2));

        // Mapper Mock
        when(mapper.toSubwayRidershipHourlyList(any(), any(), any())).thenAnswer(inv -> {
            SubwayLine line = inv.getArgument(1);
            SubwayStation station = inv.getArgument(2);
            return createHourlyRidershipList(line, station);
        });

        // Repository Mock
        when(subwayRidershipRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        DataLoadResult result = service.loadSubwayStatisticsData(TEST_YEAR_MONTH);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(48, result.getTotalCount());
    }

    @Test
    void shouldConvertApiRecordsToHourlyRecords_WhenProcessingStatistics() {
        // Given
        SubwayLine line1 = SubwayLine.of("2호선");
        SubwayStation station1 = SubwayStation.of("강남역");

        SubwayApiResponse response = createSubwayApiResponse(List.of(
                createSubwayRidershipData("2호선", "강남역")
        ));

        // API 클라이언트 Mock
        when(apiClient.fetchSubwayRidershipData(TEST_YEAR_MONTH, 1, PAGE_SIZE)).thenReturn(response);
        when(apiClient.fetchSubwayRidershipData(TEST_YEAR_MONTH, PAGE_SIZE + 1, PAGE_SIZE * 2))
                .thenReturn(null);

        // 마스터 데이터 캐시 Mock
        when(subwayLineRepository.findAll()).thenReturn(Arrays.asList(line1));
        when(subwayStationRepository.findAll()).thenReturn(Arrays.asList(station1));

        // Mapper Mock - 1개 API 레코드 → 24개 시간별 레코드
        when(mapper.toSubwayRidershipHourlyList(any(), any(), any())).thenAnswer(inv -> {
            SubwayLine line = inv.getArgument(1);
            SubwayStation station = inv.getArgument(2);
            return createHourlyRidershipList(line, station);
        });

        // Repository Mock - 24개의 시간별 레코드가 저장되는지 검증
        when(subwayRidershipRepository.saveAll(any())).thenAnswer(inv -> {
            List<?> hourlyRecords = inv.getArgument(0);
            assertEquals(24, hourlyRecords.size());
            return hourlyRecords;
        });

        // When
        DataLoadResult result = service.loadSubwayStatisticsData(TEST_YEAR_MONTH);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(24, result.getTotalCount());
    }

    private SubwayApiResponse createSubwayApiResponse(List<SubwayRidershipData> records) {
        SubwayApiResponse response = new SubwayApiResponse();
        SubwayApiResponse.SubwayApiData apiData = new SubwayApiResponse.SubwayApiData();
        apiData.setListTotalCount(records.size());
        apiData.setRow(records);
        response.setCardSubwayTime(apiData);
        return response;
    }

    private SubwayRidershipData createSubwayRidershipData(String lineName, String stationName) {
        SubwayRidershipData data = new SubwayRidershipData();
        data.setUseMm(TEST_YEAR_MONTH);
        data.setSbwyRoutLnNm(lineName);
        data.setSttn(stationName);
        data.setJobYmd("20251001");

        data.setHr0GetOnNope(100.0);
        data.setHr1GetOnNope(100.0);
        data.setHr2GetOnNope(100.0);
        data.setHr3GetOnNope(100.0);
        data.setHr4GetOnNope(100.0);
        data.setHr5GetOnNope(100.0);
        data.setHr6GetOnNope(100.0);
        data.setHr7GetOnNope(100.0);
        data.setHr8GetOnNope(100.0);
        data.setHr9GetOnNope(100.0);
        data.setHr10GetOnNope(100.0);
        data.setHr11GetOnNope(100.0);
        data.setHr12GetOnNope(100.0);
        data.setHr13GetOnNope(100.0);
        data.setHr14GetOnNope(100.0);
        data.setHr15GetOnNope(100.0);
        data.setHr16GetOnNope(100.0);
        data.setHr17GetOnNope(100.0);
        data.setHr18GetOnNope(100.0);
        data.setHr19GetOnNope(100.0);
        data.setHr20GetOnNope(100.0);
        data.setHr21GetOnNope(100.0);
        data.setHr22GetOnNope(100.0);
        data.setHr23GetOnNope(100.0);

        data.setHr0GetOffNope(50.0);
        data.setHr1GetOffNope(50.0);
        data.setHr2GetOffNope(50.0);
        data.setHr3GetOffNope(50.0);
        data.setHr4GetOffNope(50.0);
        data.setHr5GetOffNope(50.0);
        data.setHr6GetOffNope(50.0);
        data.setHr7GetOffNope(50.0);
        data.setHr8GetOffNope(50.0);
        data.setHr9GetOffNope(50.0);
        data.setHr10GetOffNope(50.0);
        data.setHr11GetOffNope(50.0);
        data.setHr12GetOffNope(50.0);
        data.setHr13GetOffNope(50.0);
        data.setHr14GetOffNope(50.0);
        data.setHr15GetOffNope(50.0);
        data.setHr16GetOffNope(50.0);
        data.setHr17GetOffNope(50.0);
        data.setHr18GetOffNope(50.0);
        data.setHr19GetOffNope(50.0);
        data.setHr20GetOffNope(50.0);
        data.setHr21GetOffNope(50.0);
        data.setHr22GetOffNope(50.0);
        data.setHr23GetOffNope(50.0);

        return data;
    }

    private List<SubwayRidershipHourly> createHourlyRidershipList(
            SubwayLine line,
            SubwayStation station
    ) {
        List<SubwayRidershipHourly> result = new java.util.ArrayList<>();
        LocalDate statDate = LocalDate.of(2025, 10, 1);

        for (byte hour = 0; hour < 24; hour++) {
            result.add(SubwayRidershipHourly.of(statDate, line, station, hour, 100, 50));
        }

        return result;
    }
}

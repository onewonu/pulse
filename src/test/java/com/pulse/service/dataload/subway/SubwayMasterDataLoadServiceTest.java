package com.pulse.service.dataload.subway;

import com.pulse.api.seoulopendata.SeoulOpenDataPlazaClient;
import com.pulse.api.seoulopendata.dto.subway.SubwayApiResponse;
import com.pulse.api.seoulopendata.dto.subway.SubwayRidershipData;
import com.pulse.config.SeoulApiProperties;
import com.pulse.dto.DataLoadResult;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.mapper.SubwayDataMapper;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayLineStationRepository;
import com.pulse.repository.subway.SubwayStationRepository;
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
class SubwayMasterDataLoadServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private SeoulOpenDataPlazaClient apiClient;

    @Mock
    private SubwayDataMapper mapper;

    @Mock
    private SubwayLineRepository subwayLineRepository;

    @Mock
    private SubwayStationRepository subwayStationRepository;

    @Mock
    private SubwayLineStationRepository subwayLineStationRepository;

    @Mock
    private SeoulApiProperties properties;

    @InjectMocks
    private SubwayMasterDataLoadService service;

    private static final int PAGE_SIZE = 1000;
    private static final String TEST_YEAR_MONTH = "202510";

    @BeforeEach
    void setUp() {
        when(properties.getPageSize()).thenReturn(PAGE_SIZE);
    }

    @Test
    void shouldLoadSubwayMasterDataSuccessfully_WhenSinglePageApiResponse() {
        // Given
        SubwayApiResponse response = createSubwayApiResponse(Arrays.asList(
                createSubwayRidershipData("1호선", "서울역"),
                createSubwayRidershipData("1호선", "시청역")
        ));

        // API 클라이언트 Mock
        when(apiClient.fetchSubwayRidershipData(TEST_YEAR_MONTH, 1, PAGE_SIZE)).thenReturn(response);

        when(apiClient.fetchSubwayRidershipData(TEST_YEAR_MONTH, PAGE_SIZE + 1, PAGE_SIZE * 2))
                .thenReturn(null);

        // Mapper Mock
        when(mapper.toSubwayLine(any())).thenAnswer(inv ->
            SubwayLine.of(inv.<SubwayRidershipData>getArgument(0).getSbwyRoutLnNm()));

        when(mapper.toSubwayStation(any())).thenAnswer(inv ->
            SubwayStation.of(inv.<SubwayRidershipData>getArgument(0).getSttn()));

        // Repository Mock
        when(subwayLineRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subwayStationRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subwayLineStationRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        DataLoadResult result = service.loadSubwayMasterData(TEST_YEAR_MONTH);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalCount());
    }

    @Test
    void shouldDeduplicateCorrectly_WhenSameLineAndStationAppearsMultipleTimes() {
        // Given
        SubwayApiResponse response = createSubwayApiResponse(Arrays.asList(
                createSubwayRidershipData("2호선", "강남역"),
                createSubwayRidershipData("2호선", "역삼역"),
                createSubwayRidershipData("2호선", "강남역"),
                createSubwayRidershipData("3호선", "강남역")
        ));

        // API 클라이언트 Mock
        when(apiClient.fetchSubwayRidershipData(TEST_YEAR_MONTH, 1, PAGE_SIZE)).thenReturn(response);

        when(apiClient.fetchSubwayRidershipData(TEST_YEAR_MONTH, PAGE_SIZE + 1, PAGE_SIZE * 2))
                .thenReturn(null);

        // Mapper Mock
        when(mapper.toSubwayLine(any())).thenAnswer(inv ->
                SubwayLine.of(inv.<SubwayRidershipData>getArgument(0).getSbwyRoutLnNm()));

        when(mapper.toSubwayStation(any())).thenAnswer(inv ->
                SubwayStation.of(inv.<SubwayRidershipData>getArgument(0).getSttn()));

        // Repository Mock: 2개의 노선, 2개의 역, 3개의 조합
        when(subwayLineRepository.saveAll(any())).thenAnswer(inv -> {
            List<SubwayLine> lines = inv.getArgument(0);
            assertEquals(2, lines.size());
            return lines;
        });

        when(subwayStationRepository.saveAll(any())).thenAnswer(inv -> {
            List<SubwayStation> stations = inv.getArgument(0);
            assertEquals(2, stations.size());
            return stations;
        });

        when(subwayLineStationRepository.saveAll(any())).thenAnswer(inv -> {
            List<?> lineStations = inv.getArgument(0);
            assertEquals(3, lineStations.size());
            return lineStations;
        });

        // When
        DataLoadResult result = service.loadSubwayMasterData(TEST_YEAR_MONTH);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(4, result.getTotalCount());
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

        data.setHr0GetOffNope(100.0);
        data.setHr1GetOffNope(100.0);
        data.setHr2GetOffNope(100.0);
        data.setHr3GetOffNope(100.0);
        data.setHr4GetOffNope(100.0);
        data.setHr5GetOffNope(100.0);
        data.setHr6GetOffNope(100.0);
        data.setHr7GetOffNope(100.0);
        data.setHr8GetOffNope(100.0);
        data.setHr9GetOffNope(100.0);
        data.setHr10GetOffNope(100.0);
        data.setHr11GetOffNope(100.0);
        data.setHr12GetOffNope(100.0);
        data.setHr13GetOffNope(100.0);
        data.setHr14GetOffNope(100.0);
        data.setHr15GetOffNope(100.0);
        data.setHr16GetOffNope(100.0);
        data.setHr17GetOffNope(100.0);
        data.setHr18GetOffNope(100.0);
        data.setHr19GetOffNope(100.0);
        data.setHr20GetOffNope(100.0);
        data.setHr21GetOffNope(100.0);
        data.setHr22GetOffNope(100.0);
        data.setHr23GetOffNope(100.0);

        return data;
    }
}

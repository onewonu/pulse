package com.pulse.service.search;

import com.pulse.api.odsay.OdsayClient;
import com.pulse.api.odsay.dto.OdsayStationSearchResponse;
import com.pulse.api.odsay.dto.StationData;
import com.pulse.dto.search.StationSearchResponse;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.repository.subway.SubwayLineRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StationSearchService")
class StationSearchServiceTest {

    @Mock
    private OdsayClient odsayClient;

    @Mock
    private SubwayLineRepository subwayLineRepository;

    @InjectMocks
    private StationSearchService stationSearchService;

    @Test
    @DisplayName("역 검색 - 정상적인 검색 결과 반환")
    void searchStation_Success() {
        // Given
        String stationName = "강남";

        StationData station1 = new StationData();
        station1.setStationName("강남역");
        station1.setStationID("1000");
        station1.setX(127.0276);
        station1.setY(37.4979);
        station1.setLaneName("2호선");

        OdsayStationSearchResponse.ResultData resultData = new OdsayStationSearchResponse.ResultData();
        resultData.setTotalCount(1);
        resultData.setStations(List.of(station1));

        OdsayStationSearchResponse response = new OdsayStationSearchResponse();
        response.setResult(resultData);

        SubwayLine line2 = SubwayLine.of("수도권 2호선", "#00A84D");

        when(odsayClient.searchStation(stationName)).thenReturn(response);
        when(subwayLineRepository.findById("수도권 2호선")).thenReturn(Optional.of(line2));

        // When
        StationSearchResponse result = stationSearchService.searchStation(stationName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.stations()).hasSize(1);

        StationSearchResponse.StationItem item = result.stations().getFirst();
        assertThat(item.stationName()).isEqualTo("강남역");
        assertThat(item.stationID()).isEqualTo("1000");
        assertThat(item.laneName()).isEqualTo("2호선");
        assertThat(item.lineColor()).isEqualTo("#00A84D");

        verify(odsayClient, times(1)).searchStation(stationName);
        verify(subwayLineRepository, times(1)).findById("수도권 2호선");
    }

    @Test
    @DisplayName("역 검색 - 결과 없음")
    void searchStation_NoResults() {
        // Given
        String stationName = "없는역";

        OdsayStationSearchResponse.ResultData resultData = new OdsayStationSearchResponse.ResultData();
        resultData.setTotalCount(0);
        resultData.setStations(List.of());

        OdsayStationSearchResponse response = new OdsayStationSearchResponse();
        response.setResult(resultData);

        when(odsayClient.searchStation(stationName)).thenReturn(response);

        // When
        StationSearchResponse result = stationSearchService.searchStation(stationName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalCount()).isZero();
        assertThat(result.stations()).isEmpty();
    }

    @Test
    @DisplayName("역 검색 - 역 이름이 null인 경우 예외 발생")
    void searchStation_NullStationName() {
        // When & Then
        assertThatThrownBy(() -> stationSearchService.searchStation(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stationName must be at least 2 characters");
    }

    @Test
    @DisplayName("역 검색 - 역 이름이 너무 짧은 경우 예외 발생")
    void searchStation_TooShortStationName() {
        // When & Then
        assertThatThrownBy(() -> stationSearchService.searchStation("강"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stationName must be at least 2 characters");
    }
}

package com.pulse.service.dataload.subway;

import com.pulse.api.seoulopendata.SeoulOpenDataClient;
import com.pulse.api.seoulopendata.dto.subway.SubwayApiResponse;
import com.pulse.api.seoulopendata.dto.subway.SubwayPassengerData;
import com.pulse.config.SeoulApiProperties;
import com.pulse.dto.dataload.DataLoadResponse;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayPassengerHourly;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.mapper.SubwayDataMapper;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayStationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubwayStatisticsDataLoadService")
class SubwayStatisticsDataLoadServiceTest {

    @Mock
    private SeoulOpenDataClient seoulOpenDataClient;

    @Mock
    private SubwayDataMapper subwayDataMapper;

    @Mock
    private SubwayLineRepository subwayLineRepository;

    @Mock
    private SubwayStationRepository subwayStationRepository;

    @Mock
    private SubwayPassengerHourlyService subwayPassengerHourlyService;

    @Mock
    private SeoulApiProperties seoulApiProperties;

    @InjectMocks
    private SubwayStatisticsDataLoadService service;

    @Test
    @DisplayName("통계 데이터 로딩 성공")
    void loadStatisticsData_Success() {
        // Given
        when(seoulApiProperties.getPageSize()).thenReturn(1000);

        SubwayLine line = SubwayLine.of("수도권 1호선", "#003DA5");
        when(subwayLineRepository.findAll()).thenReturn(List.of(line));

        SubwayStation station = SubwayStation.of(
                "426",
                "서울역",
                line,
                37.554648,
                126.972559
        );
        when(subwayStationRepository.findAllWithLine()).thenReturn(List.of(station));

        SubwayPassengerData apiData = new SubwayPassengerData();
        apiData.setSbwyRoutLnNm("1호선");
        apiData.setSttn("서울역");
        apiData.setUseMm("202401");
        apiData.setJobYmd("20240115");

        SubwayApiResponse.SubwayApiData apiDataWrapper = new SubwayApiResponse.SubwayApiData();
        apiDataWrapper.setRow(List.of(apiData));

        SubwayApiResponse apiResponse = new SubwayApiResponse();
        apiResponse.setCardSubwayTime(apiDataWrapper);

        when(seoulOpenDataClient.fetchSubwayPassengerData(eq("202401"), anyInt(), anyInt()))
                .thenReturn(apiResponse)
                .thenReturn(null);

        SubwayPassengerHourly hourly = SubwayPassengerHourly.of(
                LocalDate.of(2024, 1, 15),
                station,
                (byte) 0,
                100,
                50
        );
        when(subwayDataMapper.toSubwayPassengerHourlyList(any(SubwayPassengerData.class), any(SubwayStation.class)))
                .thenReturn(List.of(hourly));

        when(subwayPassengerHourlyService.savePassengerData(any())).thenReturn(1);

        // When
        DataLoadResponse result = service.loadSubwayStatisticsData("202401");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        verify(subwayLineRepository, times(1)).findAll();
        verify(subwayStationRepository, times(1)).findAllWithLine();
        verify(seoulOpenDataClient, atLeastOnce()).fetchSubwayPassengerData(eq("202401"), anyInt(), anyInt());
        verify(subwayPassengerHourlyService, times(1)).savePassengerData(any());
    }

}

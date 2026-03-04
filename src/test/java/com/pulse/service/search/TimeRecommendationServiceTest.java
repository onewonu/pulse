package com.pulse.service.search;

import com.pulse.api.odsay.OdsayClient;
import com.pulse.api.odsay.dto.OdsaySubwayScheduleResponse;
import com.pulse.dto.TimeRecommendationRequest;
import com.pulse.dto.TimeRecommendationResult;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayPassengerHourly;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.exception.search.IncompleteCongestionDataException;
import com.pulse.exception.search.NoSchedulesAvailableException;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayPassengerHourlyRepository;
import com.pulse.repository.subway.SubwayTrainScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimeRecommendationService")
class TimeRecommendationServiceTest {

    @Mock
    private SubwayTrainScheduleRepository subwayTrainScheduleRepository;

    @Mock
    private SubwayPassengerHourlyRepository subwayPassengerHourlyRepository;

    @Mock
    private SubwayLineRepository subwayLineRepository;

    @Mock
    private OdsayClient odsayClient;

    @InjectMocks
    private TimeRecommendationService timeRecommendationService;

    @Test
    @DisplayName("시간 추천 성공 - 완전한 추천 결과 반환")
    void recommendTimes_Success() {
        // Given
        TimeRecommendationRequest request = new TimeRecommendationRequest(
                "1000",
                "2000",
                LocalDate.of(2024, 1, 15), // 월요일
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        // 출발 시간 데이터
        when(subwayTrainScheduleRepository.findDistinctDepartureTimesByStationIdAndDayAndTimeRange(
                "1000", "평일", LocalTime.of(9, 0), LocalTime.of(10, 0)))
                .thenReturn(List.of(LocalTime.of(9, 30)));

        // Odsay API 응답 생성
        OdsaySubwayScheduleResponse.StationInfoData station1 = new OdsaySubwayScheduleResponse.StationInfoData();
        station1.setStationID("1000");
        station1.setStationName("강남역");
        station1.setDepartureTime("09:30:00");
        station1.setArrivalTime("09:30:00");

        OdsaySubwayScheduleResponse.StationInfoData station2 = new OdsaySubwayScheduleResponse.StationInfoData();
        station2.setStationID("1001");
        station2.setStationName("역삼역");
        station2.setDepartureTime("09:32:00");
        station2.setArrivalTime("09:32:00");

        OdsaySubwayScheduleResponse.StationInfoData station3 = new OdsaySubwayScheduleResponse.StationInfoData();
        station3.setStationID("2000");
        station3.setStationName("선릉역");
        station3.setDepartureTime("09:35:00");
        station3.setArrivalTime("09:35:00");

        OdsaySubwayScheduleResponse.PassStopListData passStopList = new OdsaySubwayScheduleResponse.PassStopListData();
        passStopList.setStations(List.of(station1, station2, station3));

        OdsaySubwayScheduleResponse.SubPathData subPath = new OdsaySubwayScheduleResponse.SubPathData();
        subPath.setMovingType(1); // SUBWAY
        subPath.setLaneName("수도권 2호선");
        subPath.setPassStopList(passStopList);

        OdsaySubwayScheduleResponse.InfoData info = new OdsaySubwayScheduleResponse.InfoData();
        info.setDepartureTime("09:30:00");
        info.setArrivalTime("09:35:00");
        info.setTotalTime(5);
        info.setTransferCount(0);

        OdsaySubwayScheduleResponse.PathData path = new OdsaySubwayScheduleResponse.PathData();
        path.setPathType(1); // SHORTEST_TIME
        path.setInfo(info);
        path.setSubPath(List.of(subPath));

        OdsaySubwayScheduleResponse.ResultData resultData = new OdsaySubwayScheduleResponse.ResultData();
        resultData.setPath(List.of(path));

        OdsaySubwayScheduleResponse response = new OdsaySubwayScheduleResponse();
        response.setResult(resultData);

        when(odsayClient.searchSubwaySchedule(anyString(), anyString(), anyInt(), anyString())).thenReturn(response);

        // 호선 정보
        SubwayLine line2 = SubwayLine.of("수도권 2호선", "#00A84D");
        when(subwayLineRepository.findById("수도권 2호선")).thenReturn(Optional.of(line2));

        // 혼잡도 데이터
        SubwayStation station1Entity = SubwayStation.of("1000", "강남역", line2, 37.4979, 127.0276);
        SubwayStation station2Entity = SubwayStation.of("1001", "역삼역", line2, 37.5006, 127.0364);
        SubwayStation station3Entity = SubwayStation.of("2000", "선릉역", line2, 37.5045, 127.0493);

        SubwayPassengerHourly passenger1 = SubwayPassengerHourly.of(
                LocalDate.of(2024, 1, 15), station1Entity, (byte) 9, 1000, 800);
        SubwayPassengerHourly passenger2 = SubwayPassengerHourly.of(
                LocalDate.of(2024, 1, 15), station2Entity, (byte) 9, 500, 400);
        SubwayPassengerHourly passenger3 = SubwayPassengerHourly.of(
                LocalDate.of(2024, 1, 15), station3Entity, (byte) 9, 600, 500);

        when(subwayPassengerHourlyRepository.findByStationIdsAndHourSlot(
                anyList(), eq((byte) 9)))
                .thenReturn(List.of(passenger1, passenger2, passenger3));

        // When
        TimeRecommendationResult result = timeRecommendationService.recommendTimes(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.departureStationId()).isEqualTo("1000");
        assertThat(result.arrivalStationId()).isEqualTo("2000");
        assertThat(result.recommendations()).hasSize(1);

        TimeRecommendationResult.TimeRecommendation recommendation = result.recommendations().getFirst();
        assertThat(recommendation.departureTime()).isEqualTo(LocalTime.of(9, 30, 0));
        assertThat(recommendation.arrivalTime()).isEqualTo(LocalTime.of(9, 35, 0));
        assertThat(recommendation.totalTime()).isEqualTo(5);
        assertThat(recommendation.transferCount()).isZero();
        assertThat(recommendation.stationCongestions()).hasSize(3);

        verify(subwayTrainScheduleRepository, times(1))
                .findDistinctDepartureTimesByStationIdAndDayAndTimeRange("1000", "평일",
                        LocalTime.of(9, 0), LocalTime.of(10, 0));
        verify(odsayClient, times(1)).searchSubwaySchedule(anyString(), anyString(), anyInt(), anyString());
        verify(subwayLineRepository, times(1)).findById("수도권 2호선");
        verify(subwayPassengerHourlyRepository, times(1))
                .findByStationIdsAndHourSlot(anyList(), eq((byte) 9));
    }

    @Test
    @DisplayName("시간 추천 실패 - 스케줄 없음")
    void recommendTimes_NoSchedulesAvailable() {
        // Given
        TimeRecommendationRequest request = new TimeRecommendationRequest(
                "1000",
                "2000",
                LocalDate.of(2024, 1, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        // Odsay API 응답 생성 (fetchRouteTemplate 호출용)
        OdsaySubwayScheduleResponse.StationInfoData station1 = new OdsaySubwayScheduleResponse.StationInfoData();
        station1.setStationID("1000");
        station1.setStationName("강남역");
        station1.setDepartureTime("09:00:00");
        station1.setArrivalTime("09:00:00");

        OdsaySubwayScheduleResponse.PassStopListData passStopList = new OdsaySubwayScheduleResponse.PassStopListData();
        passStopList.setStations(List.of(station1));

        OdsaySubwayScheduleResponse.SubPathData subPath = new OdsaySubwayScheduleResponse.SubPathData();
        subPath.setMovingType(1);
        subPath.setLaneName("수도권 2호선");
        subPath.setPassStopList(passStopList);

        OdsaySubwayScheduleResponse.InfoData info = new OdsaySubwayScheduleResponse.InfoData();
        info.setDepartureTime("09:00:00");
        info.setArrivalTime("09:05:00");
        info.setTotalTime(5);
        info.setTransferCount(0);

        OdsaySubwayScheduleResponse.PathData path = new OdsaySubwayScheduleResponse.PathData();
        path.setPathType(1);
        path.setInfo(info);
        path.setSubPath(List.of(subPath));

        OdsaySubwayScheduleResponse.ResultData resultData = new OdsaySubwayScheduleResponse.ResultData();
        resultData.setPath(List.of(path));

        OdsaySubwayScheduleResponse response = new OdsaySubwayScheduleResponse();
        response.setResult(resultData);

        when(odsayClient.searchSubwaySchedule(anyString(), anyString(), anyInt(), anyString())).thenReturn(response);

        when(subwayTrainScheduleRepository.findDistinctDepartureTimesByStationIdAndDayAndTimeRange(
                anyString(), anyString(), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of());

        // When & Then
        assertThatThrownBy(() -> timeRecommendationService.recommendTimes(request))
                .isInstanceOf(NoSchedulesAvailableException.class)
                .hasMessageContaining("No train schedules found");

        verify(subwayTrainScheduleRepository, times(1))
                .findDistinctDepartureTimesByStationIdAndDayAndTimeRange(anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("시간 추천 실패 - 혼잡도 데이터 불완전")
    void recommendTimes_IncompleteCongestionData() {
        // Given
        TimeRecommendationRequest request = new TimeRecommendationRequest(
                "1000",
                "2000",
                LocalDate.of(2024, 1, 15),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        // Odsay API가 빈 경로 반환
        OdsaySubwayScheduleResponse.ResultData resultData = new OdsaySubwayScheduleResponse.ResultData();
        resultData.setPath(List.of());

        OdsaySubwayScheduleResponse response = new OdsaySubwayScheduleResponse();
        response.setResult(resultData);

        when(odsayClient.searchSubwaySchedule(anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(response);

        // When & Then
        assertThatThrownBy(() -> timeRecommendationService.recommendTimes(request))
                .isInstanceOf(IncompleteCongestionDataException.class)
                .hasMessageContaining("No route found between stations");
    }

    @Test
    @DisplayName("평일/주말 변환 확인 - 월요일은 평일")
    void recommendTimes_WeekdayConversion() {
        // Given
        TimeRecommendationRequest request = new TimeRecommendationRequest(
                "1000",
                "2000",
                LocalDate.of(2024, 1, 15), // 월요일
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        // Odsay API 응답 생성
        OdsaySubwayScheduleResponse.StationInfoData station1 = new OdsaySubwayScheduleResponse.StationInfoData();
        station1.setStationID("1000");
        station1.setStationName("강남역");
        station1.setDepartureTime("09:00:00");
        station1.setArrivalTime("09:00:00");

        OdsaySubwayScheduleResponse.PassStopListData passStopList = new OdsaySubwayScheduleResponse.PassStopListData();
        passStopList.setStations(List.of(station1));

        OdsaySubwayScheduleResponse.SubPathData subPath = new OdsaySubwayScheduleResponse.SubPathData();
        subPath.setMovingType(1);
        subPath.setLaneName("수도권 2호선");
        subPath.setPassStopList(passStopList);

        OdsaySubwayScheduleResponse.InfoData info = new OdsaySubwayScheduleResponse.InfoData();
        info.setDepartureTime("09:00:00");
        info.setArrivalTime("09:05:00");
        info.setTotalTime(5);
        info.setTransferCount(0);

        OdsaySubwayScheduleResponse.PathData path = new OdsaySubwayScheduleResponse.PathData();
        path.setPathType(1);
        path.setInfo(info);
        path.setSubPath(List.of(subPath));

        OdsaySubwayScheduleResponse.ResultData resultData = new OdsaySubwayScheduleResponse.ResultData();
        resultData.setPath(List.of(path));

        OdsaySubwayScheduleResponse response = new OdsaySubwayScheduleResponse();
        response.setResult(resultData);

        when(odsayClient.searchSubwaySchedule(anyString(), anyString(), anyInt(), anyString())).thenReturn(response);

        when(subwayTrainScheduleRepository.findDistinctDepartureTimesByStationIdAndDayAndTimeRange(
                anyString(), eq("평일"), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of());

        // When & Then
        assertThatThrownBy(() -> timeRecommendationService.recommendTimes(request))
                .isInstanceOf(NoSchedulesAvailableException.class);

        verify(subwayTrainScheduleRepository, times(1))
                .findDistinctDepartureTimesByStationIdAndDayAndTimeRange("1000", "평일",
                        LocalTime.of(9, 0), LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("평일/주말 변환 확인 - 토요일은 주말")
    void recommendTimes_WeekendConversion() {
        // Given
        TimeRecommendationRequest request = new TimeRecommendationRequest(
                "1000",
                "2000",
                LocalDate.of(2024, 1, 13), // 토요일
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );

        // Odsay API 응답 생성
        OdsaySubwayScheduleResponse.StationInfoData station1 = new OdsaySubwayScheduleResponse.StationInfoData();
        station1.setStationID("1000");
        station1.setStationName("강남역");
        station1.setDepartureTime("09:00:00");
        station1.setArrivalTime("09:00:00");

        OdsaySubwayScheduleResponse.PassStopListData passStopList = new OdsaySubwayScheduleResponse.PassStopListData();
        passStopList.setStations(List.of(station1));

        OdsaySubwayScheduleResponse.SubPathData subPath = new OdsaySubwayScheduleResponse.SubPathData();
        subPath.setMovingType(1);
        subPath.setLaneName("수도권 2호선");
        subPath.setPassStopList(passStopList);

        OdsaySubwayScheduleResponse.InfoData info = new OdsaySubwayScheduleResponse.InfoData();
        info.setDepartureTime("09:00:00");
        info.setArrivalTime("09:05:00");
        info.setTotalTime(5);
        info.setTransferCount(0);

        OdsaySubwayScheduleResponse.PathData path = new OdsaySubwayScheduleResponse.PathData();
        path.setPathType(1);
        path.setInfo(info);
        path.setSubPath(List.of(subPath));

        OdsaySubwayScheduleResponse.ResultData resultData = new OdsaySubwayScheduleResponse.ResultData();
        resultData.setPath(List.of(path));

        OdsaySubwayScheduleResponse response = new OdsaySubwayScheduleResponse();
        response.setResult(resultData);

        when(odsayClient.searchSubwaySchedule(anyString(), anyString(), anyInt(), anyString())).thenReturn(response);

        when(subwayTrainScheduleRepository.findDistinctDepartureTimesByStationIdAndDayAndTimeRange(
                anyString(), eq("주말"), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of());

        // When & Then
        assertThatThrownBy(() -> timeRecommendationService.recommendTimes(request))
                .isInstanceOf(NoSchedulesAvailableException.class);

        verify(subwayTrainScheduleRepository, times(1))
                .findDistinctDepartureTimesByStationIdAndDayAndTimeRange("1000", "주말",
                        LocalTime.of(9, 0), LocalTime.of(10, 0));
    }
}

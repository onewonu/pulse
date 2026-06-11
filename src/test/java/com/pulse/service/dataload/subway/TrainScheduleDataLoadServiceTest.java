package com.pulse.service.dataload.subway;

import com.pulse.api.seoulmetro.SeoulMetroClient;
import com.pulse.dto.dataload.DataLoadResponse;
import com.pulse.mapper.TrainScheduleMapper;
import com.pulse.repository.subway.SubwayStationRepository;
import com.pulse.repository.subway.SubwayTrainScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainScheduleDataLoadService")
class TrainScheduleDataLoadServiceTest {

    @Mock
    private SeoulMetroClient seoulMetroClient;

    @Mock
    private SubwayTrainScheduleSaveService subwayTrainScheduleSaveService;

    @Mock
    private SubwayStationRepository subwayStationRepository;

    @Mock
    private SubwayTrainScheduleRepository subwayTrainScheduleRepository;

    @Mock
    private TrainScheduleMapper trainScheduleMapper;

    @InjectMocks
    private TrainScheduleDataLoadService service;

    @Test
    @DisplayName("스케줄 데이터 로딩 성공 - Repository 호출 확인")
    void loadScheduleData_Success() {
        // Given
        when(subwayStationRepository.findAll()).thenReturn(List.of());

        // When
        DataLoadResponse result = service.loadTrainSchedules("평일");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        verify(subwayStationRepository, times(1)).findAll();
        verify(subwayTrainScheduleSaveService, times(1)).deleteByDayType("평일");
        verify(subwayTrainScheduleSaveService, never()).saveBatch(any());
    }

    @Test
    @DisplayName("모든 스케줄 삭제 성공")
    void deleteAllSchedules_Success() {
        // Given
        when(subwayTrainScheduleRepository.count()).thenReturn(100L);

        // When
        DataLoadResponse result = service.deleteAllTrainSchedules();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        verify(subwayTrainScheduleRepository, times(1)).count();
        verify(subwayTrainScheduleRepository, times(1)).deleteAll();
    }
}

package com.pulse.service.dataload.subway;

import com.pulse.dto.dataload.DataLoadResponse;
import com.pulse.repository.subway.SubwayPassengerHourlyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubwayPassengerHourlyService")
class SubwayPassengerHourlyServiceTest {

    @Mock
    private SubwayPassengerHourlyRepository subwayPassengerHourlyRepository;

    @InjectMocks
    private SubwayPassengerHourlyService service;

    @Test
    @DisplayName("통계 데이터 삭제 성공")
    void deleteByYearMonth_Success() {
        // Given
        String yearMonth = "202401";
        when(subwayPassengerHourlyRepository.deleteByYearMonth(yearMonth)).thenReturn(100);

        // When
        DataLoadResponse result = service.deleteByYearMonth(yearMonth);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        verify(subwayPassengerHourlyRepository, times(1)).deleteByYearMonth(yearMonth);
    }
}

package com.pulse.service.dataload.subway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.config.MasterDataProperties;
import com.pulse.dto.DataLoadResult;
import com.pulse.dto.masterdata.LinesData;
import com.pulse.dto.masterdata.StationExportData;
import com.pulse.dto.masterdata.StationMasterData;
import com.pulse.dto.masterdata.StationSearchResult;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayStationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubwayMasterDataLoadService")
class SubwayMasterDataLoadServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private SubwayLineRepository subwayLineRepository;

    @Mock
    private SubwayStationRepository subwayStationRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private MasterDataProperties masterDataProperties;

    @InjectMocks
    private SubwayMasterDataLoadService service;

    @Test
    @DisplayName("마스터 데이터 로딩 성공")
    void loadMasterData_Success() throws Exception {
        // Given
        when(masterDataProperties.getLinesPath()).thenReturn("file:/path/to/lines.json");
        when(masterDataProperties.getStationsPath()).thenReturn("file:/path/to/stations.json");

        Resource linesResource = mock(Resource.class);
        Resource stationsResource = mock(Resource.class);
        when(resourceLoader.getResource(anyString())).thenReturn(linesResource, stationsResource);
        when(linesResource.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));
        when(stationsResource.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));

        LinesData.LineInfo lineInfo = new LinesData.LineInfo("수도권 1호선", "#003DA5");
        LinesData linesData = new LinesData(null, null, List.of(lineInfo));

        StationMasterData stationData = new StationMasterData("서울역", "426", "126.972559", "37.554648", "수도권 1호선");

        StationSearchResult searchResult = new StationSearchResult("서울역", 1, List.of(stationData));

        StationExportData stationExportData = new StationExportData(null, null, null, List.of(searchResult));

        when(objectMapper.readValue(any(InputStream.class), eq(LinesData.class))).thenReturn(linesData);
        when(objectMapper.readValue(any(InputStream.class), eq(StationExportData.class))).thenReturn(stationExportData);

        SubwayLine mockLine = SubwayLine.of("수도권 1호선", "#003DA5");
        when(subwayLineRepository.findById("수도권 1호선")).thenReturn(Optional.of(mockLine));
        when(subwayLineRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subwayStationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        DataLoadResult result = service.loadMasterDataFromJson();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        verify(subwayStationRepository, times(1)).deleteAll();
        verify(subwayLineRepository, times(1)).deleteAll();
        verify(subwayLineRepository, times(1)).saveAll(argThat(lines -> ((List<?>) lines).size() == 1));
        verify(subwayStationRepository, times(1)).saveAll(argThat(stations -> ((List<?>) stations).size() == 1));
    }
}

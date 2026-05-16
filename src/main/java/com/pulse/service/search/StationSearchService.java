package com.pulse.service.search;

import com.pulse.api.odsay.OdsayClient;
import com.pulse.api.odsay.dto.OdsayStationSearchResponse;
import com.pulse.api.odsay.dto.StationData;
import com.pulse.dto.search.StationSearchResponse;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.util.LineNameNormalizer;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class StationSearchService {

    private final OdsayClient odsayClient;
    private final SubwayLineRepository subwayLineRepository;

    public StationSearchService(OdsayClient odsayClient, SubwayLineRepository subwayLineRepository) {
        this.odsayClient = odsayClient;
        this.subwayLineRepository = subwayLineRepository;
    }

    public StationSearchResponse searchStation(String stationName) {
        if (stationName == null || stationName.trim().length() < 2) {
            throw new IllegalArgumentException("stationName must be at least 2 characters");
        }

        OdsayStationSearchResponse response = odsayClient.searchStation(stationName);

        OdsayStationSearchResponse.ResultData result = response.getResult();
        if (result == null || result.getStations() == null || result.getStations().isEmpty()) {
            return new StationSearchResponse(0, Collections.emptyList());
        }

        List<StationSearchResponse.StationItem> stations = result.getStations().stream()
                .map(this::mapToStationItem)
                .toList();

        return new StationSearchResponse(result.getTotalCount(), stations);
    }

    private StationSearchResponse.StationItem mapToStationItem(StationData data) {
        String laneName = data.getLaneName();

        String lineColor = Optional.ofNullable(laneName)
                .map(LineNameNormalizer::normalize)
                .flatMap(subwayLineRepository::findById)
                .map(SubwayLine::getColor)
                .orElse(null);

        return new StationSearchResponse.StationItem(
                data.getStationName(),
                data.getStationID(),
                data.getX(),
                data.getY(),
                laneName,
                lineColor
        );
    }
}

package com.pulse.service.search;

import com.pulse.api.odsay.OdsayClient;
import com.pulse.api.odsay.dto.OdsayStationSearchResponse;
import com.pulse.api.odsay.dto.StationData;
import com.pulse.dto.StationSearchResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class StationSearchService {

    private final OdsayClient odsayClient;

    public StationSearchService(OdsayClient odsayClient) {
        this.odsayClient = odsayClient;
    }

    public StationSearchResult searchStation(String stationName) {
        if (stationName == null || stationName.trim().length() < 2) {
            throw new IllegalArgumentException("stationName must be at least 2 characters");
        }

        OdsayStationSearchResponse response = odsayClient.searchStation(stationName);

        OdsayStationSearchResponse.ResultData result = response.getResult();
        if (result == null || result.getStations() == null || result.getStations().isEmpty()) {
            return new StationSearchResult(0, Collections.emptyList());
        }

        List<StationSearchResult.StationItem> stations = new ArrayList<>();
        for (StationData data : result.getStations()) {
            stations.add(mapToStationItem(data));
        }

        return new StationSearchResult(result.getTotalCount(), stations);
    }

    private StationSearchResult.StationItem mapToStationItem(StationData data) {
        return new StationSearchResult.StationItem(
                data.getStationName(),
                data.getStationID(),
                data.getX(),
                data.getY(),
                data.getLaneName()
        );
    }
}

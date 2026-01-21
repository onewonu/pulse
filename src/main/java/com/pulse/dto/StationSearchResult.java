package com.pulse.dto;

import java.util.List;

public class StationSearchResult {

    private final int totalCount;
    private final List<StationItem> stations;

    public StationSearchResult(int totalCount, List<StationItem> stations) {
        this.totalCount = totalCount;
        this.stations = stations;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public List<StationItem> getStations() {
        return stations;
    }

    public static class StationItem {

        private final String stationName;
        private final String stationID;
        private final Double x;
        private final Double y;
        private final String laneName;
        private final String lineColor;

        public StationItem(
                String stationName,
                String stationID,
                Double x,
                Double y,
                String laneName,
                String lineColor
        ) {
            this.stationName = stationName;
            this.stationID = stationID;
            this.x = x;
            this.y = y;
            this.laneName = laneName;
            this.lineColor = lineColor;
        }

        public String getStationName() {
            return stationName;
        }

        public String getStationID() {
            return stationID;
        }

        public Double getX() {
            return x;
        }

        public Double getY() {
            return y;
        }

        public String getLaneName() {
            return laneName;
        }

        public String getLineColor() {
            return lineColor;
        }
    }
}

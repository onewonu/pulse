package com.pulse.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TimeRecommendationResult {

    private Integer departureSID;
    private Integer arrivalEID;
    private String departureStationName;
    private String arrivalStationName;
    private LocalDate travelDate;
    private int dayCode;
    private List<TimeRecommendation> recommendations;
    private String message;

    public TimeRecommendationResult(
            Integer departureSID,
            Integer arrivalEID,
            String departureStationName,
            String arrivalStationName,
            LocalDate travelDate,
            int dayCode,
            List<TimeRecommendation> recommendations,
            String message
    ) {
        this.departureSID = departureSID;
        this.arrivalEID = arrivalEID;
        this.departureStationName = departureStationName;
        this.arrivalStationName = arrivalStationName;
        this.travelDate = travelDate;
        this.dayCode = dayCode;
        this.recommendations = recommendations;
        this.message = message;
    }

    public Integer getDepartureSID() {
        return departureSID;
    }

    public Integer getArrivalEID() {
        return arrivalEID;
    }

    public String getDepartureStationName() {
        return departureStationName;
    }

    public String getArrivalStationName() {
        return arrivalStationName;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public int getDayCode() {
        return dayCode;
    }

    public List<TimeRecommendation> getRecommendations() {
        return recommendations;
    }

    public String getMessage() {
        return message;
    }

    public void setDepartureSID(Integer departureSID) {
        this.departureSID = departureSID;
    }

    public void setArrivalEID(Integer arrivalEID) {
        this.arrivalEID = arrivalEID;
    }

    public void setDepartureStationName(String departureStationName) {
        this.departureStationName = departureStationName;
    }

    public void setArrivalStationName(String arrivalStationName) {
        this.arrivalStationName = arrivalStationName;
    }

    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }

    public void setDayCode(int dayCode) {
        this.dayCode = dayCode;
    }

    public void setRecommendations(List<TimeRecommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static class TimeRecommendation {

        private LocalTime departureTime;
        private LocalTime arrivalTime;
        private int totalTime;
        private int transferCount;
        private double congestionScore;
        private CongestionLevel congestionLevel;
        private List<StationCongestion> stationCongestions;

        public TimeRecommendation() {
        }

        public TimeRecommendation(
                LocalTime departureTime,
                LocalTime arrivalTime,
                int totalTime,
                int transferCount,
                double congestionScore,
                CongestionLevel congestionLevel,
                List<StationCongestion> stationCongestions
        ) {
            this.departureTime = departureTime;
            this.arrivalTime = arrivalTime;
            this.totalTime = totalTime;
            this.transferCount = transferCount;
            this.congestionScore = congestionScore;
            this.congestionLevel = congestionLevel;
            this.stationCongestions = stationCongestions;
        }

        public LocalTime getDepartureTime() {
            return departureTime;
        }

        public LocalTime getArrivalTime() {
            return arrivalTime;
        }

        public int getTotalTime() {
            return totalTime;
        }

        public int getTransferCount() {
            return transferCount;
        }

        public double getCongestionScore() {
            return congestionScore;
        }

        public CongestionLevel getCongestionLevel() {
            return congestionLevel;
        }

        public List<StationCongestion> getStationCongestions() {
            return stationCongestions;
        }

        public void setDepartureTime(LocalTime departureTime) {
            this.departureTime = departureTime;
        }

        public void setArrivalTime(LocalTime arrivalTime) {
            this.arrivalTime = arrivalTime;
        }

        public void setTotalTime(int totalTime) {
            this.totalTime = totalTime;
        }

        public void setTransferCount(int transferCount) {
            this.transferCount = transferCount;
        }

        public void setCongestionScore(double congestionScore) {
            this.congestionScore = congestionScore;
        }

        public void setCongestionLevel(CongestionLevel congestionLevel) {
            this.congestionLevel = congestionLevel;
        }

        public void setStationCongestions(List<StationCongestion> stationCongestions) {
            this.stationCongestions = stationCongestions;
        }
    }

    public static class StationCongestion {

        private String stationId;
        private String stationName;
        private LocalTime arrivalTime;
        private LocalTime departureTime;
        private Integer boardingCount;
        private Integer alightingCount;
        private Integer totalPassengers;

        public StationCongestion() {
        }

        public StationCongestion(
                String stationId,
                String stationName,
                LocalTime arrivalTime,
                LocalTime departureTime,
                Integer boardingCount,
                Integer alightingCount,
                Integer totalPassengers
        ) {
            this.stationId = stationId;
            this.stationName = stationName;
            this.arrivalTime = arrivalTime;
            this.departureTime = departureTime;
            this.boardingCount = boardingCount;
            this.alightingCount = alightingCount;
            this.totalPassengers = totalPassengers;
        }

        public String getStationId() {
            return stationId;
        }

        public String getStationName() {
            return stationName;
        }

        public LocalTime getArrivalTime() {
            return arrivalTime;
        }

        public LocalTime getDepartureTime() {
            return departureTime;
        }

        public Integer getBoardingCount() {
            return boardingCount;
        }

        public Integer getAlightingCount() {
            return alightingCount;
        }

        public Integer getTotalPassengers() {
            return totalPassengers;
        }

        public void setStationId(String stationId) {
            this.stationId = stationId;
        }

        public void setStationName(String stationName) {
            this.stationName = stationName;
        }

        public void setArrivalTime(LocalTime arrivalTime) {
            this.arrivalTime = arrivalTime;
        }

        public void setDepartureTime(LocalTime departureTime) {
            this.departureTime = departureTime;
        }

        public void setBoardingCount(Integer boardingCount) {
            this.boardingCount = boardingCount;
        }

        public void setAlightingCount(Integer alightingCount) {
            this.alightingCount = alightingCount;
        }

        public void setTotalPassengers(Integer totalPassengers) {
            this.totalPassengers = totalPassengers;
        }
    }
}

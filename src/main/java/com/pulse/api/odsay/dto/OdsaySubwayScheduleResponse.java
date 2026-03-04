package com.pulse.api.odsay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OdsaySubwayScheduleResponse {

    @JsonProperty("result")
    private ResultData result;

    public ResultData getResult() {
        return result;
    }

    public void setResult(ResultData result) {
        this.result = result;
    }

    public static class ResultData {

        @JsonProperty("notificationCode")
        private Integer notificationCode;

        @JsonProperty("notificationMessage")
        private String notificationMessage;

        @JsonProperty("path")
        private List<PathData> path;

        @JsonProperty("error")
        private ErrorData error;

        public Integer getNotificationCode() {
            return notificationCode;
        }

        public String getNotificationMessage() {
            return notificationMessage;
        }

        public List<PathData> getPath() {
            return path;
        }

        public ErrorData getError() {
            return error;
        }

        public void setNotificationCode(Integer notificationCode) {
            this.notificationCode = notificationCode;
        }

        public void setNotificationMessage(String notificationMessage) {
            this.notificationMessage = notificationMessage;
        }

        public void setPath(List<PathData> path) {
            this.path = path;
        }

        public void setError(ErrorData error) {
            this.error = error;
        }
    }

    public static class ErrorData {

        @JsonProperty("code")
        private Integer code;

        @JsonProperty("msg")
        private String message;

        public Integer getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class PathData {

        @JsonProperty("pathType")
        private Integer pathType;

        @JsonProperty("info")
        private InfoData info;

        @JsonProperty("subPath")
        private List<SubPathData> subPath;

        public Integer getPathType() {
            return pathType;
        }

        public InfoData getInfo() {
            return info;
        }

        public List<SubPathData> getSubPath() {
            return subPath;
        }

        public void setPathType(Integer pathType) {
            this.pathType = pathType;
        }

        public void setInfo(InfoData info) {
            this.info = info;
        }

        public void setSubPath(List<SubPathData> subPath) {
            this.subPath = subPath;
        }
    }

    public static class InfoData {

        @JsonProperty("day")
        private Integer day;

        @JsonProperty("totalTime")
        private Integer totalTime;

        @JsonProperty("subwayTravelTime")
        private Integer subwayTravelTime;

        @JsonProperty("exchangeWalkTime")
        private Integer exchangeWalkTime;

        @JsonProperty("subwayTravelDistance")
        private Integer subwayTravelDistance;

        @JsonProperty("firstStartStationName")
        private String firstStartStationName;

        @JsonProperty("lastEndStationName")
        private String lastEndStationName;

        @JsonProperty("departureTime")
        private String departureTime;

        @JsonProperty("arrivalTime")
        private String arrivalTime;

        @JsonProperty("stationCount")
        private Integer stationCount;

        @JsonProperty("transferCount")
        private Integer transferCount;

        public Integer getDay() {
            return day;
        }

        public Integer getTotalTime() {
            return totalTime;
        }

        public Integer getSubwayTravelTime() {
            return subwayTravelTime;
        }

        public Integer getExchangeWalkTime() {
            return exchangeWalkTime;
        }

        public Integer getSubwayTravelDistance() {
            return subwayTravelDistance;
        }

        public String getFirstStartStationName() {
            return firstStartStationName;
        }

        public String getLastEndStationName() {
            return lastEndStationName;
        }

        public String getDepartureTime() {
            return departureTime;
        }

        public String getArrivalTime() {
            return arrivalTime;
        }

        public Integer getStationCount() {
            return stationCount;
        }

        public Integer getTransferCount() {
            return transferCount;
        }

        public void setDay(Integer day) {
            this.day = day;
        }

        public void setTotalTime(Integer totalTime) {
            this.totalTime = totalTime;
        }

        public void setSubwayTravelTime(Integer subwayTravelTime) {
            this.subwayTravelTime = subwayTravelTime;
        }

        public void setExchangeWalkTime(Integer exchangeWalkTime) {
            this.exchangeWalkTime = exchangeWalkTime;
        }

        public void setSubwayTravelDistance(Integer subwayTravelDistance) {
            this.subwayTravelDistance = subwayTravelDistance;
        }

        public void setFirstStartStationName(String firstStartStationName) {
            this.firstStartStationName = firstStartStationName;
        }

        public void setLastEndStationName(String lastEndStationName) {
            this.lastEndStationName = lastEndStationName;
        }

        public void setDepartureTime(String departureTime) {
            this.departureTime = departureTime;
        }

        public void setArrivalTime(String arrivalTime) {
            this.arrivalTime = arrivalTime;
        }

        public void setStationCount(Integer stationCount) {
            this.stationCount = stationCount;
        }

        public void setTransferCount(Integer transferCount) {
            this.transferCount = transferCount;
        }
    }

    public static class SubPathData {

        @JsonProperty("movingType")
        private Integer movingType;

        @JsonProperty("sectionTime")
        private Integer sectionTime;

        @JsonProperty("laneID")
        private Integer laneID;

        @JsonProperty("laneName")
        private String laneName;

        @JsonProperty("isExpressLane")
        private String isExpressLane;

        @JsonProperty("startName")
        private String startName;

        @JsonProperty("endName")
        private String endName;

        @JsonProperty("startID")
        private String startID;

        @JsonProperty("endID")
        private String endID;

        @JsonProperty("departureTime")
        private String departureTime;

        @JsonProperty("arrivalTime")
        private String arrivalTime;

        @JsonProperty("stopStationCount")
        private Integer stopStationCount;

        @JsonProperty("wayCode")
        private Integer wayCode;

        @JsonProperty("wayName")
        private String wayName;

        @JsonProperty("fastTrain")
        private Integer fastTrain;

        @JsonProperty("fastDoor")
        private Integer fastDoor;

        @JsonProperty("passStopList")
        private PassStopListData passStopList;

        public Integer getMovingType() {
            return movingType;
        }

        public Integer getSectionTime() {
            return sectionTime;
        }

        public Integer getLaneID() {
            return laneID;
        }

        public String getLaneName() {
            return laneName;
        }

        public String getIsExpressLane() {
            return isExpressLane;
        }

        public String getStartName() {
            return startName;
        }

        public String getEndName() {
            return endName;
        }

        public String getStartID() {
            return startID;
        }

        public String getEndID() {
            return endID;
        }

        public String getDepartureTime() {
            return departureTime;
        }

        public String getArrivalTime() {
            return arrivalTime;
        }

        public Integer getStopStationCount() {
            return stopStationCount;
        }

        public Integer getWayCode() {
            return wayCode;
        }

        public String getWayName() {
            return wayName;
        }

        public Integer getFastTrain() {
            return fastTrain;
        }

        public Integer getFastDoor() {
            return fastDoor;
        }

        public PassStopListData getPassStopList() {
            return passStopList;
        }

        public void setMovingType(Integer movingType) {
            this.movingType = movingType;
        }

        public void setSectionTime(Integer sectionTime) {
            this.sectionTime = sectionTime;
        }

        public void setLaneID(Integer laneID) {
            this.laneID = laneID;
        }

        public void setLaneName(String laneName) {
            this.laneName = laneName;
        }

        public void setIsExpressLane(String isExpressLane) {
            this.isExpressLane = isExpressLane;
        }

        public void setStartName(String startName) {
            this.startName = startName;
        }

        public void setEndName(String endName) {
            this.endName = endName;
        }

        public void setStartID(String startID) {
            this.startID = startID;
        }

        public void setEndID(String endID) {
            this.endID = endID;
        }

        public void setDepartureTime(String departureTime) {
            this.departureTime = departureTime;
        }

        public void setArrivalTime(String arrivalTime) {
            this.arrivalTime = arrivalTime;
        }

        public void setStopStationCount(Integer stopStationCount) {
            this.stopStationCount = stopStationCount;
        }

        public void setWayCode(Integer wayCode) {
            this.wayCode = wayCode;
        }

        public void setWayName(String wayName) {
            this.wayName = wayName;
        }

        public void setFastTrain(Integer fastTrain) {
            this.fastTrain = fastTrain;
        }

        public void setFastDoor(Integer fastDoor) {
            this.fastDoor = fastDoor;
        }

        public void setPassStopList(PassStopListData passStopList) {
            this.passStopList = passStopList;
        }
    }

    public static class PassStopListData {

        @JsonProperty("stations")
        private List<StationInfoData> stations;

        public List<StationInfoData> getStations() {
            return stations;
        }

        public void setStations(List<StationInfoData> stations) {
            this.stations = stations;
        }
    }

    public static class StationInfoData {

        @JsonProperty("index")
        private Integer index;

        @JsonProperty("stationID")
        private String stationID;

        @JsonProperty("stationName")
        private String stationName;

        @JsonProperty("travelTime")
        private Integer travelTime;

        @JsonProperty("departureTime")
        private String departureTime;

        @JsonProperty("arrivalTime")
        private String arrivalTime;

        @JsonProperty("isStop")
        private String isStop;

        public Integer getIndex() {
            return index;
        }

        public String getStationID() {
            return stationID;
        }

        public String getStationName() {
            return stationName;
        }

        public Integer getTravelTime() {
            return travelTime;
        }

        public String getDepartureTime() {
            return departureTime;
        }

        public String getArrivalTime() {
            return arrivalTime;
        }

        public String getIsStop() {
            return isStop;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public void setStationID(String stationID) {
            this.stationID = stationID;
        }

        public void setStationName(String stationName) {
            this.stationName = stationName;
        }

        public void setTravelTime(Integer travelTime) {
            this.travelTime = travelTime;
        }

        public void setDepartureTime(String departureTime) {
            this.departureTime = departureTime;
        }

        public void setArrivalTime(String arrivalTime) {
            this.arrivalTime = arrivalTime;
        }

        public void setIsStop(String isStop) {
            this.isStop = isStop;
        }
    }
}

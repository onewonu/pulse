package com.pulse.api.odsay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OdsayStationSearchResponse {

    @JsonProperty("result")
    private ResultData result;

    public ResultData getResult() {
        return result;
    }

    public void setResult(ResultData result) {
        this.result = result;
    }

    public static class ResultData {

        @JsonProperty("totalCount")
        private Integer totalCount;

        @JsonProperty("station")
        private List<StationData> stations;

        @JsonProperty("error")
        private ErrorData error;

        public Integer getTotalCount() {
            return totalCount;
        }

        public List<StationData> getStations() {
            return stations;
        }

        public ErrorData getError() {
            return error;
        }

        public void setTotalCount(Integer totalCount) {
            this.totalCount = totalCount;
        }

        public void setStations(List<StationData> stations) {
            this.stations = stations;
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
}

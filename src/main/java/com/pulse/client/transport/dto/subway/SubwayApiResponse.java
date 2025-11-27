package com.pulse.client.transport.dto.subway;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pulse.client.transport.dto.ApiResult;

import java.util.List;

public class SubwayApiResponse {

    @JsonProperty("CardSubwayTime")
    private SubwayApiData cardSubwayTime;

    public SubwayApiData getCardSubwayTime() {
        return cardSubwayTime;
    }

    public void setCardSubwayTime(SubwayApiData cardSubwayTime) {
        this.cardSubwayTime = cardSubwayTime;
    }

    public List<SubwayRidershipData> getData() {
        return cardSubwayTime != null ? cardSubwayTime.getRow() : null;
    }

    public static class SubwayApiData {

        @JsonProperty("list_total_count")
        private Integer listTotalCount;

        @JsonProperty("RESULT")
        private ApiResult result;

        @JsonProperty("row")
        private List<SubwayRidershipData> row;

        public Integer getListTotalCount() {
            return listTotalCount;
        }

        public ApiResult getResult() {
            return result;
        }

        public List<SubwayRidershipData> getRow() {
            return row;
        }

        public void setListTotalCount(Integer listTotalCount) {
            this.listTotalCount = listTotalCount;
        }

        public void setResult(ApiResult result) {
            this.result = result;
        }

        public void setRow(List<SubwayRidershipData> row) {
            this.row = row;
        }
    }
}

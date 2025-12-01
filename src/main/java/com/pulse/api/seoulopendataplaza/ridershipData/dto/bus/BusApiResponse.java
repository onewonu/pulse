package com.pulse.api.seoulopendataplaza.ridershipData.dto.bus;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pulse.api.seoulopendataplaza.ridershipData.ApiResponse;
import com.pulse.api.seoulopendataplaza.ridershipData.ApiResult;

import java.util.List;

public class BusApiResponse implements ApiResponse {

    @JsonProperty("CardBusTimeNew")
    private BusApiData cardBusTimeNew;

    public BusApiData getCardBusTimeNew() {
        return cardBusTimeNew;
    }

    public void setCardBusTimeNew(BusApiData cardBusTimeNew) {
        this.cardBusTimeNew = cardBusTimeNew;
    }

    public List<BusRidershipData> getData() {
        return cardBusTimeNew != null ? cardBusTimeNew.getRow() : null;
    }

    @Override
    public ApiResult getResult() {
        return cardBusTimeNew != null ? cardBusTimeNew.getResult() : null;
    }

    @Override
    public boolean hasData() {
        List<BusRidershipData> data = getData();
        return data != null && !data.isEmpty();
    }

    public static class BusApiData {

        @JsonProperty("list_total_count")
        private Integer listTotalCount;

        @JsonProperty("RESULT")
        private ApiResult result;

        @JsonProperty("row")
        private List<BusRidershipData> row;

        public Integer getListTotalCount() {
            return listTotalCount;
        }

        public ApiResult getResult() {
            return result;
        }

        public List<BusRidershipData> getRow() {
            return row;
        }

        public void setListTotalCount(Integer listTotalCount) {
            this.listTotalCount = listTotalCount;
        }

        public void setResult(ApiResult result) {
            this.result = result;
        }

        public void setRow(List<BusRidershipData> row) {
            this.row = row;
        }
    }
}

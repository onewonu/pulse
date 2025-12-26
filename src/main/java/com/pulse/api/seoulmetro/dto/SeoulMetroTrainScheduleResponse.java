package com.pulse.api.seoulmetro.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SeoulMetroTrainScheduleResponse {

    @JsonProperty("response")
    private Response response;

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public static class Response {

        @JsonProperty("header")
        private Header header;

        @JsonProperty("body")
        private Body body;

        public Header getHeader() {
            return header;
        }

        public Body getBody() {
            return body;
        }

        public void setHeader(Header header) {
            this.header = header;
        }

        public void setBody(Body body) {
            this.body = body;
        }
    }

    public static class Header {

        @JsonProperty("resultCode")
        private String resultCode;

        @JsonProperty("resultMsg")
        private String resultMsg;

        public String getResultCode() {
            return resultCode;
        }

        public String getResultMsg() {
            return resultMsg;
        }

        public void setResultCode(String resultCode) {
            this.resultCode = resultCode;
        }

        public void setResultMsg(String resultMsg) {
            this.resultMsg = resultMsg;
        }
    }

    public static class Body {

        @JsonProperty("numOfRows")
        private Integer numOfRows;

        @JsonProperty("totalCount")
        private Integer totalCount;

        @JsonProperty("pageNo")
        private Integer pageNo;

        @JsonProperty("items")
        private Items items;

        public Integer getNumOfRows() {
            return numOfRows;
        }

        public Integer getTotalCount() {
            return totalCount;
        }

        public Integer getPageNo() {
            return pageNo;
        }

        public Items getItems() {
            return items;
        }

        public void setNumOfRows(Integer numOfRows) {
            this.numOfRows = numOfRows;
        }

        public void setTotalCount(Integer totalCount) {
            this.totalCount = totalCount;
        }

        public void setPageNo(Integer pageNo) {
            this.pageNo = pageNo;
        }

        public void setItems(Items items) {
            this.items = items;
        }
    }

    public static class Items {

        @JsonProperty("item")
        private List<TrainScheduleItem> item;

        public List<TrainScheduleItem> getItem() {
            return item;
        }

        public void setItem(List<TrainScheduleItem> item) {
            this.item = item;
        }
    }
}

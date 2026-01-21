package com.pulse.dto.masterdata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class LinesData {

    @JsonProperty("exportedAt")
    private String exportedAt;

    @JsonProperty("totalLines")
    private Integer totalLines;

    @JsonProperty("lines")
    private List<LineInfo> lines;

    public String getExportedAt() {
        return exportedAt;
    }

    public Integer getTotalLines() {
        return totalLines;
    }

    public List<LineInfo> getLines() {
        return lines;
    }

    public void setExportedAt(String exportedAt) {
        this.exportedAt = exportedAt;
    }

    public void setTotalLines(Integer totalLines) {
        this.totalLines = totalLines;
    }

    public void setLines(List<LineInfo> lines) {
        this.lines = lines;
    }

    public static class LineInfo {
        @JsonProperty("lineName")
        private String lineName;

        @JsonProperty("color")
        private String color;

        public String getLineName() {
            return lineName;
        }

        public String getColor() {
            return color;
        }

        public void setLineName(String lineName) {
            this.lineName = lineName;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }
}

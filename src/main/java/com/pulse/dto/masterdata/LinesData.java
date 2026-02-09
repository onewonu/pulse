package com.pulse.dto.masterdata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LinesData(
    @JsonProperty("exportedAt")
    String exportedAt,

    @JsonProperty("totalLines")
    Integer totalLines,

    @JsonProperty("lines")
    List<LineInfo> lines
) {
    public record LineInfo(
        @JsonProperty("lineName")
        String lineName,

        @JsonProperty("color")
        String color
    ) {}
}

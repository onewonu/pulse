package com.pulse.mapper;

import com.pulse.api.seoulopendataplaza.ridershipData.dto.subway.SubwayRidershipData;
import com.pulse.entity.subway.SubwayLine;
import com.pulse.entity.subway.SubwayRidershipHourly;
import com.pulse.entity.subway.SubwayStation;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class SubwayDataMapper {

    public SubwayLine toSubwayLine(SubwayRidershipData data) {
        return SubwayLine.of(data.getSbwyRoutLnNm());
    }

    public SubwayStation toSubwayStation(SubwayRidershipData data) {
        return SubwayStation.of(data.getSttn());
    }

    public List<SubwayRidershipHourly> toSubwayRidershipHourlyList(
            SubwayRidershipData data,
            SubwayLine line,
            SubwayStation station
    ) {
        List<SubwayRidershipHourly> result = new ArrayList<>();
        LocalDate statDate = YearMonth.parse(data.getUseMm(), DateTimeFormatter.ofPattern("yyyyMM")).atDay(1);

        Integer[] boardingCounts = extractBoardingCounts(data);
        Integer[] alightingCounts = extractAlightingCounts(data);

        for (byte hour = 0; hour < 24; hour++) {
            result.add(SubwayRidershipHourly.of(
                    statDate,
                    line,
                    station,
                    hour,
                    boardingCounts[hour] != null ? boardingCounts[hour] : 0,
                    alightingCounts[hour] != null ? alightingCounts[hour] : 0
            ));
        }

        return result;
    }

    private Integer[] extractBoardingCounts(SubwayRidershipData data) {
        return new Integer[]{
                toInteger(data.getHr0GetOnNope()),
                toInteger(data.getHr1GetOnNope()),
                toInteger(data.getHr2GetOnNope()),
                toInteger(data.getHr3GetOnNope()),
                toInteger(data.getHr4GetOnNope()),
                toInteger(data.getHr5GetOnNope()),
                toInteger(data.getHr6GetOnNope()),
                toInteger(data.getHr7GetOnNope()),
                toInteger(data.getHr8GetOnNope()),
                toInteger(data.getHr9GetOnNope()),
                toInteger(data.getHr10GetOnNope()),
                toInteger(data.getHr11GetOnNope()),
                toInteger(data.getHr12GetOnNope()),
                toInteger(data.getHr13GetOnNope()),
                toInteger(data.getHr14GetOnNope()),
                toInteger(data.getHr15GetOnNope()),
                toInteger(data.getHr16GetOnNope()),
                toInteger(data.getHr17GetOnNope()),
                toInteger(data.getHr18GetOnNope()),
                toInteger(data.getHr19GetOnNope()),
                toInteger(data.getHr20GetOnNope()),
                toInteger(data.getHr21GetOnNope()),
                toInteger(data.getHr22GetOnNope()),
                toInteger(data.getHr23GetOnNope())
        };
    }

    private Integer[] extractAlightingCounts(SubwayRidershipData data) {
        return new Integer[]{
                toInteger(data.getHr0GetOffNope()),
                toInteger(data.getHr1GetOffNope()),
                toInteger(data.getHr2GetOffNope()),
                toInteger(data.getHr3GetOffNope()),
                toInteger(data.getHr4GetOffNope()),
                toInteger(data.getHr5GetOffNope()),
                toInteger(data.getHr6GetOffNope()),
                toInteger(data.getHr7GetOffNope()),
                toInteger(data.getHr8GetOffNope()),
                toInteger(data.getHr9GetOffNope()),
                toInteger(data.getHr10GetOffNope()),
                toInteger(data.getHr11GetOffNope()),
                toInteger(data.getHr12GetOffNope()),
                toInteger(data.getHr13GetOffNope()),
                toInteger(data.getHr14GetOffNope()),
                toInteger(data.getHr15GetOffNope()),
                toInteger(data.getHr16GetOffNope()),
                toInteger(data.getHr17GetOffNope()),
                toInteger(data.getHr18GetOffNope()),
                toInteger(data.getHr19GetOffNope()),
                toInteger(data.getHr20GetOffNope()),
                toInteger(data.getHr21GetOffNope()),
                toInteger(data.getHr22GetOffNope()),
                toInteger(data.getHr23GetOffNope())
        };
    }

    private Integer toInteger(Double value) {
        return value != null ? value.intValue() : null;
    }
}

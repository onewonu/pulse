package com.pulse.mapper;

import com.pulse.client.transport.dto.bus.BusRidershipData;
import com.pulse.entity.bus.BusRidershipHourly;
import com.pulse.entity.bus.BusRoute;
import com.pulse.entity.bus.BusStop;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class BusDataMapper {

    public BusRoute toBusRoute(BusRidershipData data) {
        return BusRoute.of(data.getRteNo(), data.getRteNm(), data.getTrfcMnsTypeCd(), data.getTrfcMnsTypeNm());
    }

    public BusStop toBusStop(BusRidershipData data) {
        return BusStop.of(data.getStopsId(), data.getStopsArsNo(), data.getSbwyStnsNm());
    }

    public List<BusRidershipHourly> toBusRidershipHourlyList(BusRidershipData data, BusRoute route, BusStop stop) {

        LocalDate statDate = YearMonth.parse(data.getUseYm(), DateTimeFormatter.ofPattern("yyyyMM")).atDay(1);
        List<BusRidershipHourly> result = new ArrayList<>();

        Integer[] boardingCounts = extractBoardingCounts(data);
        Integer[] alightingCounts = extractAlightingCounts(data);

        for (byte hour = 0; hour < 24; hour++) {
            result.add(BusRidershipHourly.of(
                    statDate,
                    route,
                    stop,
                    hour,
                    boardingCounts[hour] != null ? boardingCounts[hour] : 0,
                    alightingCounts[hour] != null ? alightingCounts[hour] : 0
            ));
        }

        return result;
    }

    private Integer[] extractBoardingCounts(BusRidershipData data) {
        return new Integer[]{
                toInteger(data.getHr0GetOnTnope()),
                toInteger(data.getHr1GetOnNope()),
                toInteger(data.getHr2GetOnTnope()),
                toInteger(data.getHr3GetOnTnope()),
                toInteger(data.getHr4GetOnTnope()),
                toInteger(data.getHr5GetOnTnope()),
                toInteger(data.getHr6GetOnTnope()),
                toInteger(data.getHr7GetOnTnope()),
                toInteger(data.getHr8GetOnTnope()),
                toInteger(data.getHr9GetOnTnope()),
                toInteger(data.getHr10GetOnTnope()),
                toInteger(data.getHr11GetOnTnope()),
                toInteger(data.getHr12GetOnTnope()),
                toInteger(data.getHr13GetOnTnope()),
                toInteger(data.getHr14GetOnTnope()),
                toInteger(data.getHr15GetOnTnope()),
                toInteger(data.getHr16GetOnTnope()),
                toInteger(data.getHr17GetOnTnope()),
                toInteger(data.getHr18GetOnTnope()),
                toInteger(data.getHr19GetOnTnope()),
                toInteger(data.getHr20GetOnTnope()),
                toInteger(data.getHr21GetOnTnope()),
                toInteger(data.getHr22GetOnTnope()),
                toInteger(data.getHr23GetOnTnope())
        };
    }

    private Integer[] extractAlightingCounts(BusRidershipData data) {
        return new Integer[]{
                toInteger(data.getHr0GetOffTnope()),
                toInteger(data.getHr1GetOffNope()),
                toInteger(data.getHr2GetOffTnope()),
                toInteger(data.getHr3GetOffTnope()),
                toInteger(data.getHr4GetOffTnope()),
                toInteger(data.getHr5GetOffTnope()),
                toInteger(data.getHr6GetOffTnope()),
                toInteger(data.getHr7GetOffTnope()),
                toInteger(data.getHr8GetOffTnope()),
                toInteger(data.getHr9GetOffTnope()),
                toInteger(data.getHr10GetOffTnope()),
                toInteger(data.getHr11GetOffTnope()),
                toInteger(data.getHr12GetOffTnope()),
                toInteger(data.getHr13GetOffTnope()),
                toInteger(data.getHr14GetOffTnope()),
                toInteger(data.getHr15GetOffTnope()),
                toInteger(data.getHr16GetOffTnope()),
                toInteger(data.getHr17GetOffTnope()),
                toInteger(data.getHr18GetOffTnope()),
                toInteger(data.getHr19GetOffTnope()),
                toInteger(data.getHr20GetOffTnope()),
                toInteger(data.getHr21GetOffTnope()),
                toInteger(data.getHr22GetOffTnope()),
                toInteger(data.getHr23GetOffTnope())
        };
    }

    private Integer toInteger(Double value) {
        return value != null ? value.intValue() : null;
    }
}

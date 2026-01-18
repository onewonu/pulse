package com.pulse.mapper;

import com.pulse.api.seoulmetro.dto.TrainScheduleItem;
import com.pulse.entity.subway.SubwayStation;
import com.pulse.entity.subway.SubwayTrainSchedule;
import com.pulse.repository.subway.SubwayLineRepository;
import com.pulse.repository.subway.SubwayStationRepository;
import com.pulse.util.TimeParser;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TrainScheduleMapper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // private final SubwayLineRepository lineRepository;
    private final SubwayStationRepository stationRepository;

    public TrainScheduleMapper(
            SubwayLineRepository lineRepository,
            SubwayStationRepository stationRepository
    ) {
        // this.lineRepository = lineRepository;
        this.stationRepository = stationRepository;
    }

    public SubwayTrainSchedule toSubwayTrainSchedule(
            TrainScheduleItem item,
            String lineName,
            Map<String, SubwayStation> stationCache
    ) {
        SubwayStation departureStation = findStationByName(item.getStnNm(), lineName, stationCache);
        SubwayStation arrivalStation = findStationByName(item.getArvlStnNm(), lineName, stationCache);

        if (departureStation == null || arrivalStation == null) {
            return null;
        }

        LocalTime departureTime = TimeParser.parseHHmmssWithNormalization(item.getTrainDptreTm());
        LocalTime arrivalTime = TimeParser.parseHHmmssWithNormalization(item.getTrainArvlTm());

        if (departureTime == null || arrivalTime == null) {
            return null;
        }

        Boolean isExpress = "Y".equalsIgnoreCase(item.getEtrnYn());
        LocalDateTime validFrom = parseDateTime(item.getVldBgngDt());
        LocalDateTime validTo = parseDateTime(item.getVldEndDt());

        return SubwayTrainSchedule.of(
                item.getTrainno(),
                departureStation,
                arrivalStation,
                departureTime,
                arrivalTime,
                item.getUpbdnbSe(),
                item.getWkndSe(),
                isExpress,
                validFrom,
                validTo
        );
    }

    public SubwayStation findStationByName(
            String stationName,
            String lineName,
            Map<String, SubwayStation> stationCache
    ) {
        String key = stationName + "|" + lineName;
        return stationCache.computeIfAbsent(key, k -> {
            Optional<SubwayStation> exact = stationRepository.findByStationNameAndLineLineName(stationName, lineName);
            if (exact.isPresent()) {
                return exact.get();
            }

            List<SubwayStation> matches = stationRepository.findByStationNameStartingWith(stationName);
            return matches.isEmpty() ? null : matches.getFirst();
        });
    }

    private LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeString, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

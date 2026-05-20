package com.pulse.service.dataload.subway;

import com.pulse.dto.dataload.DataLoadResponse;
import com.pulse.entity.subway.SubwayPassengerHourly;
import com.pulse.repository.subway.SubwayPassengerHourlyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class SubwayPassengerHourlyService {

    private final SubwayPassengerHourlyRepository subwayPassengerRepository;

    public SubwayPassengerHourlyService(SubwayPassengerHourlyRepository subwayPassengerRepository) {
        this.subwayPassengerRepository = subwayPassengerRepository;
    }

    @Transactional
    public DataLoadResponse deleteByYearMonth(String yearMonth) {
        int deletedCount = subwayPassengerRepository.deleteByYearMonth(yearMonth);
        return DataLoadResponse.success("Subway statistics deleted", deletedCount);
    }

    @Transactional
    public int savePassengerData(Map<String, SubwayPassengerHourly> hourlyDataMap) {
        subwayPassengerRepository.saveAll(hourlyDataMap.values());
        return hourlyDataMap.size();
    }
}

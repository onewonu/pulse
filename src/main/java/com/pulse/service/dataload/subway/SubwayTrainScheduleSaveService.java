package com.pulse.service.dataload.subway;

import com.pulse.entity.subway.SubwayTrainSchedule;
import com.pulse.repository.subway.SubwayTrainScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubwayTrainScheduleSaveService {

    private static final Logger log = LoggerFactory.getLogger(SubwayTrainScheduleSaveService.class);

    private final SubwayTrainScheduleRepository scheduleRepository;

    public SubwayTrainScheduleSaveService(SubwayTrainScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional
    public void deleteByDayType(String dayType) {
        scheduleRepository.deleteByDayType(dayType);
        log.info("Deleted existing schedules for dayType: {}", dayType);
    }

    @Transactional
    public int saveBatch(List<SubwayTrainSchedule> batch) {
        scheduleRepository.saveAll(batch);
        return batch.size();
    }
}

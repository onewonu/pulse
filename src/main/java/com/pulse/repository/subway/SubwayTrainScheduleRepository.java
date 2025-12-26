package com.pulse.repository.subway;

import com.pulse.entity.subway.SubwayTrainSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubwayTrainScheduleRepository extends JpaRepository<SubwayTrainSchedule, Long> {

    @Modifying
    @Query("DELETE FROM SubwayTrainSchedule s WHERE s.dayType = :dayType")
    void deleteByDayType(@Param("dayType") String dayType);
}

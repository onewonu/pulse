package com.pulse.repository.subway;

import com.pulse.entity.subway.SubwayTrainSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface SubwayTrainScheduleRepository extends JpaRepository<SubwayTrainSchedule, Long> {

    @Modifying
    @Query("DELETE FROM SubwayTrainSchedule s WHERE s.dayType = :dayType")
    void deleteByDayType(@Param("dayType") String dayType);

    @Query("SELECT DISTINCT s.departureTime FROM SubwayTrainSchedule s WHERE " +
           "s.departureStation.stationName = :departureStationName AND " +
           "s.dayType = :dayType AND " +
           "s.departureTime BETWEEN :startTime AND :endTime " +
           "ORDER BY s.departureTime")
    List<LocalTime> findDistinctDepartureTimesByStationAndDayAndTimeRange(
            @Param("departureStationName") String departureStationName,
            @Param("dayType") String dayType,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}

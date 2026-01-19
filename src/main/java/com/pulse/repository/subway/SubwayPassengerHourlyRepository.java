package com.pulse.repository.subway;

import com.pulse.entity.subway.SubwayPassengerHourly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubwayPassengerHourlyRepository extends JpaRepository<SubwayPassengerHourly, Long> {

    @Modifying
    @Query("DELETE FROM SubwayPassengerHourly s WHERE s.statDate = :statDate")
    void deleteByStatDate(@Param("statDate") LocalDate statDate);

    @Modifying
    @Query("DELETE FROM SubwayPassengerHourly s WHERE FUNCTION('DATE_FORMAT', s.statDate, '%Y%m') = :yearMonth")
    int deleteByYearMonth(@Param("yearMonth") String yearMonth);

    @Query("SELECT s FROM SubwayPassengerHourly s WHERE " +
           "s.subwayStation.stationId IN :stationIds AND " +
           "s.hourSlot = :hourSlot")
    List<SubwayPassengerHourly> findByStationIdsAndHourSlot(
            @Param("stationIds") List<String> stationIds,
            @Param("hourSlot") Byte hourSlot
    );
}

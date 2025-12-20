package com.pulse.repository.subway;

import com.pulse.entity.subway.SubwayRidershipHourly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface SubwayRidershipHourlyRepository extends JpaRepository<SubwayRidershipHourly, Long> {

    @Modifying
    @Query("DELETE FROM SubwayRidershipHourly s WHERE s.statDate = :statDate")
    void deleteByStatDate(@Param("statDate") LocalDate statDate);

    @Modifying
    @Query("DELETE FROM SubwayRidershipHourly s WHERE FUNCTION('DATE_FORMAT', s.statDate, '%Y%m') = :yearMonth")
    int deleteByYearMonth(@Param("yearMonth") String yearMonth);
}

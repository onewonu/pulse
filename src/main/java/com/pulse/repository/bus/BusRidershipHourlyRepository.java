package com.pulse.repository.bus;

import com.pulse.entity.bus.BusRidershipHourly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface BusRidershipHourlyRepository extends JpaRepository<BusRidershipHourly, Long> {

    @Modifying
    @Query("DELETE FROM BusRidershipHourly b WHERE b.statDate = :statDate")
    void deleteByStatDate(@Param("statDate") LocalDate statDate);
}

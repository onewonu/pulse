package com.pulse.repository.subway;

import com.pulse.entity.subway.SubwayStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubwayStationRepository extends JpaRepository<SubwayStation, String> {

    @Query("SELECT s FROM SubwayStation s WHERE s.stationName LIKE CONCAT(:stationName, '%') ORDER BY LENGTH(s.stationName)")
    List<SubwayStation> findByStationNameStartingWith(@Param("stationName") String stationName);
}

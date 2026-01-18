package com.pulse.repository.subway;

import com.pulse.entity.subway.SubwayStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubwayStationRepository extends JpaRepository<SubwayStation, String> {

    @Query("SELECT s FROM SubwayStation s WHERE s.stationName LIKE CONCAT(:stationName, '%') ORDER BY LENGTH(s.stationName)")
    List<SubwayStation> findByStationNameStartingWith(@Param("stationName") String stationName);

    @Query("SELECT s FROM SubwayStation s WHERE s.stationName = :stationName AND s.subwayLine.lineName = :lineName")
    Optional<SubwayStation> findByStationNameAndLineLineName(
            @Param("stationName") String stationName,
            @Param("lineName") String lineName
    );

    List<SubwayStation> findByStationName(String stationName);
}

package com.pulse.repository.subway;

import com.pulse.entity.subway.SubwayStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubwayStationRepository extends JpaRepository<SubwayStation, String> {

    Optional<SubwayStation> findByStationName(String stationName);
}

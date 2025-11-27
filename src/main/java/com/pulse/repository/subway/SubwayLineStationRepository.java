package com.pulse.repository.subway;

import com.pulse.entity.subway.SubwayLineStation;
import com.pulse.entity.subway.SubwayLineStationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubwayLineStationRepository extends JpaRepository<SubwayLineStation, SubwayLineStationId> {}

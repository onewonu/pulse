package com.pulse.repository.subway;

import com.pulse.entity.subway.SubwayLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubwayLineRepository extends JpaRepository<SubwayLine, String> {

    Optional<SubwayLine> findByLineName(String lineName);
}

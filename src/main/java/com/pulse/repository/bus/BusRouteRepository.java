package com.pulse.repository.bus;

import com.pulse.entity.bus.BusRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusRouteRepository extends JpaRepository<BusRoute, String> {

    Optional<BusRoute> findByRouteNumber(String routeNumber);
}

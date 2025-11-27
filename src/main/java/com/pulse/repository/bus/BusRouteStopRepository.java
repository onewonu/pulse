package com.pulse.repository.bus;

import com.pulse.entity.bus.BusRouteStop;
import com.pulse.entity.bus.BusRouteStopId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusRouteStopRepository extends JpaRepository<BusRouteStop, BusRouteStopId> {}

package com.majuro.transit.repository.gtfs;

import com.majuro.transit.model.gtfs.GtfsRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GtfsRouteRepository extends JpaRepository<GtfsRoute, String> {
}

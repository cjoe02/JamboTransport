package com.majuro.transit.repository.gtfs;

import com.majuro.transit.model.gtfs.GtfsStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GtfsStopRepository extends JpaRepository<GtfsStop, String> {
}

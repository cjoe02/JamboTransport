package com.majuro.transit.repository.gtfs;

import com.majuro.transit.model.gtfs.GtfsCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GtfsCalendarRepository extends JpaRepository<GtfsCalendar, String> {
}

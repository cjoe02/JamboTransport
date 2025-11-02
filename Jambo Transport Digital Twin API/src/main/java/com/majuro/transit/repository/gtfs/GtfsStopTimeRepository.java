package com.majuro.transit.repository.gtfs;

import com.majuro.transit.model.gtfs.GtfsStopTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface GtfsStopTimeRepository extends JpaRepository<GtfsStopTime, Long> {

    List<GtfsStopTime> findByTripTripIdOrderByStopSequenceAsc(String tripId);

    @Query("SELECT st FROM GtfsStopTime st WHERE st.stop.stopId = :stopId " +
           "AND st.arrivalTime >= :currentTime ORDER BY st.arrivalTime ASC")
    List<GtfsStopTime> findUpcomingArrivals(@Param("stopId") String stopId, @Param("currentTime") LocalTime currentTime);
}

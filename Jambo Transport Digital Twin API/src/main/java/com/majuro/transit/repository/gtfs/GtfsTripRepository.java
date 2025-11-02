package com.majuro.transit.repository.gtfs;

import com.majuro.transit.model.gtfs.GtfsTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface GtfsTripRepository extends JpaRepository<GtfsTrip, String> {

    List<GtfsTrip> findByRouteRouteId(String routeId);

    @Query("SELECT DISTINCT t FROM GtfsTrip t " +
           "JOIN FETCH t.stopTimes st " +
           "WHERE t.route.routeId = :routeId " +
           "AND EXISTS (SELECT 1 FROM GtfsStopTime st2 WHERE st2.trip = t AND st2.departureTime <= :currentTime) " +
           "AND EXISTS (SELECT 1 FROM GtfsStopTime st3 WHERE st3.trip = t AND st3.arrivalTime >= :currentTime)")
    List<GtfsTrip> findActiveTrips(@Param("routeId") String routeId, @Param("currentTime") LocalTime currentTime);
}

package com.majuro.transit.service;

import com.majuro.transit.model.BusPosition;
import com.majuro.transit.model.gtfs.GtfsRoute;
import com.majuro.transit.model.gtfs.GtfsStop;
import com.majuro.transit.model.gtfs.GtfsStopTime;
import com.majuro.transit.model.gtfs.GtfsTrip;
import com.majuro.transit.repository.gtfs.GtfsRouteRepository;
import com.majuro.transit.repository.gtfs.GtfsStopRepository;
import com.majuro.transit.repository.gtfs.GtfsStopTimeRepository;
import com.majuro.transit.repository.gtfs.GtfsTripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GtfsBusSimulationService {

    private final GtfsStopRepository stopRepository;
    private final GtfsRouteRepository routeRepository;
    private final GtfsTripRepository tripRepository;
    private final GtfsStopTimeRepository stopTimeRepository;
    private final GtfsPositionCalculator positionCalculator;

    @Transactional(readOnly = true)
    public List<BusPosition> getAllActiveBusPositions() {
        LocalTime currentTime = LocalTime.now();

        // Get all trips
        List<GtfsTrip> allTrips = tripRepository.findAll();

        // Calculate positions for all trips
        List<BusPosition> allPositions = allTrips.stream()
                .map(trip -> positionCalculator.calculatePosition(trip, currentTime))
                .filter(BusPosition::getIsOperational)
                .collect(Collectors.toList());

        // Group by route and bus number to get only current trip per physical bus
        // Extract bus number from tripId (e.g., "ROUTE_A_BUS1_TRIP001" -> "ROUTE_A_BUS1")
        return allPositions.stream()
                .collect(Collectors.groupingBy(pos -> {
                    // Extract physical bus identifier (route + bus number)
                    String label = pos.getBusLabel();
                    // Format: ROUTE_X_BUSY_TRIPZZZ or ROUTE_X_BUSY_TRIPZZZ_RETURN
                    int tripIndex = label.indexOf("_TRIP");
                    return tripIndex > 0 ? label.substring(0, tripIndex) : label;
                }))
                .values()
                .stream()
                .map(positions -> {
                    // For each physical bus, find the trip that's currently in progress
                    // Sort by how far along the trip is (progressPercent + time)
                    return positions.stream()
                            .filter(p -> "MOVING".equals(p.getStatus()) || "AT_STOP".equals(p.getStatus()))
                            .findFirst()
                            .orElse(positions.get(0));
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BusPosition getTripPosition(String tripId) {
        LocalTime currentTime = LocalTime.now();

        GtfsTrip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));

        return positionCalculator.calculatePosition(trip, currentTime);
    }

    @Transactional(readOnly = true)
    public List<BusPosition> getActiveTripsForRoute(String routeId) {
        LocalTime currentTime = LocalTime.now();

        List<GtfsTrip> trips = tripRepository.findByRouteRouteId(routeId);

        return trips.stream()
                .map(trip -> positionCalculator.calculatePosition(trip, currentTime))
                .filter(BusPosition::getIsOperational)
                .collect(Collectors.toList());
    }

    public List<GtfsStop> getAllStops() {
        return stopRepository.findAll();
    }

    public List<GtfsRoute> getAllRoutes() {
        return routeRepository.findAll();
    }

    public GtfsRoute getRoute(String routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found: " + routeId));
    }

    public GtfsStop getStop(String stopId) {
        return stopRepository.findById(stopId)
                .orElseThrow(() -> new RuntimeException("Stop not found: " + stopId));
    }

    @Transactional(readOnly = true)
    public List<GtfsStopTime> getUpcomingArrivals(String stopId, int limit) {
        LocalTime currentTime = LocalTime.now();
        List<GtfsStopTime> arrivals = stopTimeRepository.findUpcomingArrivals(stopId, currentTime);

        return arrivals.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GtfsTrip> getTripsForRoute(String routeId) {
        return tripRepository.findByRouteRouteId(routeId);
    }

    @Transactional(readOnly = true)
    public GtfsTrip getTripByTripId(String tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));
    }
}

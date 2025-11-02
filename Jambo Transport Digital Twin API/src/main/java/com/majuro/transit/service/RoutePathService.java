package com.majuro.transit.service;

import com.majuro.transit.model.gtfs.GtfsStopTime;
import com.majuro.transit.model.gtfs.GtfsTrip;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoutePathService {

    private final RoadNetworkRoutingService roadRoutingService;

    // Cache for route paths to avoid repeated OSRM API calls
    private final java.util.Map<String, List<RoutePathPoint>> routePathCache =
        new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Generates a detailed path for a route that follows the actual road network.
     * Uses OSRM routing service to generate realistic paths between stops.
     * Results are cached per trip to improve performance.
     *
     * @param trip The GTFS trip
     * @param pointsPerSegment Ignored when using road network routing (kept for API compatibility)
     * @return List of coordinate points representing the route path
     */
    public List<RoutePathPoint> generateRoutePath(GtfsTrip trip, Integer pointsPerSegment) {
        // Check cache first
        String cacheKey = trip.getTripId();
        if (routePathCache.containsKey(cacheKey)) {
            return routePathCache.get(cacheKey);
        }

        // Generate and cache the path
        List<RoutePathPoint> path = generateRoadBasedPath(trip);
        routePathCache.put(cacheKey, path);
        return path;
    }

    /**
     * Generates path using road network routing (OSRM)
     */
    private List<RoutePathPoint> generateRoadBasedPath(GtfsTrip trip) {
        List<RoutePathPoint> pathPoints = new ArrayList<>();
        List<GtfsStopTime> stopTimes = trip.getStopTimes();

        if (stopTimes.isEmpty()) {
            return pathPoints;
        }

        // Extract stop coordinates
        List<double[]> stopCoordinates = stopTimes.stream()
            .map(st -> new double[]{
                st.getStop().getStopLat(),
                st.getStop().getStopLon()
            })
            .toList();

        // Get road-based path
        List<double[]> roadPath = roadRoutingService.generateRoadPathOptimized(stopCoordinates);

        if (roadPath.isEmpty()) {
            // Fallback to simple stop-to-stop if routing fails
            return generateSimplePath(trip);
        }

        // Convert road path to RoutePathPoints with distance calculations
        double totalDistance = 0.0;

        for (int i = 0; i < roadPath.size(); i++) {
            double[] coord = roadPath.get(i);

            // Calculate distance from previous point
            if (i > 0) {
                double[] prevCoord = roadPath.get(i - 1);
                totalDistance += calculateHaversineDistance(
                    prevCoord[0], prevCoord[1],
                    coord[0], coord[1]
                );
            }

            // Check if this coordinate matches a stop
            GtfsStopTime matchingStop = findMatchingStop(coord[0], coord[1], stopTimes);

            if (matchingStop != null) {
                pathPoints.add(new RoutePathPoint(
                    coord[0],
                    coord[1],
                    matchingStop.getStop().getStopId(),
                    matchingStop.getStop().getStopName(),
                    matchingStop.getStopSequence(),
                    totalDistance,
                    true
                ));
            } else {
                pathPoints.add(new RoutePathPoint(
                    coord[0],
                    coord[1],
                    null,
                    null,
                    null,
                    totalDistance,
                    false
                ));
            }
        }

        return pathPoints;
    }

    /**
     * Find a stop that matches the given coordinate (within 50 meters)
     */
    private GtfsStopTime findMatchingStop(double lat, double lon, List<GtfsStopTime> stopTimes) {
        for (GtfsStopTime stopTime : stopTimes) {
            double distance = calculateHaversineDistance(
                lat, lon,
                stopTime.getStop().getStopLat(),
                stopTime.getStop().getStopLon()
            );

            // Consider it a match if within 50 meters (0.05 km)
            if (distance < 0.05) {
                return stopTime;
            }
        }
        return null;
    }

    /**
     * Simple fallback path that just connects stops directly
     */
    private List<RoutePathPoint> generateSimplePath(GtfsTrip trip) {
        List<RoutePathPoint> pathPoints = new ArrayList<>();
        List<GtfsStopTime> stopTimes = trip.getStopTimes();
        double totalDistance = 0.0;

        for (int i = 0; i < stopTimes.size(); i++) {
            GtfsStopTime stopTime = stopTimes.get(i);

            if (i > 0) {
                GtfsStopTime prevStop = stopTimes.get(i - 1);
                totalDistance += calculateHaversineDistance(
                    prevStop.getStop().getStopLat(), prevStop.getStop().getStopLon(),
                    stopTime.getStop().getStopLat(), stopTime.getStop().getStopLon()
                );
            }

            pathPoints.add(new RoutePathPoint(
                stopTime.getStop().getStopLat(),
                stopTime.getStop().getStopLon(),
                stopTime.getStop().getStopId(),
                stopTime.getStop().getStopName(),
                stopTime.getStopSequence(),
                totalDistance,
                true
            ));
        }

        return pathPoints;
    }


    /**
     * Calculate distance between two points using the Haversine formula
     * Returns distance in kilometers
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_KM = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }


    /**
     * Represents a point along the route path
     */
    public record RoutePathPoint(
            Double latitude,
            Double longitude,
            String stopId,        // Null if not a stop
            String stopName,      // Null if not a stop
            Integer sequence,     // Null if not a stop
            Double distanceFromStart, // Distance in kilometers from route start
            Boolean isStop        // True if this is an actual stop, false if following road
    ) {}
}

package com.majuro.transit.service;

import com.majuro.transit.model.BusPosition;
import com.majuro.transit.model.RouteImpact;
import com.majuro.transit.model.gtfs.GtfsStopTime;
import com.majuro.transit.model.gtfs.GtfsTrip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GtfsPositionCalculator {

    private final RoutePathService routePathService;
    private final TidalImpactCalculator tidalImpactCalculator;

    public BusPosition calculatePosition(GtfsTrip trip, LocalTime currentTime) {
        List<GtfsStopTime> stopTimes = trip.getStopTimes();

        if (stopTimes.isEmpty()) {
            return createNotOperationalPosition(trip);
        }

        // Find current position in schedule
        GtfsStopTime previousStop = null;
        GtfsStopTime nextStop = null;

        for (int i = 0; i < stopTimes.size(); i++) {
            GtfsStopTime stopTime = stopTimes.get(i);

            if (currentTime.isBefore(stopTime.getDepartureTime())) {
                nextStop = stopTime;
                if (i > 0) {
                    previousStop = stopTimes.get(i - 1);
                }
                break;
            }
        }

        // If no next stop found, trip hasn't started yet or has completed
        if (nextStop == null) {
            // Check if before first stop
            if (currentTime.isBefore(stopTimes.get(0).getDepartureTime())) {
                return createNotOperationalPosition(trip);
            }
            // Trip has completed
            return createCompletedTripPosition(trip, stopTimes.get(stopTimes.size() - 1));
        }

        // If no previous stop, we're at the first stop waiting to depart
        if (previousStop == null) {
            return createAtStopPosition(trip, stopTimes.get(0), stopTimes.get(1));
        }

        // Calculate position between previous and next stop
        return calculateIntermediatePosition(trip, previousStop, nextStop, currentTime);
    }

    private BusPosition calculateIntermediatePosition(GtfsTrip trip, GtfsStopTime from, GtfsStopTime to, LocalTime currentTime) {
        BusPosition position = new BusPosition();
        position.setBusId(generateBusId(trip.getTripId()));
        position.setBusLabel(trip.getTripId());
        position.setRouteName(trip.getRoute().getRouteShortName());
        position.setIsOperational(true);
        position.setIsOnBreak(false);
        position.setStatus("MOVING");

        // Calculate progress between stops
        long totalSeconds = ChronoUnit.SECONDS.between(from.getDepartureTime(), to.getArrivalTime());
        long elapsedSeconds = ChronoUnit.SECONDS.between(from.getDepartureTime(), currentTime);

        double progressRatio = totalSeconds > 0 ? (double) elapsedSeconds / totalSeconds : 0;
        progressRatio = Math.max(0, Math.min(1, progressRatio)); // Clamp between 0 and 1

        position.setProgressPercent(progressRatio * 100);

        // Calculate position along road path
        double[] coordinates = calculatePositionOnRoadPath(trip, from, to, progressRatio);
        position.setCurrentLatitude(coordinates[0]);
        position.setCurrentLongitude(coordinates[1]);

        // Set stop information
        position.setCurrentStop(convertGtfsStopToBusStop(from.getStop()));
        position.setNextStop(convertGtfsStopToBusStop(to.getStop()));

        // Calculate time to next stop (with tidal delay adjustment)
        long secondsToNext = ChronoUnit.SECONDS.between(currentTime, to.getArrivalTime());
        int minutesToNext = (int) Math.max(0, (secondsToNext + 59) / 60); // Round up

        // Apply tidal impact (this will set minutesToNextStop and estimatedArrivalTime)
        applyTidalImpact(position, trip.getRoute().getRouteId(), minutesToNext, currentTime);

        return position;
    }

    /**
     * Calculate bus position along the road path between two stops
     */
    private double[] calculatePositionOnRoadPath(GtfsTrip trip, GtfsStopTime from, GtfsStopTime to, double progressRatio) {
        try {
            // Get the full route path
            List<RoutePathService.RoutePathPoint> fullPath = routePathService.generateRoutePath(trip, null);

            // Find the segment between these two stops
            List<RoutePathService.RoutePathPoint> segmentPath = extractSegmentBetweenStops(
                fullPath,
                from.getStop().getStopId(),
                to.getStop().getStopId()
            );

            if (segmentPath.isEmpty() || segmentPath.size() < 2) {
                // Fallback to simple linear interpolation
                return linearInterpolate(from, to, progressRatio);
            }

            // Calculate total distance of segment
            double totalDistance = calculateSegmentDistance(segmentPath);

            // Find position along the path based on progress ratio
            double targetDistance = totalDistance * progressRatio;
            return findPositionAtDistance(segmentPath, targetDistance);

        } catch (Exception e) {
            log.warn("Error calculating road path position, falling back to linear interpolation: {}", e.getMessage());
            return linearInterpolate(from, to, progressRatio);
        }
    }

    /**
     * Extract the path segment between two stops
     */
    private List<RoutePathService.RoutePathPoint> extractSegmentBetweenStops(
            List<RoutePathService.RoutePathPoint> fullPath,
            String fromStopId,
            String toStopId) {

        int fromIndex = -1;
        int toIndex = -1;

        for (int i = 0; i < fullPath.size(); i++) {
            RoutePathService.RoutePathPoint point = fullPath.get(i);
            if (fromStopId.equals(point.stopId())) {
                fromIndex = i;
            }
            if (toStopId.equals(point.stopId()) && fromIndex >= 0) {
                toIndex = i;
                break;
            }
        }

        if (fromIndex >= 0 && toIndex > fromIndex) {
            return fullPath.subList(fromIndex, toIndex + 1);
        }

        return List.of();
    }

    /**
     * Calculate total distance of a path segment
     */
    private double calculateSegmentDistance(List<RoutePathService.RoutePathPoint> segment) {
        double distance = 0.0;
        for (int i = 1; i < segment.size(); i++) {
            RoutePathService.RoutePathPoint prev = segment.get(i - 1);
            RoutePathService.RoutePathPoint curr = segment.get(i);
            distance += haversineDistance(
                prev.latitude(), prev.longitude(),
                curr.latitude(), curr.longitude()
            );
        }
        return distance;
    }

    /**
     * Find position along path at a specific distance
     */
    private double[] findPositionAtDistance(List<RoutePathService.RoutePathPoint> segment, double targetDistance) {
        double accumulatedDistance = 0.0;

        for (int i = 1; i < segment.size(); i++) {
            RoutePathService.RoutePathPoint prev = segment.get(i - 1);
            RoutePathService.RoutePathPoint curr = segment.get(i);

            double segmentDist = haversineDistance(
                prev.latitude(), prev.longitude(),
                curr.latitude(), curr.longitude()
            );

            if (accumulatedDistance + segmentDist >= targetDistance) {
                // Target is within this segment
                double ratio = (targetDistance - accumulatedDistance) / segmentDist;
                double lat = prev.latitude() + (curr.latitude() - prev.latitude()) * ratio;
                double lon = prev.longitude() + (curr.longitude() - prev.longitude()) * ratio;
                return new double[]{lat, lon};
            }

            accumulatedDistance += segmentDist;
        }

        // If we've gone past the end, return the last point
        RoutePathService.RoutePathPoint lastPoint = segment.get(segment.size() - 1);
        return new double[]{lastPoint.latitude(), lastPoint.longitude()};
    }

    /**
     * Simple linear interpolation fallback
     */
    private double[] linearInterpolate(GtfsStopTime from, GtfsStopTime to, double progressRatio) {
        double fromLat = from.getStop().getStopLat();
        double fromLon = from.getStop().getStopLon();
        double toLat = to.getStop().getStopLat();
        double toLon = to.getStop().getStopLon();

        double lat = fromLat + (toLat - fromLat) * progressRatio;
        double lon = fromLon + (toLon - fromLon) * progressRatio;
        return new double[]{lat, lon};
    }

    /**
     * Calculate Haversine distance between two points in kilometers
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_KM = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    private BusPosition createAtStopPosition(GtfsTrip trip, GtfsStopTime current, GtfsStopTime next) {
        BusPosition position = new BusPosition();
        position.setBusId(generateBusId(trip.getTripId()));
        position.setBusLabel(trip.getTripId());
        position.setRouteName(trip.getRoute().getRouteShortName());
        position.setIsOperational(true);
        position.setIsOnBreak(false);
        position.setStatus("AT_STOP");

        position.setCurrentStop(convertGtfsStopToBusStop(current.getStop()));
        position.setNextStop(convertGtfsStopToBusStop(next.getStop()));

        position.setCurrentLatitude(current.getStop().getStopLat());
        position.setCurrentLongitude(current.getStop().getStopLon());
        position.setProgressPercent(0.0);

        LocalTime currentTime = LocalTime.now();
        long secondsToNext = ChronoUnit.SECONDS.between(currentTime, next.getArrivalTime());
        int minutesToNext = (int) Math.max(0, (secondsToNext + 59) / 60);

        // Apply tidal impact
        applyTidalImpact(position, trip.getRoute().getRouteId(), minutesToNext, currentTime);

        return position;
    }

    private BusPosition createCompletedTripPosition(GtfsTrip trip, GtfsStopTime lastStop) {
        BusPosition position = new BusPosition();
        position.setBusId(generateBusId(trip.getTripId()));
        position.setBusLabel(trip.getTripId());
        position.setRouteName(trip.getRoute().getRouteShortName());
        position.setIsOperational(false);
        position.setIsOnBreak(false);
        position.setStatus("TRIP_COMPLETED");

        position.setCurrentStop(convertGtfsStopToBusStop(lastStop.getStop()));
        position.setCurrentLatitude(lastStop.getStop().getStopLat());
        position.setCurrentLongitude(lastStop.getStop().getStopLon());
        position.setProgressPercent(100.0);
        position.setMinutesToNextStop(0);

        // Set default tidal fields for completed trips
        position.setTidalDelayApplied(false);
        position.setEstimatedDelayMinutes(0);
        position.setTidalImpactLevel("NONE");
        position.setEstimatedArrivalTime(null);

        return position;
    }

    private BusPosition createNotOperationalPosition(GtfsTrip trip) {
        BusPosition position = new BusPosition();
        position.setBusId(generateBusId(trip.getTripId()));
        position.setBusLabel(trip.getTripId());
        position.setRouteName(trip.getRoute().getRouteShortName());
        position.setIsOperational(false);
        position.setIsOnBreak(false);
        position.setStatus("NOT_OPERATIONAL");
        position.setProgressPercent(0.0);
        position.setMinutesToNextStop(0);

        // Set default tidal fields for non-operational buses
        position.setTidalDelayApplied(false);
        position.setEstimatedDelayMinutes(0);
        position.setTidalImpactLevel("NONE");
        position.setEstimatedArrivalTime(null);

        return position;
    }

    private Long generateBusId(String tripId) {
        return (long) tripId.hashCode();
    }

    private com.majuro.transit.dto.StopDTO convertGtfsStopToBusStop(com.majuro.transit.model.gtfs.GtfsStop gtfsStop) {
        return new com.majuro.transit.dto.StopDTO(
                gtfsStop.getStopId(),
                gtfsStop.getStopName(),
                gtfsStop.getStopLat(),
                gtfsStop.getStopLon()
        );
    }

    /**
     * Apply tidal impact to bus position
     */
    private void applyTidalImpact(BusPosition position, String routeId, int baseMinutesToNext, LocalTime currentTime) {
        try {
            RouteImpact impact = tidalImpactCalculator.calculateImpact(routeId);

            if (impact.getImpactLevel() != RouteImpact.ImpactLevel.NONE) {
                // Apply delay multiplier
                int adjustedMinutes = (int) Math.ceil(baseMinutesToNext * impact.getDelayMultiplier());
                position.setMinutesToNextStop(adjustedMinutes);
                position.setTidalDelayApplied(true);
                position.setEstimatedDelayMinutes(impact.getEstimatedDelayMinutes());
                position.setTidalImpactLevel(impact.getImpactLevel().name());

                // Calculate estimated arrival time
                LocalTime estimatedArrival = currentTime.plusMinutes(adjustedMinutes);
                position.setEstimatedArrivalTime(estimatedArrival.toString());

                if (impact.getImpactLevel() == RouteImpact.ImpactLevel.SHUTDOWN) {
                    position.setIsOperational(false);
                    position.setStatus("TIDAL_SHUTDOWN");
                }
            } else {
                position.setMinutesToNextStop(baseMinutesToNext);
                position.setTidalDelayApplied(false);
                position.setEstimatedDelayMinutes(0);
                position.setTidalImpactLevel("NONE");

                // Calculate estimated arrival time (no delay)
                LocalTime estimatedArrival = currentTime.plusMinutes(baseMinutesToNext);
                position.setEstimatedArrivalTime(estimatedArrival.toString());
            }
        } catch (Exception e) {
            log.warn("Failed to apply tidal impact: {}", e.getMessage());
            // Fallback to no impact
            position.setMinutesToNextStop(baseMinutesToNext);
            position.setTidalDelayApplied(false);
            position.setEstimatedDelayMinutes(0);
            position.setTidalImpactLevel("NONE");

            // Calculate estimated arrival time (no delay)
            LocalTime estimatedArrival = currentTime.plusMinutes(baseMinutesToNext);
            position.setEstimatedArrivalTime(estimatedArrival.toString());
        }
    }
}

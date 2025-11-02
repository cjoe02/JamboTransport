package com.majuro.transit.controller;

import com.majuro.transit.dto.BusPositionDTO;
import com.majuro.transit.dto.StopDTO;
import com.majuro.transit.model.gtfs.GtfsStopTime;
import com.majuro.transit.model.gtfs.GtfsTrip;
import com.majuro.transit.model.RouteImpact;
import com.majuro.transit.service.GtfsBusSimulationService;
import com.majuro.transit.service.RoutePathService;
import com.majuro.transit.service.TidalImpactCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gtfs/buses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GtfsBusController {

    private final GtfsBusSimulationService simulationService;
    private final RoutePathService routePathService;
    private final TidalImpactCalculator tidalImpactCalculator;

    @GetMapping("/active")
    public ResponseEntity<List<BusPositionDTO>> getActiveBuses() {
        List<BusPositionDTO> positions = simulationService.getAllActiveBusPositions()
                .stream()
                .map(BusPositionDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(positions);
    }

    @GetMapping("/{busIdOrLabel}")
    public ResponseEntity<BusPositionDTO> getBusPosition(@PathVariable String busIdOrLabel) {
        // First, try to find by trip ID directly
        try {
            BusPositionDTO position = BusPositionDTO.fromEntity(
                    simulationService.getTripPosition(busIdOrLabel)
            );
            return ResponseEntity.ok(position);
        } catch (RuntimeException e) {
            // Not found as trip ID, try searching in active buses by ID
        }

        // Try to match by busId in active buses
        try {
            Long busId = Long.parseLong(busIdOrLabel);
            List<BusPositionDTO> activeBuses = simulationService.getAllActiveBusPositions()
                    .stream()
                    .map(BusPositionDTO::fromEntity)
                    .collect(Collectors.toList());

            BusPositionDTO bus = activeBuses.stream()
                    .filter(b -> b.getBusId().equals(busId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Bus not found with ID: " + busIdOrLabel));

            return ResponseEntity.ok(bus);
        } catch (NumberFormatException e) {
            // Not a numeric ID, throw not found
            throw new RuntimeException("Bus not found with ID or label: " + busIdOrLabel);
        }
    }

    @GetMapping("/trips/{tripId}/position")
    public ResponseEntity<BusPositionDTO> getTripPosition(@PathVariable String tripId) {
        BusPositionDTO position = BusPositionDTO.fromEntity(
                simulationService.getTripPosition(tripId)
        );
        return ResponseEntity.ok(position);
    }

    @GetMapping("/{busId}/route")
    public ResponseEntity<BusRouteDTO> getBusRoute(@PathVariable Long busId) {
        // Find the trip by busId (busId is generated from tripId hashCode)
        List<BusPositionDTO> activeBuses = simulationService.getAllActiveBusPositions()
                .stream()
                .map(BusPositionDTO::fromEntity)
                .collect(Collectors.toList());

        BusPositionDTO bus = activeBuses.stream()
                .filter(b -> b.getBusId().equals(busId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Bus not found with ID: " + busId));

        return ResponseEntity.ok(getBusRouteByTripId(bus.getBusLabel()));
    }

    @GetMapping("/trip/{busLabel}/route")
    public ResponseEntity<BusRouteDTO> getBusRouteByLabel(@PathVariable String busLabel) {
        return ResponseEntity.ok(getBusRouteByTripId(busLabel));
    }

    @GetMapping("/trip/{tripId}/path")
    public ResponseEntity<RoutePathDTO> getRoutePath(
            @PathVariable String tripId,
            @RequestParam(required = false) Integer pointsPerSegment) {

        GtfsTrip trip = simulationService.getTripByTripId(tripId);
        List<RoutePathService.RoutePathPoint> pathPoints =
                routePathService.generateRoutePath(trip, pointsPerSegment);

        return ResponseEntity.ok(new RoutePathDTO(
                tripId,
                trip.getRoute().getRouteShortName(),
                trip.getTripHeadsign(),
                trip.getDirectionId(),
                pathPoints
        ));
    }

    @GetMapping("/route/{routeId}/path")
    public ResponseEntity<RoutePathDTO> getRoutePathSimplified(@PathVariable String routeId) {
        // Convert A/B to ROUTE_A/ROUTE_B
        String fullRouteId = routeId.matches("^[AB]$") ? "ROUTE_" + routeId : routeId;

        // Get any trip from this route (all trips for a route have the same path)
        List<GtfsTrip> trips = simulationService.getTripsForRoute(fullRouteId);

        if (trips.isEmpty()) {
            throw new RuntimeException("Route not found: " + routeId);
        }

        // Use the first trip to generate the path
        GtfsTrip trip = trips.get(0);
        List<RoutePathService.RoutePathPoint> pathPoints =
                routePathService.generateRoutePath(trip, null);

        return ResponseEntity.ok(new RoutePathDTO(
                fullRouteId,
                trip.getRoute().getRouteShortName(),
                trip.getTripHeadsign(),
                trip.getDirectionId(),
                pathPoints
        ));
    }

    private BusRouteDTO getBusRouteByTripId(String tripId) {
        GtfsTrip trip = simulationService.getTripByTripId(tripId);
        LocalTime currentTime = LocalTime.now();

        // Get tidal impact for this route
        RouteImpact impact = tidalImpactCalculator.calculateImpact(trip.getRoute().getRouteId());
        Integer delayMinutes = impact.getEstimatedDelayMinutes();

        List<BusRouteStopDTO> stops = trip.getStopTimes().stream()
                .map(st -> {
                    LocalTime scheduledArrival = st.getArrivalTime();
                    LocalTime scheduledDeparture = st.getDepartureTime();

                    // Calculate estimated times with tidal delay
                    LocalTime estimatedArrival = scheduledArrival.plusMinutes(delayMinutes);
                    LocalTime estimatedDeparture = scheduledDeparture.plusMinutes(delayMinutes);

                    boolean isPassed = scheduledDeparture.isBefore(currentTime);
                    boolean isCurrent = !isPassed &&
                        (scheduledArrival.isAfter(currentTime) || scheduledArrival.equals(currentTime));

                    return new BusRouteStopDTO(
                        st.getStop().getStopId(),
                        st.getStop().getStopName(),
                        st.getStop().getStopLat(),
                        st.getStop().getStopLon(),
                        scheduledArrival.toString(),
                        scheduledDeparture.toString(),
                        estimatedArrival.toString(),
                        estimatedDeparture.toString(),
                        delayMinutes,
                        impact.getInundationLevel(),
                        st.getStopSequence(),
                        isPassed,
                        isCurrent
                    );
                })
                .collect(Collectors.toList());

        return new BusRouteDTO(
            tripId,
            trip.getRoute().getRouteShortName(),
            trip.getTripHeadsign(),
            trip.getDirectionId(),
            stops
        );
    }

    // Inner DTO classes
    public record BusRouteDTO(
        String tripId,
        String routeName,
        String headsign,
        Integer directionId,
        List<BusRouteStopDTO> stops
    ) {}

    public record BusRouteStopDTO(
        String stopId,
        String stopName,
        Double latitude,
        Double longitude,
        String scheduledArrivalTime,
        String scheduledDepartureTime,
        String estimatedArrivalTime,
        String estimatedDepartureTime,
        Integer tidalDelayMinutes,
        String inundationLevel,
        Integer sequence,
        Boolean isPassed,
        Boolean isCurrent
    ) {}

    public record RoutePathDTO(
        String tripId,
        String routeName,
        String headsign,
        Integer directionId,
        List<RoutePathService.RoutePathPoint> path
    ) {}
}

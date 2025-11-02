package com.majuro.transit.controller;

import com.majuro.transit.dto.BusPositionDTO;
import com.majuro.transit.model.gtfs.GtfsRoute;
import com.majuro.transit.model.gtfs.GtfsTrip;
import com.majuro.transit.service.GtfsBusSimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gtfs/routes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GtfsRouteController {

    private final GtfsBusSimulationService simulationService;

    @GetMapping
    public ResponseEntity<List<RouteDTO>> getAllRoutes() {
        List<RouteDTO> routes = simulationService.getAllRoutes()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<RouteDTO> getRoute(@PathVariable String routeId) {
        GtfsRoute route = simulationService.getRoute(routeId);
        return ResponseEntity.ok(convertToDTO(route));
    }

    @GetMapping("/{routeId}/buses")
    public ResponseEntity<List<BusPositionDTO>> getActiveTripsForRoute(@PathVariable String routeId) {
        List<BusPositionDTO> positions = simulationService.getActiveTripsForRoute(routeId)
                .stream()
                .map(BusPositionDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(positions);
    }

    @GetMapping("/{routeId}/trips")
    public ResponseEntity<List<TripDTO>> getTripsForRoute(@PathVariable String routeId) {
        List<GtfsTrip> trips = simulationService.getTripsForRoute(routeId);

        List<TripDTO> tripDTOs = trips.stream()
                .map(trip -> {
                    String startTime = trip.getStopTimes().isEmpty() ? null :
                        trip.getStopTimes().get(0).getDepartureTime().toString();
                    String endTime = trip.getStopTimes().isEmpty() ? null :
                        trip.getStopTimes().get(trip.getStopTimes().size() - 1).getArrivalTime().toString();

                    return new TripDTO(
                        trip.getTripId(),
                        trip.getRoute().getRouteShortName(),
                        trip.getTripHeadsign(),
                        trip.getDirectionId(),
                        startTime,
                        endTime
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(tripDTOs);
    }

    private RouteDTO convertToDTO(GtfsRoute route) {
        return new RouteDTO(
                route.getRouteId(),
                route.getRouteShortName(),
                route.getRouteLongName(),
                route.getRouteType()
        );
    }

    // Inner DTO classes
    public record RouteDTO(String routeId, String shortName, String longName, Integer type) {}
    public record TripDTO(String tripId, String routeName, String headsign, Integer directionId,
                         String startTime, String endTime) {}
}

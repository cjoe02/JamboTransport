package com.majuro.transit.controller;

import com.majuro.transit.dto.StopDTO;
import com.majuro.transit.model.RouteImpact;
import com.majuro.transit.model.gtfs.GtfsStop;
import com.majuro.transit.model.gtfs.GtfsStopTime;
import com.majuro.transit.service.GtfsBusSimulationService;
import com.majuro.transit.service.TidalImpactCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gtfs/stops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GtfsStopController {

    private final GtfsBusSimulationService simulationService;
    private final TidalImpactCalculator tidalImpactCalculator;

    @GetMapping
    public ResponseEntity<List<StopDTO>> getAllStops() {
        List<StopDTO> stops = simulationService.getAllStops()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stops);
    }

    @GetMapping("/{stopId}")
    public ResponseEntity<StopDTO> getStop(@PathVariable String stopId) {
        GtfsStop stop = simulationService.getStop(stopId);
        return ResponseEntity.ok(convertToDTO(stop));
    }

    @GetMapping("/{stopId}/arrivals")
    public ResponseEntity<List<ArrivalDTO>> getUpcomingArrivals(
            @PathVariable String stopId,
            @RequestParam(defaultValue = "5") int limit) {

        List<GtfsStopTime> arrivals = simulationService.getUpcomingArrivals(stopId, limit);

        List<ArrivalDTO> arrivalDTOs = arrivals.stream()
                .map(st -> {
                    LocalTime scheduledTime = st.getArrivalTime();
                    String routeId = st.getTrip().getRoute().getRouteId();

                    // Get tidal impact for this route
                    RouteImpact impact = tidalImpactCalculator.calculateImpact(routeId);

                    // Calculate adjusted arrival time
                    LocalTime estimatedTime = scheduledTime;
                    int delayMinutes = 0;
                    String serviceStatus = "ON_TIME";

                    if (impact.getImpactLevel() != RouteImpact.ImpactLevel.NONE) {
                        // Calculate delay based on estimated route delay
                        delayMinutes = impact.getEstimatedDelayMinutes();

                        // Add delay to scheduled time
                        estimatedTime = scheduledTime.plusMinutes(delayMinutes);

                        if (impact.getImpactLevel() == RouteImpact.ImpactLevel.SHUTDOWN) {
                            serviceStatus = "SHUTDOWN";
                        } else if (impact.getImpactLevel() == RouteImpact.ImpactLevel.MAJOR_DELAYS) {
                            serviceStatus = "MAJOR_DELAY";
                        } else {
                            serviceStatus = "MINOR_DELAY";
                        }
                    }

                    return new ArrivalDTO(
                        st.getTrip().getTripId(),
                        st.getTrip().getRoute().getRouteShortName(),
                        st.getTrip().getTripHeadsign(),
                        scheduledTime.toString(),
                        estimatedTime.toString(),
                        delayMinutes,
                        impact.getInundationLevel(),
                        impact.getInundationRisk(),
                        serviceStatus
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(arrivalDTOs);
    }

    private StopDTO convertToDTO(GtfsStop stop) {
        return new StopDTO(stop.getStopId(), stop.getStopName(), stop.getStopLat(), stop.getStopLon());
    }

    // Inner DTO class for arrivals with tidal impact
    public record ArrivalDTO(
        String tripId,
        String routeName,
        String headsign,
        String scheduledArrivalTime,
        String estimatedArrivalTime,
        Integer tidalDelayMinutes,
        String inundationLevel,
        Double inundationRisk,
        String serviceStatus
    ) {}
}

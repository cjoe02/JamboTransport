package com.majuro.transit.service;

import com.majuro.transit.model.RouteSegmentOrientation;
import com.majuro.transit.model.RouteSegmentOrientation.Orientation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RouteOrientationService {

    private List<RouteSegmentOrientation> orientations = new ArrayList<>();

    @PostConstruct
    public void initialize() {
        log.info("Initializing Route Orientation Service - hardcoding Majuro route orientations...");

        // ROUTE A - Hardcoded segment orientations based on Majuro geography
        // North-facing roads (exposed to northerly waves from ocean side)
        orientations.add(RouteSegmentOrientation.builder()
                .routeId("ROUTE_A")
                .fromStop("DUD")
                .toStop("RITA")
                .orientation(Orientation.NORTH_FACING)
                .build());

        orientations.add(RouteSegmentOrientation.builder()
                .routeId("ROUTE_A")
                .fromStop("RITA")
                .toStop("DELAP")
                .orientation(Orientation.NORTH_FACING)
                .build());

        orientations.add(RouteSegmentOrientation.builder()
                .routeId("ROUTE_A")
                .fromStop("DELAP")
                .toStop("ULIGA")
                .orientation(Orientation.NORTH_FACING)
                .build());

        // South-facing roads (exposed to southerly waves from lagoon side)
        orientations.add(RouteSegmentOrientation.builder()
                .routeId("ROUTE_A")
                .fromStop("ULIGA")
                .toStop("DARRIT")
                .orientation(Orientation.SOUTH_FACING)
                .build());

        orientations.add(RouteSegmentOrientation.builder()
                .routeId("ROUTE_A")
                .fromStop("DARRIT")
                .toStop("LAURA")
                .orientation(Orientation.SOUTH_FACING)
                .build());

        // ROUTE B - Hardcoded segment orientations
        orientations.add(RouteSegmentOrientation.builder()
                .routeId("ROUTE_B")
                .fromStop("DUD")
                .toStop("RITA")
                .orientation(Orientation.NORTH_FACING)
                .build());

        orientations.add(RouteSegmentOrientation.builder()
                .routeId("ROUTE_B")
                .fromStop("RITA")
                .toStop("AIRPORT")
                .orientation(Orientation.SOUTH_FACING)
                .build());

        orientations.add(RouteSegmentOrientation.builder()
                .routeId("ROUTE_B")
                .fromStop("AIRPORT")
                .toStop("LAURA")
                .orientation(Orientation.SOUTH_FACING)
                .build());

        orientations.add(RouteSegmentOrientation.builder()
                .routeId("ROUTE_B")
                .fromStop("LAURA")
                .toStop("MAJURO")
                .orientation(Orientation.NORTH_FACING)
                .build());

        log.info("Loaded {} route segment orientations", orientations.size());
    }

    public List<RouteSegmentOrientation> getOrientationsForRoute(String routeId) {
        return orientations.stream()
                .filter(o -> o.getRouteId().equals(routeId))
                .collect(Collectors.toList());
    }

    public List<RouteSegmentOrientation> getAllOrientations() {
        return new ArrayList<>(orientations);
    }

    public List<RouteSegmentOrientation> getSegmentsByOrientation(String routeId, Orientation orientation) {
        return orientations.stream()
                .filter(o -> o.getRouteId().equals(routeId) && o.getOrientation() == orientation)
                .collect(Collectors.toList());
    }
}

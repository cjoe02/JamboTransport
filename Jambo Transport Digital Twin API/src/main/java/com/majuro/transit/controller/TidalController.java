package com.majuro.transit.controller;

import com.majuro.transit.model.RouteImpact;
import com.majuro.transit.model.TidalReading;
import com.majuro.transit.service.TidalDataService;
import com.majuro.transit.service.TidalImpactCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tidal")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TidalController {

    private final TidalDataService tidalDataService;
    private final TidalImpactCalculator tidalImpactCalculator;

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentTidalReading() {
        TidalReading reading = tidalDataService.getCurrentReading();

        Map<String, Object> response = new HashMap<>();
        response.put("waveHeight", reading.getWaveHeight());
        response.put("waveDirection", reading.getWaveDirection());
        response.put("directionName", reading.getDirectionName());
        response.put("wavePeriod", reading.getWavePeriod());
        response.put("timestamp", reading.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("stationId", reading.getStationId());
        response.put("stationName", reading.getStationName());
        response.put("latitude", reading.getLatitude());
        response.put("longitude", reading.getLongitude());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/impact")
    public ResponseEntity<Map<String, Object>> getAllRouteImpacts() {
        List<RouteImpact> impacts = tidalImpactCalculator.calculateAllRouteImpacts();
        TidalReading currentReading = tidalDataService.getCurrentReading();

        Map<String, Object> response = new HashMap<>();
        response.put("routeImpacts", impacts);
        response.put("lastUpdated", currentReading.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("currentWaveHeight", currentReading.getWaveHeight());
        response.put("currentWaveDirection", currentReading.getDirectionName());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/impact/{routeId}")
    public ResponseEntity<RouteImpact> getRouteImpact(@PathVariable String routeId) {
        RouteImpact impact = tidalImpactCalculator.calculateImpact(routeId);
        return ResponseEntity.ok(impact);
    }

    @GetMapping("/historical")
    public ResponseEntity<List<TidalReading>> getHistoricalData() {
        List<TidalReading> historical = tidalDataService.getHistoricalData();
        return ResponseEntity.ok(historical);
    }
}

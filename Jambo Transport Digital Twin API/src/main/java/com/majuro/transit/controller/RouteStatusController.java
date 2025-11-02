package com.majuro.transit.controller;

import com.majuro.transit.model.RouteImpact;
import com.majuro.transit.model.TidalReading;
import com.majuro.transit.service.TidalDataService;
import com.majuro.transit.service.TidalImpactCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RouteStatusController {

    private final TidalImpactCalculator tidalImpactCalculator;
    private final TidalDataService tidalDataService;

    @GetMapping("/{routeId}/status")
    public ResponseEntity<Map<String, Object>> getRouteStatus(@PathVariable String routeId) {
        RouteImpact impact = tidalImpactCalculator.calculateImpact(routeId);
        TidalReading currentReading = tidalDataService.getCurrentReading();

        Map<String, Object> response = new HashMap<>();
        response.put("routeId", impact.getRouteId());
        response.put("serviceable", impact.isServiceable());
        response.put("impactLevel", impact.getImpactLevel().name());
        response.put("delayMultiplier", impact.getDelayMultiplier());
        response.put("estimatedDelayMinutes", impact.getEstimatedDelayMinutes());
        response.put("reason", impact.getReason());
        response.put("affectedSegments", impact.getAffectedSegments());
        response.put("currentWaveHeight", currentReading.getWaveHeight());
        response.put("waveDirection", currentReading.getDirectionName());

        // Inundation assessment
        response.put("inundationRisk", impact.getInundationRisk());
        response.put("inundationLevel", impact.getInundationLevel());
        response.put("inundationDescription", impact.getInundationDescription());

        return ResponseEntity.ok(response);
    }
}

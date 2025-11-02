package com.majuro.transit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteImpact {

    private String routeId;
    private ImpactLevel impactLevel;
    private Double delayMultiplier;
    private Integer estimatedDelayMinutes;
    private String reason;
    private List<String> affectedSegments;
    private boolean serviceable;

    // Inundation assessment
    private Double currentWaveHeight;
    private Double inundationRisk;  // 0.0 to 1.0 scale
    private String inundationLevel; // "SAFE", "LOW_RISK", "HIGH_RISK", "CRITICAL"
    private String inundationDescription;

    public enum ImpactLevel {
        NONE,
        SLIGHT_DELAYS,
        MAJOR_DELAYS,
        SHUTDOWN
    }
}

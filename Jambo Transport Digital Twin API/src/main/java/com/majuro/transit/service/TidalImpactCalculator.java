package com.majuro.transit.service;

import com.majuro.transit.model.RouteImpact;
import com.majuro.transit.model.RouteImpact.ImpactLevel;
import com.majuro.transit.model.RouteSegmentOrientation;
import com.majuro.transit.model.RouteSegmentOrientation.Orientation;
import com.majuro.transit.model.TidalReading;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TidalImpactCalculator {

    private final TidalDataService tidalDataService;
    private final RouteOrientationService orientationService;

    // Majuro's highest land elevation is 3 meters
    private static final double MAJURO_MAX_ELEVATION = 3.0;

    public RouteImpact calculateImpact(String routeId) {
        TidalReading currentReading = tidalDataService.getCurrentReading();
        List<RouteSegmentOrientation> routeSegments = orientationService.getOrientationsForRoute(routeId);

        if (routeSegments.isEmpty()) {
            log.warn("No segments found for route: {}", routeId);
            return createNoImpact(routeId);
        }

        double waveHeight = currentReading.getWaveHeight();
        String waveDirection = currentReading.getDirectionName();
        double waveDegrees = currentReading.getWaveDirection();

        // Determine which segments are affected
        List<String> affectedSegments = getAffectedSegments(routeSegments, waveDegrees);

        // Calculate impact level and delay
        ImpactLevel impactLevel;
        double delayMultiplier;
        int estimatedDelayMinutes;
        boolean serviceable;

        if (waveHeight >= 5.0) {
            // 5m+ = Service shutdown
            impactLevel = ImpactLevel.SHUTDOWN;
            delayMultiplier = 0.0;
            estimatedDelayMinutes = 0;
            serviceable = false;
        } else if (waveHeight >= 4.0) {
            // 4-5m = Major delays (50% slower)
            impactLevel = ImpactLevel.MAJOR_DELAYS;
            delayMultiplier = 1.5;
            estimatedDelayMinutes = calculateDelayMinutes(routeId, 0.5);
            serviceable = true;
        } else if (waveHeight >= 3.0) {
            // 3-4m = Slight delays (20% slower)
            impactLevel = ImpactLevel.SLIGHT_DELAYS;
            delayMultiplier = 1.2;
            estimatedDelayMinutes = calculateDelayMinutes(routeId, 0.2);
            serviceable = true;
        } else {
            // < 3m = No impact
            return createNoImpact(routeId);
        }

        // Only apply impact if affected segments exist
        if (affectedSegments.isEmpty()) {
            return createNoImpact(routeId);
        }

        String reason = String.format(
            "%s segments affected by %s %.1fm waves",
            affectedSegments.size() == routeSegments.size() ? "All" :
            affectedSegments.size() > routeSegments.size() / 2 ? "Most" : "Some",
            waveDirection.toLowerCase(),
            waveHeight
        );

        // Calculate inundation assessment
        InundationAssessment inundation = calculateInundation(waveHeight);

        return RouteImpact.builder()
                .routeId(routeId)
                .impactLevel(impactLevel)
                .delayMultiplier(delayMultiplier)
                .estimatedDelayMinutes(estimatedDelayMinutes)
                .reason(reason)
                .affectedSegments(affectedSegments)
                .serviceable(serviceable)
                .currentWaveHeight(waveHeight)
                .inundationRisk(inundation.risk)
                .inundationLevel(inundation.level)
                .inundationDescription(inundation.description)
                .build();
    }

    private InundationAssessment calculateInundation(double waveHeight) {
        // Majuro's max elevation is 3m
        // Calculate risk based on how close wave height is to this threshold

        if (waveHeight >= 5.0) {
            // 5m+ waves exceed Majuro's max elevation by 2m+
            return new InundationAssessment(
                1.0,
                "CRITICAL",
                String.format("Wave height %.1fm exceeds Majuro's max elevation (3m) by %.1fm. Severe flooding expected on all low-lying roads. Service shutdown required.",
                    waveHeight, waveHeight - MAJURO_MAX_ELEVATION)
            );
        } else if (waveHeight >= 4.0) {
            // 4-5m waves exceed max elevation by 1-2m
            double excess = waveHeight - MAJURO_MAX_ELEVATION;
            return new InundationAssessment(
                0.7,
                "HIGH_RISK",
                String.format("Wave height %.1fm exceeds Majuro's max elevation (3m) by %.1fm. Significant flooding expected on affected road segments. Major delays likely.",
                    waveHeight, excess)
            );
        } else if (waveHeight >= 3.0) {
            // 3-4m waves at or near max elevation
            double proximity = waveHeight - MAJURO_MAX_ELEVATION;
            return new InundationAssessment(
                0.4,
                "LOW_RISK",
                String.format("Wave height %.1fm approaches Majuro's max elevation (3m). Minor flooding possible on exposed coastal segments (%.1fm proximity). Slight delays expected.",
                    waveHeight, proximity)
            );
        } else {
            // < 3m waves below max elevation
            double margin = MAJURO_MAX_ELEVATION - waveHeight;
            return new InundationAssessment(
                0.0,
                "SAFE",
                String.format("Wave height %.1fm is below Majuro's max elevation (3m) with %.1fm safety margin. No inundation risk. Normal operations.",
                    waveHeight, margin)
            );
        }
    }

    private static class InundationAssessment {
        final double risk;
        final String level;
        final String description;

        InundationAssessment(double risk, String level, String description) {
            this.risk = risk;
            this.level = level;
            this.description = description;
        }
    }

    private List<String> getAffectedSegments(List<RouteSegmentOrientation> segments, double waveDegrees) {
        boolean isNortherlyWave = waveDegrees >= 315 || waveDegrees < 45;
        boolean isSoutherlyWave = waveDegrees >= 135 && waveDegrees < 225;

        return segments.stream()
                .filter(segment -> {
                    if (segment.getOrientation() == Orientation.NORTH_FACING && isSoutherlyWave) {
                        // North-facing roads affected by southerly waves
                        return true;
                    } else if (segment.getOrientation() == Orientation.SOUTH_FACING && isNortherlyWave) {
                        // South-facing roads affected by northerly waves
                        return true;
                    }
                    return false;
                })
                .map(segment -> segment.getFromStop() + " → " + segment.getToStop())
                .collect(Collectors.toList());
    }

    private int calculateDelayMinutes(String routeId, double delayPercentage) {
        // Estimate average route time (assuming 30-45 minutes per route)
        int averageRouteDuration = routeId.equals("ROUTE_A") ? 40 : 35;
        return (int) Math.ceil(averageRouteDuration * delayPercentage);
    }

    private RouteImpact createNoImpact(String routeId) {
        TidalReading currentReading = tidalDataService.getCurrentReading();
        double waveHeight = currentReading.getWaveHeight();
        InundationAssessment inundation = calculateInundation(waveHeight);

        return RouteImpact.builder()
                .routeId(routeId)
                .impactLevel(ImpactLevel.NONE)
                .delayMultiplier(1.0)
                .estimatedDelayMinutes(0)
                .reason("No tidal impact - normal operations")
                .affectedSegments(new ArrayList<>())
                .serviceable(true)
                .currentWaveHeight(waveHeight)
                .inundationRisk(inundation.risk)
                .inundationLevel(inundation.level)
                .inundationDescription(inundation.description)
                .build();
    }

    public List<RouteImpact> calculateAllRouteImpacts() {
        List<RouteImpact> impacts = new ArrayList<>();
        impacts.add(calculateImpact("ROUTE_A"));
        impacts.add(calculateImpact("ROUTE_B"));
        return impacts;
    }
}

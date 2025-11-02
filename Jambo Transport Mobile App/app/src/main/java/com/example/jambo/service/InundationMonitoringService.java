package com.example.jambo.service;

import com.example.jambo.model.BusRoute;
import com.example.jambo.model.InundationReading;
import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;
import java.util.List;

public class InundationMonitoringService {
    
    private static InundationMonitoringService instance;
    private List<InundationReading> currentReadings;
    
    private InundationMonitoringService() {
        currentReadings = new ArrayList<>();
    }
    
    public static InundationMonitoringService getInstance() {
        if (instance == null) {
            instance = new InundationMonitoringService();
        }
        return instance;
    }

    public void addInundationReadingsToRoute(BusRoute route, List<InundationReading> readings) {
        if (route == null || readings == null) {
            return;
        }

        route.setInundationReadings(readings);
        
        BusRoute.FloodStatus overallStatus = determineRouteFloodStatus(readings);
        route.setFloodStatus(overallStatus);
        
        currentReadings.addAll(readings);
    }

    public BusRoute.FloodStatus determineRouteFloodStatus(List<InundationReading> readings) {
        if (readings == null || readings.isEmpty()) {
            return BusRoute.FloodStatus.SAFE;
        }

        boolean hasWarning = false;
        boolean hasMinor = false;

        for (InundationReading reading : readings) {
            switch (reading.getLevel()) {
                case WARNING:
                    hasWarning = true;
                    break;
                case MINOR:
                    hasMinor = true;
                    break;
                case SAFE:
                default:
                    break;
            }
        }

        if (hasWarning) {
            return BusRoute.FloodStatus.FLOOD_WARNING;
        } else if (hasMinor) {
            return BusRoute.FloodStatus.MINOR_FLOODING;
        } else {
            return BusRoute.FloodStatus.SAFE;
        }
    }

    public List<InundationReading> getReadingsNearRoute(BusRoute route, double radiusKm) {
        List<InundationReading> nearbyReadings = new ArrayList<>();
        
        if (route.getRoutePoints() == null || route.getRoutePoints().isEmpty()) {
            return nearbyReadings;
        }

        for (InundationReading reading : currentReadings) {
            for (GeoPoint routePoint : route.getRoutePoints()) {
                double distance = calculateDistance(reading.getLocation(), routePoint);
                if (distance <= radiusKm) {
                    nearbyReadings.add(reading);
                    break;
                }
            }
        }
        
        return nearbyReadings;
    }

    public void simulateInundationData(BusRoute route) {
        List<InundationReading> simulatedReadings = new ArrayList<>();
        
        if (route.getRoutePoints() == null || route.getRoutePoints().isEmpty()) {
            return;
        }

        for (int i = 0; i < route.getRoutePoints().size(); i += 3) {
            GeoPoint point = route.getRoutePoints().get(i);
            
            GeoPoint sensorLocation = new GeoPoint(
                point.getLatitude() + (Math.random() - 0.5) * 0.002, // ~200m variation
                point.getLongitude() + (Math.random() - 0.5) * 0.002
            );
            
            double waterLevel = Math.random() * 25.0;
            
            InundationReading reading = new InundationReading(
                "sensor_" + i,
                sensorLocation,
                waterLevel,
                "majuro_sensor_" + i
            );
            
            simulatedReadings.add(reading);
        }
        
        addInundationReadingsToRoute(route, simulatedReadings);
    }

    private double calculateDistance(GeoPoint point1, GeoPoint point2) {
        final int R = 6371;
        
        double latDistance = Math.toRadians(point2.getLatitude() - point1.getLatitude());
        double lonDistance = Math.toRadians(point2.getLongitude() - point1.getLongitude());
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(point1.getLatitude())) * Math.cos(Math.toRadians(point2.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }

    public List<InundationReading> getCurrentReadings() {
        return new ArrayList<>(currentReadings);
    }

    public void clearReadings() {
        currentReadings.clear();
    }
}
package com.example.jambo.model;

import org.osmdroid.util.GeoPoint;
import java.util.List;

public class BusRoute {
    private String routeId;
    private String routeName;
    private String startLocation;
    private String endLocation;
    private List<GeoPoint> routePoints;
    private List<Bus> activeBuses;
    private FloodStatus floodStatus;
    private List<InundationReading> inundationReadings;

    public BusRoute() {}

    public BusRoute(String routeId, String routeName, String startLocation, String endLocation,
                   List<GeoPoint> routePoints, List<Bus> activeBuses) {
        this.routeId = routeId;
        this.routeName = routeName;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.routePoints = routePoints;
        this.activeBuses = activeBuses;
        this.floodStatus = FloodStatus.SAFE;
    }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public String getStartLocation() { return startLocation; }
    public void setStartLocation(String startLocation) { this.startLocation = startLocation; }

    public String getEndLocation() { return endLocation; }
    public void setEndLocation(String endLocation) { this.endLocation = endLocation; }

    public List<GeoPoint> getRoutePoints() { return routePoints; }
    public void setRoutePoints(List<GeoPoint> routePoints) { this.routePoints = routePoints; }

    public List<Bus> getActiveBuses() { return activeBuses; }
    public void setActiveBuses(List<Bus> activeBuses) { this.activeBuses = activeBuses; }

    public FloodStatus getFloodStatus() { return floodStatus; }
    public void setFloodStatus(FloodStatus floodStatus) { this.floodStatus = floodStatus; }

    public List<InundationReading> getInundationReadings() { return inundationReadings; }
    public void setInundationReadings(List<InundationReading> inundationReadings) { 
        this.inundationReadings = inundationReadings; 
    }

    public enum FloodStatus {
        SAFE("Safe"),
        MINOR_FLOODING("Minor Flooding"),
        FLOOD_WARNING("Flood Warning");

        private final String displayName;

        FloodStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
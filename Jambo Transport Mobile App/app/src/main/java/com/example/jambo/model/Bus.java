package com.example.jambo.model;

import org.osmdroid.util.GeoPoint;

public class Bus {
    private String busId;
    private String busNumber;
    private GeoPoint currentLocation;
    private String routeId;
    private boolean isActive;
    private long lastUpdateTime;

    public Bus() {}

    public Bus(String busId, String busNumber, GeoPoint currentLocation, String routeId, boolean isActive) {
        this.busId = busId;
        this.busNumber = busNumber;
        this.currentLocation = currentLocation;
        this.routeId = routeId;
        this.isActive = isActive;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }

    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }

    public GeoPoint getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(GeoPoint currentLocation) { 
        this.currentLocation = currentLocation; 
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
}
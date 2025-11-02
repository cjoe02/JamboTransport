package com.example.jambo.model;

import org.osmdroid.util.GeoPoint;

public class InundationReading {
    private String readingId;
    private GeoPoint location;
    private double waterLevel; // in centimeters
    private long timestamp;
    private String sensorId;
    private InundationLevel level;

    public InundationReading() {}

    public InundationReading(String readingId, GeoPoint location, double waterLevel, String sensorId) {
        this.readingId = readingId;
        this.location = location;
        this.waterLevel = waterLevel;
        this.sensorId = sensorId;
        this.timestamp = System.currentTimeMillis();
        this.level = determineInundationLevel(waterLevel);
    }

    private InundationLevel determineInundationLevel(double waterLevel) {
        if (waterLevel < 5.0) {
            return InundationLevel.SAFE;
        } else if (waterLevel < 15.0) {
            return InundationLevel.MINOR;
        } else {
            return InundationLevel.WARNING;
        }
    }

    public String getReadingId() { return readingId; }
    public void setReadingId(String readingId) { this.readingId = readingId; }

    public GeoPoint getLocation() { return location; }
    public void setLocation(GeoPoint location) { this.location = location; }

    public double getWaterLevel() { return waterLevel; }
    public void setWaterLevel(double waterLevel) { 
        this.waterLevel = waterLevel;
        this.level = determineInundationLevel(waterLevel);
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public InundationLevel getLevel() { return level; }
    public void setLevel(InundationLevel level) { this.level = level; }

    public enum InundationLevel {
        SAFE("Safe", "#4CAF50"),
        MINOR("Minor Flooding", "#FF9800"),
        WARNING("Flood Warning", "#FF3D00");

        private final String displayName;
        private final String colorHex;

        InundationLevel(String displayName, String colorHex) {
            this.displayName = displayName;
            this.colorHex = colorHex;
        }

        public String getDisplayName() { return displayName; }
        public String getColorHex() { return colorHex; }
    }
}
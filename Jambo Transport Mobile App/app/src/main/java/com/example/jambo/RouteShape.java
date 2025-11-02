package com.example.jambo;

import com.google.gson.annotations.SerializedName;

public class RouteShape {
    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("sequence")
    private Integer sequence;

    @SerializedName("stopId")
    private String stopId;

    @SerializedName("stopName")
    private String stopName;

    @SerializedName("distanceFromStart")
    private Double distanceFromStart;

    @SerializedName("isStop")
    private Boolean isStop;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Integer getSequence() {
        return sequence;
    }

    public String getStopId() {
        return stopId;
    }

    public String getStopName() {
        return stopName;
    }

    public Double getDistanceFromStart() {
        return distanceFromStart;
    }

    public Boolean isStop() {
        return isStop;
    }
}

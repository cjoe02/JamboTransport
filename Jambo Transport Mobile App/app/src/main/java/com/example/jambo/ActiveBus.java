package com.example.jambo;

import com.google.gson.annotations.SerializedName;

public class ActiveBus {
    @SerializedName("busId")
    private int busId;

    @SerializedName("busLabel")
    private String busLabel;

    @SerializedName("routeName")
    private String routeName;

    @SerializedName("currentLatitude")
    private double latitude;

    @SerializedName("currentLongitude")
    private double longitude;

    @SerializedName("status")
    private String status;

    @SerializedName("isOperational")
    private boolean isOperational;

    @SerializedName("minutesToNextStop")
    private int minutesToNextStop;

    @SerializedName("nextStopName")
    private String nextStopName;

    @SerializedName("arrivalTime")
    private String arrivalTime;

    public int getBusId() {
        return busId;
    }

    public String getBusLabel() {
        return busLabel;
    }

    public String getRouteName() {
        return routeName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getStatus() {
        return status;
    }

    public boolean isOperational() {
        return isOperational;
    }

    public int getMinutesToNextStop() {
        return minutesToNextStop;
    }

    public String getNextStopName() {
        return nextStopName;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }
}

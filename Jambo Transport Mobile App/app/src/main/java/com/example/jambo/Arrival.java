package com.example.jambo;

import com.google.gson.annotations.SerializedName;

public class Arrival {
    @SerializedName("tripId")
    private String busLabel;

    @SerializedName("routeName")
    private String routeName;

    @SerializedName("headsign")
    private String headsign;

    @SerializedName("arrivalTime")
    private String arrivalTime;

    @SerializedName("scheduledArrivalTime")
    private String scheduledArrivalTime;

    @SerializedName("estimatedArrivalTime")
    private String estimatedArrivalTime;

    @SerializedName("serviceStatus")
    private String serviceStatus;

    public String getBusLabel() {
        return busLabel;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getHeadsign() {
        return headsign;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public String getScheduledArrivalTime() {
        return scheduledArrivalTime;
    }

    public String getEstimatedArrivalTime() {
        return estimatedArrivalTime;
    }

    public String getServiceStatus() {
        return serviceStatus;
    }
}

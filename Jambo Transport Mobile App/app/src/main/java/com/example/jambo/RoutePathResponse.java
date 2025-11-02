package com.example.jambo;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RoutePathResponse {
    @SerializedName("tripId")
    private String tripId;

    @SerializedName("routeName")
    private String routeName;

    @SerializedName("headsign")
    private String headsign;

    @SerializedName("directionId")
    private int directionId;

    @SerializedName("path")
    private List<RouteShape> path;

    public String getTripId() {
        return tripId;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getHeadsign() {
        return headsign;
    }

    public int getDirectionId() {
        return directionId;
    }

    public List<RouteShape> getPath() {
        return path;
    }

    // Legacy method for backward compatibility
    public String getRoute() {
        return routeName;
    }
}

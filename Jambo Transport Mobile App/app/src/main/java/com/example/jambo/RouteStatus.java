package com.example.jambo;

import com.google.gson.annotations.SerializedName;

public class RouteStatus {
    @SerializedName("impactLevel")
    private String impactLevel;

    @SerializedName("reason")
    private String reason;

    @SerializedName("estimatedDelayMinutes")
    private Integer estimatedDelayMinutes;

    @SerializedName("tideDirection")
    private String tideDirection;

    public String getImpactLevel() {
        return impactLevel;
    }

    public String getReason() {
        return reason;
    }

    public Integer getEstimatedDelayMinutes() {
        return estimatedDelayMinutes;
    }

    public String getTideDirection() {
        return tideDirection;
    }
}

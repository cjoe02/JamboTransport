package com.majuro.transit.model;

import com.majuro.transit.dto.StopDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusPosition {

    private Long busId;
    private String busLabel;
    private String routeName;

    private StopDTO currentStop;
    private StopDTO nextStop;

    private Double currentLatitude;
    private Double currentLongitude;

    private Double progressPercent; // Progress on current segment (0-100)
    private Boolean isOnBreak;
    private Boolean isOperational;

    private Integer minutesToNextStop;
    private String status; // "MOVING", "ON_BREAK", "NOT_OPERATIONAL"

    // Tidal impact fields
    private Boolean tidalDelayApplied;
    private Integer estimatedDelayMinutes;
    private String tidalImpactLevel; // "NONE", "SLIGHT_DELAYS", "MAJOR_DELAYS", "SHUTDOWN"
    private String estimatedArrivalTime; // Next stop ETA with tidal delay (HH:mm format)
}

package com.majuro.transit.dto;

import com.majuro.transit.model.BusPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusPositionDTO {

    private Long busId;
    private String busLabel;
    private String routeName;

    private StopDTO currentStop;
    private StopDTO nextStop;

    private Double currentLatitude;
    private Double currentLongitude;

    private Double progressPercent;
    private Boolean isOnBreak;
    private Boolean isOperational;
    private Integer minutesToNextStop;
    private String status;

    // Tidal impact fields
    private Boolean tidalDelayApplied;
    private Integer estimatedDelayMinutes;
    private String tidalImpactLevel;
    private String estimatedArrivalTime;  // Next stop ETA with tidal delay

    public static BusPositionDTO fromEntity(BusPosition position) {
        BusPositionDTO dto = new BusPositionDTO();
        dto.setBusId(position.getBusId());
        dto.setBusLabel(position.getBusLabel());
        dto.setRouteName(position.getRouteName());
        dto.setCurrentStop(position.getCurrentStop());
        dto.setNextStop(position.getNextStop());
        dto.setCurrentLatitude(position.getCurrentLatitude());
        dto.setCurrentLongitude(position.getCurrentLongitude());
        dto.setProgressPercent(position.getProgressPercent());
        dto.setIsOnBreak(position.getIsOnBreak());
        dto.setIsOperational(position.getIsOperational());
        dto.setMinutesToNextStop(position.getMinutesToNextStop());
        dto.setStatus(position.getStatus());
        dto.setTidalDelayApplied(position.getTidalDelayApplied());
        dto.setEstimatedDelayMinutes(position.getEstimatedDelayMinutes());
        dto.setTidalImpactLevel(position.getTidalImpactLevel());
        dto.setEstimatedArrivalTime(position.getEstimatedArrivalTime());
        return dto;
    }
}

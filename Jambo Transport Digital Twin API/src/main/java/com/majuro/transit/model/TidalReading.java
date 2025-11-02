package com.majuro.transit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tidal_readings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TidalReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String stationId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Double waveHeight;  // in meters

    @Column(nullable = false)
    private Double wavePeriod;  // Tp in seconds

    @Column(nullable = false)
    private Double waveDirection;  // Dp in degrees (0-360)

    @Column
    private String stationName;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Transient
    public String getDirectionName() {
        if (waveDirection >= 315 || waveDirection < 45) {
            return "NORTHERLY";
        } else if (waveDirection >= 45 && waveDirection < 135) {
            return "EASTERLY";
        } else if (waveDirection >= 135 && waveDirection < 225) {
            return "SOUTHERLY";
        } else {
            return "WESTERLY";
        }
    }
}

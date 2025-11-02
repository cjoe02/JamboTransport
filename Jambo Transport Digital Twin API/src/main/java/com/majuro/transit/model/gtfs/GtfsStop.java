package com.majuro.transit.model.gtfs;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gtfs_stops")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GtfsStop {

    @Id
    private String stopId;

    @Column(nullable = false)
    private String stopName;

    @Column(nullable = false)
    private Double stopLat;

    @Column(nullable = false)
    private Double stopLon;
}

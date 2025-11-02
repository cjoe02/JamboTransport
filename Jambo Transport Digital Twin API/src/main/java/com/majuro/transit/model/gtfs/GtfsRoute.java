package com.majuro.transit.model.gtfs;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gtfs_routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GtfsRoute {

    @Id
    private String routeId;

    @Column(nullable = false)
    private String routeShortName;

    @Column(nullable = false)
    private String routeLongName;

    @Column(nullable = false)
    private Integer routeType; // 3 = Bus
}

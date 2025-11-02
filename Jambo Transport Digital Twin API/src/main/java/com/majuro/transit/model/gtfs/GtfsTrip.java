package com.majuro.transit.model.gtfs;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gtfs_trips")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GtfsTrip {

    @Id
    private String tripId;

    @ManyToOne
    @JoinColumn(name = "route_id", nullable = false)
    private GtfsRoute route;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private GtfsCalendar service;

    @Column(nullable = false)
    private String tripHeadsign;

    @Column(nullable = false)
    private Integer directionId;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopSequence ASC")
    private List<GtfsStopTime> stopTimes = new ArrayList<>();
}
